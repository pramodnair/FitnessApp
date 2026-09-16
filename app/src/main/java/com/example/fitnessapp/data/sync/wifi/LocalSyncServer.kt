package com.example.fitnessapp.data.sync.wifi

import android.util.Log
import com.example.fitnessapp.data.model.UserDailyScore
import com.example.fitnessapp.data.sync.SyncCheerPayload
import com.example.fitnessapp.data.sync.SyncDataPayload
import com.example.fitnessapp.data.sync.SyncHandshake
import com.example.fitnessapp.data.sync.SyncResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException

class LocalSyncServer(
    private val expectedPairCodeProvider: () -> String,
    private val localHandshakeProvider: (port: Int) -> SyncHandshake,
    private val localScoreProvider: () -> UserDailyScore,
    private val onPartnerScoreReceived: (UserDailyScore, String?) -> Unit,
    private val onCheerReceived: (senderName: String, message: String) -> Unit
) {
    companion object {
        private const val TAG = "LocalSyncServer"
        const val DEFAULT_PORT = 8988
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    var boundPort: Int = 0
        private set

    val isRunning: Boolean
        get() = serverSocket?.isClosed == false && serverJob?.isActive == true

    @Synchronized
    fun start(preferredPort: Int = DEFAULT_PORT): Int {
        if (isRunning) return boundPort

        var port = preferredPort
        var socket: ServerSocket? = null

        // Attempt preferred port, fallback to any free port (0) if busy
        try {
            socket = ServerSocket(port)
        } catch (e: Exception) {
            Log.w(TAG, "Preferred port $port unavailable, binding to dynamic port...")
            try {
                socket = ServerSocket(0)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to bind ServerSocket", e2)
                return 0
            }
        }

        serverSocket = socket
        boundPort = socket.localPort
        Log.i(TAG, "Local sync server running on port $boundPort")

        serverJob = scope.launch {
            while (isActive && serverSocket?.isClosed == false) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch { handleClient(clientSocket) }
                } catch (e: SocketException) {
                    // Socket closed normally on stop
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error accepting client connection", e)
                }
            }
        }

        return boundPort
    }

    @Synchronized
    fun stop() {
        try {
            serverJob?.cancel()
            serverJob = null
            serverSocket?.close()
            serverSocket = null
            boundPort = 0
            Log.i(TAG, "Local sync server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val inStream = socket.getInputStream()
            val outStream = socket.getOutputStream()
            val writer = PrintWriter(OutputStreamWriter(outStream, Charsets.UTF_8), true)

            fun readAsciiLine(): String {
                val sb = StringBuilder()
                var c: Int
                while (inStream.read().also { c = it } != -1) {
                    if (c == '\n'.code) break
                    if (c != '\r'.code) sb.append(c.toChar())
                }
                return sb.toString()
            }

            val requestLine = readAsciiLine()
            if (requestLine.isBlank()) return
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0]
            val path = parts[1]

            // Read headers
            var contentLength = 0
            while (true) {
                val headerLine = readAsciiLine()
                if (headerLine.isBlank()) break
                if (headerLine.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = headerLine.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            // Read body in exact UTF-8 bytes
            val body = if (contentLength > 0) {
                val bytes = ByteArray(contentLength)
                var totalRead = 0
                while (totalRead < contentLength) {
                    val read = inStream.read(bytes, totalRead, contentLength - totalRead)
                    if (read == -1) break
                    totalRead += read
                }
                String(bytes, 0, totalRead, Charsets.UTF_8)
            } else ""

            when {
                path == "/ping" || path.startsWith("/ping?") -> {
                    val handshake = localHandshakeProvider(boundPort)
                    val responseJson = json.encodeToString(handshake)
                    sendHttpResponse(writer, 200, "OK", responseJson)
                }

                path == "/sync" && method.equals("POST", ignoreCase = true) -> {
                    try {
                        val payload = json.decodeFromString<SyncDataPayload>(body)
                        val expectedCode = expectedPairCodeProvider().trim().uppercase()
                        val receivedCode = payload.pairCode.trim().uppercase()

                        if (expectedCode.isNotEmpty() && receivedCode != expectedCode) {
                            Log.w(TAG, "Rejected sync: code mismatch (got $receivedCode, expected $expectedCode)")
                            val errorResponse = json.encodeToString(
                                SyncResponse(
                                    status = "REJECTED_PAIR_CODE",
                                    message = "Pairing code mismatch."
                                )
                            )
                            sendHttpResponse(writer, 403, "Forbidden", errorResponse)
                        } else {
                            // Update local repository with partner's score
                            onPartnerScoreReceived(payload.senderScore, payload.cheerMessage)

                            // Respond with our own current score
                            val response = SyncResponse(
                                status = "ACCEPTED",
                                message = "Synced successfully",
                                recipientScore = localScoreProvider()
                            )
                            sendHttpResponse(writer, 200, "OK", json.encodeToString(response))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Malformed sync payload", e)
                        sendHttpResponse(writer, 400, "Bad Request", "{\"error\":\"Malformed JSON\"}")
                    }
                }

                path == "/cheer" && method.equals("POST", ignoreCase = true) -> {
                    try {
                        val cheer = json.decodeFromString<SyncCheerPayload>(body)
                        val expectedCode = expectedPairCodeProvider().trim().uppercase()
                        if (expectedCode.isEmpty() || cheer.pairCode.trim().uppercase() == expectedCode) {
                            onCheerReceived(cheer.senderName, cheer.message)
                            sendHttpResponse(writer, 200, "OK", "{\"status\":\"CHEER_DELIVERED\"}")
                        } else {
                            sendHttpResponse(writer, 403, "Forbidden", "{\"error\":\"Code mismatch\"}")
                        }
                    } catch (e: Exception) {
                        sendHttpResponse(writer, 400, "Bad Request", "{\"error\":\"Invalid cheer\"}")
                    }
                }

                else -> {
                    sendHttpResponse(writer, 404, "Not Found", "{\"error\":\"Not Found\"}")
                }
            }

            socket.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        }
    }

    private fun sendHttpResponse(writer: PrintWriter, statusCode: Int, statusText: String, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        writer.print("HTTP/1.1 $statusCode $statusText\r\n")
        writer.print("Content-Type: application/json; charset=utf-8\r\n")
        writer.print("Content-Length: ${bytes.size}\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.print(body)
        writer.flush()
    }
}
