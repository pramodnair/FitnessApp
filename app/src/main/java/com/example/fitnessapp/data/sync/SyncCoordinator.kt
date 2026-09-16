package com.example.fitnessapp.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.fitnessapp.data.model.UserDailyScore
import com.example.fitnessapp.data.repository.FitnessRepository
import com.example.fitnessapp.data.sync.wifi.LocalNsdDiscovery
import com.example.fitnessapp.data.sync.wifi.LocalSyncClient
import com.example.fitnessapp.data.sync.wifi.LocalSyncServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

class SyncCoordinator(
    private val context: Context,
    private val repository: FitnessRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "SyncCoordinator"
    }

    private val installationId: String = run {
        val prefs = context.getSharedPreferences("fitness_sync_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("sync_installation_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString("sync_installation_id", id).apply()
        }
        id
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _connectionStatus = MutableStateFlow<SyncConnectionStatus>(SyncConnectionStatus.Disconnected)
    val connectionStatus: StateFlow<SyncConnectionStatus> = _connectionStatus.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _localDeviceIp = MutableStateFlow<String?>(null)
    val localDeviceIp: StateFlow<String?> = _localDeviceIp.asStateFlow()

    private val _localPort = MutableStateFlow<Int>(LocalSyncServer.DEFAULT_PORT)
    val localPort: StateFlow<Int> = _localPort.asStateFlow()

    private val _isScanningSubnet = MutableStateFlow(false)
    val isScanningSubnet: StateFlow<Boolean> = _isScanningSubnet.asStateFlow()

    private val syncClient = LocalSyncClient()
    private var syncServer: LocalSyncServer? = null
    private var nsdDiscovery: LocalNsdDiscovery? = null

    private var partnerIp: String? = null
    private var partnerPort: Int = 0
    private var partnerName: String? = null

    private var autoSyncJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    init {
        repository.setOnDataChangedListener {
            triggerAutoSync()
        }
    }

    fun start() {
        Log.i(TAG, "Starting SyncCoordinator...")
        registerNetworkCallback()
        checkInitialNetworkState()
    }

    fun stop() {
        Log.i(TAG, "Stopping SyncCoordinator...")
        unregisterNetworkCallback()
        stopWifiServices()
        _connectionStatus.value = SyncConnectionStatus.Disconnected
    }

    fun setPairCode(code: String) {
        repository.setPairCode(code)
        // If searching or connected, re-probe partner
        if (partnerIp != null && partnerPort > 0) {
            coroutineScope.launch {
                probeDiscoveredPeer(partnerIp!!, partnerPort, partnerName ?: "Partner")
            }
        }
    }

    fun getPairCode(): String = repository.pairCode.value

    fun triggerManualSync(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        coroutineScope.launch {
            _isSyncing.value = true
            try {
                val ip = partnerIp
                val port = partnerPort

                if (ip != null && port > 0) {
                    val myScore = repository.getCurrentUserDailyScore()
                    val pairCode = repository.pairCode.value

                    val payload = SyncDataPayload(
                        pairCode = pairCode,
                        senderScore = myScore
                    )

                    val result = syncClient.sendSync(ip, port, payload)
                    result.onSuccess { response ->
                        if (response.status == "ACCEPTED") {
                            response.recipientScore?.let { partnerScore ->
                                repository.updateSyncedPartnerScore(partnerScore)
                            }
                            _connectionStatus.value = SyncConnectionStatus.ConnectedWifi(
                                partnerIp = ip,
                                partnerName = partnerName ?: "Partner",
                                lastSyncedTime = System.currentTimeMillis()
                            )
                            onComplete(true, "Synced successfully with ${partnerName ?: "partner"} via Wi-Fi!")
                        } else {
                            onComplete(false, "Sync rejected: ${response.message}")
                        }
                    }.onFailure { error ->
                        onComplete(false, "Sync error: ${error.message}")
                    }
                } else {
                    // Fallback mode check
                    _connectionStatus.value = SyncConnectionStatus.CloudFallback(
                        isConfigured = false,
                        statusMessage = "Wi-Fi partner not found. Ready for Firebase Cloud Sync."
                    )
                    onComplete(false, "Searching for partner on Wi-Fi (make sure both phones are on same Wi-Fi with code ${getPairCode()})")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Manual sync exception", e)
                onComplete(false, "Error: ${e.message}")
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun sendCheer(message: String, onResult: (Boolean) -> Unit = {}) {
        coroutineScope.launch {
            val ip = partnerIp
            val port = partnerPort
            val activeUser = repository.activeProfile.value

            if (ip != null && port > 0) {
                val payload = SyncCheerPayload(
                    pairCode = repository.pairCode.value,
                    senderName = activeUser.name,
                    message = message
                )
                val result = syncClient.sendCheer(ip, port, payload)
                onResult(result.isSuccess)
            } else {
                onResult(false)
            }
        }
    }

    private fun triggerAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = coroutineScope.launch {
            delay(1500) // Debounce rapid writes
            val ip = partnerIp
            val port = partnerPort
            if (ip != null && port > 0) {
                Log.d(TAG, "Triggering automatic background sync to $ip:$port...")
                val myScore = repository.getCurrentUserDailyScore()
                val payload = SyncDataPayload(
                    pairCode = repository.pairCode.value,
                    senderScore = myScore
                )
                val result = syncClient.sendSync(ip, port, payload)
                result.onSuccess { response ->
                    response.recipientScore?.let {
                        repository.updateSyncedPartnerScore(it)
                    }
                    _connectionStatus.value = SyncConnectionStatus.ConnectedWifi(
                        partnerIp = ip,
                        partnerName = partnerName ?: "Partner",
                        lastSyncedTime = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private fun checkInitialNetworkState() {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            startWifiServices()
        } else if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            _connectionStatus.value = SyncConnectionStatus.CloudFallback(
                isConfigured = false,
                statusMessage = "Using Mobile Cellular Data. Cloud Sync Ready."
            )
        } else {
            _connectionStatus.value = SyncConnectionStatus.Disconnected
        }
    }

    private fun registerNetworkCallback() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    startWifiServices()
                } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    stopWifiServices()
                    _connectionStatus.value = SyncConnectionStatus.CloudFallback(
                        isConfigured = false,
                        statusMessage = "Using Mobile Cellular Data. Cloud Sync Ready."
                    )
                }
            }

            override fun onLost(network: Network) {
                stopWifiServices()
                _connectionStatus.value = SyncConnectionStatus.Disconnected
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering network callback", e)
            }
            networkCallback = null
        }
    }

    @Synchronized
    private fun startWifiServices() {
        if (syncServer?.isRunning == true) return

        _connectionStatus.value = SyncConnectionStatus.SearchingWifi
        val activeProfile = repository.activeProfile.value

        // Resolve and update current device Wi-Fi IP
        val currentIp = getLocalIpAddress()
        _localDeviceIp.value = currentIp
        Log.i(TAG, "Local device Wi-Fi IP is: $currentIp")

        syncServer = LocalSyncServer(
            expectedPairCodeProvider = { repository.pairCode.value },
            localHandshakeProvider = { boundPort ->
                SyncHandshake(
                    deviceName = android.os.Build.MODEL,
                    userId = activeProfile.id,
                    userName = activeProfile.name,
                    pairCode = repository.pairCode.value,
                    port = boundPort,
                    installationId = installationId
                )
            },
            localScoreProvider = { repository.getCurrentUserDailyScore() },
            onPartnerScoreReceived = { score, cheer ->
                repository.updateSyncedPartnerScore(score)
                cheer?.let { repository.receiveIncomingCheer(score.userName, it) }
            },
            onCheerReceived = { sender, message ->
                repository.receiveIncomingCheer(sender, message)
            }
        )

        val port = syncServer?.start() ?: 0
        if (port > 0) {
            _localPort.value = port
            val serviceName = "FitnessApp-${activeProfile.name.filter { it.isLetterOrDigit() }}"
            nsdDiscovery = LocalNsdDiscovery(
                context = context,
                onPeerDiscovered = { ip, peerPort, peerName ->
                    coroutineScope.launch {
                        probeDiscoveredPeer(ip, peerPort, peerName)
                    }
                },
                onPeerLost = { peerName ->
                    if (peerName == partnerName) {
                        partnerIp = null
                        partnerPort = 0
                        _connectionStatus.value = SyncConnectionStatus.SearchingWifi
                    }
                }
            )
            nsdDiscovery?.start(serviceName, port)

            // Auto-trigger subnet sweep in case router blocks mDNS/NSD multicast
            if (partnerIp == null && currentIp != null) {
                coroutineScope.launch {
                    delay(1200)
                    if (partnerIp == null) {
                        scanSubnetForPartner()
                    }
                }
            }
        }
    }

    @Synchronized
    private fun stopWifiServices() {
        nsdDiscovery?.stop()
        nsdDiscovery = null
        syncServer?.stop()
        syncServer = null
        partnerIp = null
        partnerPort = 0
        _localDeviceIp.value = null
    }

    suspend fun scanSubnetForPartner(onResult: (Boolean, String) -> Unit = { _, _ -> }): Boolean {
        val localIp = _localDeviceIp.value ?: getLocalIpAddress()
        if (localIp == null) {
            Log.w(TAG, "Subnet scan aborted: No local Wi-Fi IP detected")
            onResult(false, "Not connected to Wi-Fi. Connect both phones to Wi-Fi.")
            return false
        }
        _localDeviceIp.value = localIp

        val subnetPrefix = localIp.substringBeforeLast(".") + "."
        val selfLastOctet = localIp.substringAfterLast(".").toIntOrNull() ?: -1
        val myPairCode = repository.pairCode.value.trim().uppercase()

        Log.i(TAG, "Starting fast subnet probe on ${subnetPrefix}1-254 (ignoring self .$selfLastOctet, code $myPairCode)...")
        _isScanningSubnet.value = true

        val semaphore = Semaphore(25)
        var matchFound = false
        val jobs = mutableListOf<Job>()

        try {
            coroutineScope {
                for (octet in 1..254) {
                    if (octet == selfLastOctet) continue
                    val targetIp = "$subnetPrefix$octet"
                    val job = launch {
                        if (matchFound) return@launch
                        semaphore.withPermit {
                            if (matchFound) return@withPermit
                            val targetPort = LocalSyncServer.DEFAULT_PORT
                            val res = syncClient.quickPing(targetIp, targetPort)
                            res.onSuccess { handshake ->
                                if (handshake.installationId.isNotEmpty() && handshake.installationId == installationId) {
                                    return@onSuccess
                                }
                                val peerCode = handshake.pairCode.trim().uppercase()
                                if (myPairCode.isNotEmpty() && myPairCode == peerCode) {
                                    Log.i(TAG, "Subnet scan MATCH: Partner '${handshake.userName}' found at $targetIp:${handshake.port}")
                                    matchFound = true
                                    partnerIp = targetIp
                                    partnerPort = handshake.port
                                    partnerName = handshake.userName

                                    _connectionStatus.value = SyncConnectionStatus.ConnectedWifi(
                                        partnerIp = targetIp,
                                        partnerName = handshake.userName
                                    )

                                    // Initial handshake sync
                                    val myScore = repository.getCurrentUserDailyScore()
                                    val syncRes = syncClient.sendSync(
                                        targetIp, handshake.port,
                                        SyncDataPayload(pairCode = myPairCode, senderScore = myScore)
                                    )
                                    syncRes.onSuccess { response ->
                                        response.recipientScore?.let { repository.updateSyncedPartnerScore(it) }
                                    }
                                }
                            }
                        }
                    }
                    jobs.add(job)
                }
                jobs.forEach { it.join() }
            }
        } finally {
            _isScanningSubnet.value = false
        }

        return if (matchFound) {
            onResult(true, "Found and connected to partner ($partnerIp)!")
            true
        } else {
            onResult(false, "No partner found on Wi-Fi subnet with code '$myPairCode'")
            false
        }
    }

    fun connectToPartnerDirectly(targetIp: String, targetPort: Int = LocalSyncServer.DEFAULT_PORT, onResult: (Boolean, String) -> Unit) {
        coroutineScope.launch {
            val trimmedIp = targetIp.trim()
            if (trimmedIp.isBlank()) {
                onResult(false, "IP address cannot be empty")
                return@launch
            }
            if (isOwnIpAddress(trimmedIp)) {
                onResult(false, "That is your own device's IP ($trimmedIp)")
                return@launch
            }

            _isSyncing.value = true
            Log.i(TAG, "Direct connect attempt to $trimmedIp:$targetPort...")
            val pingResult = syncClient.ping(trimmedIp, targetPort)
            pingResult.onSuccess { handshake ->
                if (handshake.installationId.isNotEmpty() && handshake.installationId == installationId) {
                    onResult(false, "Cannot connect: this is your own device")
                    _isSyncing.value = false
                    return@onSuccess
                }
                val myPairCode = repository.pairCode.value.trim().uppercase()
                val peerCode = handshake.pairCode.trim().uppercase()
                if (myPairCode.isNotEmpty() && myPairCode != peerCode) {
                    onResult(false, "Pair code mismatch: partner has '$peerCode', you have '$myPairCode'")
                    _isSyncing.value = false
                    return@onSuccess
                }

                partnerIp = trimmedIp
                partnerPort = handshake.port
                partnerName = handshake.userName
                _connectionStatus.value = SyncConnectionStatus.ConnectedWifi(
                    partnerIp = trimmedIp,
                    partnerName = handshake.userName
                )

                // Sync immediately
                val myScore = repository.getCurrentUserDailyScore()
                val syncRes = syncClient.sendSync(
                    trimmedIp, handshake.port,
                    SyncDataPayload(pairCode = myPairCode, senderScore = myScore)
                )
                syncRes.onSuccess { res ->
                    res.recipientScore?.let { repository.updateSyncedPartnerScore(it) }
                }

                _isSyncing.value = false
                onResult(true, "Connected to ${handshake.userName} ($trimmedIp)!")
            }.onFailure { err ->
                _isSyncing.value = false
                onResult(false, "Could not reach $trimmedIp:$targetPort (${err.message})")
            }
        }
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error obtaining local IP", e)
        }
        return null
    }

    fun isOwnIpAddress(ip: String): Boolean {
        val current = _localDeviceIp.value ?: getLocalIpAddress()
        if (current != null && current == ip) return true
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            for (intf in interfaces) {
                for (addr in intf.inetAddresses) {
                    if (addr.hostAddress == ip) return true
                }
            }
        } catch (_: Exception) {}
        return false
    }

    private suspend fun probeDiscoveredPeer(ip: String, port: Int, peerName: String) {
        Log.i(TAG, "Probing discovered peer at $ip:$port ($peerName)...")
        val pingResult = syncClient.ping(ip, port)
        pingResult.onSuccess { handshake ->
            // Check installationId first to prevent false-positives
            if (handshake.installationId.isNotEmpty() && handshake.installationId == installationId) {
                Log.d(TAG, "Ignoring self handshake from own device ($ip:$port)")
                return@onSuccess
            }
            if (handshake.installationId.isEmpty() && isOwnIpAddress(ip)) {
                Log.d(TAG, "Ignoring self handshake from own IP ($ip:$port)")
                return@onSuccess
            }

            val myPairCode = repository.pairCode.value.trim().uppercase()
            val peerPairCode = handshake.pairCode.trim().uppercase()

            if (myPairCode.isNotEmpty() && myPairCode == peerPairCode) {
                Log.i(TAG, "Pair code match confirmed with ${handshake.userName} ($ip:$port)")
                partnerIp = ip
                partnerPort = port
                partnerName = handshake.userName

                _connectionStatus.value = SyncConnectionStatus.ConnectedWifi(
                    partnerIp = ip,
                    partnerName = handshake.userName
                )

                // Perform initial handshake sync
                val myScore = repository.getCurrentUserDailyScore()
                val syncResult = syncClient.sendSync(
                    ip, port,
                    SyncDataPayload(pairCode = myPairCode, senderScore = myScore)
                )
                syncResult.onSuccess { res ->
                    res.recipientScore?.let { repository.updateSyncedPartnerScore(it) }
                }
            } else {
                Log.w(TAG, "Peer pair code mismatch: got '$peerPairCode', local is '$myPairCode'")
            }
        }.onFailure { err ->
            Log.w(TAG, "Peer probe failed: ${err.message}")
        }
    }
}
