package com.example.fitnessapp.data.sync.wifi

import android.util.Log
import com.example.fitnessapp.data.sync.SyncCheerPayload
import com.example.fitnessapp.data.sync.SyncDataPayload
import com.example.fitnessapp.data.sync.SyncHandshake
import com.example.fitnessapp.data.sync.SyncResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

class LocalSyncClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "LocalSyncClient"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    suspend fun ping(ip: String, port: Int): Result<SyncHandshake> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$ip:$port/ping"
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val handshake = json.decodeFromString<SyncHandshake>(body)
                    Result.success(handshake)
                } else {
                    Result.failure(Exception("Ping returned code ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to ping $ip:$port", e)
            Result.failure(e)
        }
    }

    /**
     * Ultra-fast ping with short connect timeout (default 350ms) designed for subnet sweeping.
     */
    suspend fun quickPing(ip: String, port: Int, timeoutMs: Int = 350): Result<SyncHandshake> = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeoutMs)
            socket.soTimeout = 1000
            val out = OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
            out.write("GET /ping HTTP/1.1\r\nHost: $ip:$port\r\nConnection: close\r\n\r\n")
            out.flush()

            val statusLine = reader.readLine() ?: ""
            if (!statusLine.contains("200")) {
                return@withContext Result.failure(Exception("HTTP status: $statusLine"))
            }

            var contentLength = 0
            while (true) {
                val header = reader.readLine() ?: break
                if (header.isEmpty()) break
                if (header.startsWith("Content-Length:", ignoreCase = true)) {
                    contentLength = header.substringAfter(":").trim().toIntOrNull() ?: 0
                }
            }

            val body = if (contentLength > 0) {
                val buf = CharArray(contentLength)
                var read = 0
                while (read < contentLength) {
                    val r = reader.read(buf, read, contentLength - read)
                    if (r == -1) break
                    read += r
                }
                String(buf, 0, read)
            } else {
                reader.readText()
            }

            val handshake = json.decodeFromString<SyncHandshake>(body)
            Result.success(handshake)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try { socket?.close() } catch (_: Exception) {}
        }
    }

    suspend fun sendSync(ip: String, port: Int, payload: SyncDataPayload): Result<SyncResponse> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$ip:$port/sync"
            val jsonBody = json.encodeToString(payload)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val syncResponse = json.decodeFromString<SyncResponse>(body)
                    Result.success(syncResponse)
                } else {
                    Log.w(TAG, "Sync to $ip:$port failed with HTTP ${response.code}: $body")
                    Result.failure(Exception("HTTP ${response.code}: $body"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing sync to $ip:$port", e)
            Result.failure(e)
        }
    }

    suspend fun sendCheer(ip: String, port: Int, payload: SyncCheerPayload): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = "http://$ip:$port/cheer"
            val jsonBody = json.encodeToString(payload)
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(true)
                } else {
                    Result.failure(Exception("Cheer failed with HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending cheer to $ip:$port", e)
            Result.failure(e)
        }
    }
}
