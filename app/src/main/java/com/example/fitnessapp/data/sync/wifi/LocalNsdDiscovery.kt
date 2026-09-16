package com.example.fitnessapp.data.sync.wifi

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.net.InetAddress

class LocalNsdDiscovery(
    private val context: Context,
    private val onPeerDiscovered: (ip: String, port: Int, peerName: String) -> Unit,
    private val onPeerLost: (peerName: String) -> Unit
) {
    companion object {
        private const val TAG = "LocalNsdDiscovery"
        const val SERVICE_TYPE = "_fitnessduel._tcp."
    }

    private val nsdManager: NsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var multicastLock: WifiManager.MulticastLock? = null
    private var isDiscovering = false
    private var isRegistered = false
    private var registeredServiceName: String? = null

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun start(localServiceName: String, port: Int) {
        acquireMulticastLock()
        registerService(localServiceName, port)
        discoverServices()
    }

    fun stop() {
        stopDiscovery()
        unregisterService()
        releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (multicastLock == null && wifiManager != null) {
                multicastLock = wifiManager.createMulticastLock("FitnessAppMulticastLock").apply {
                    setReferenceCounted(true)
                    acquire()
                }
                Log.d(TAG, "Acquired Wi-Fi MulticastLock")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Unable to acquire MulticastLock", e)
        }
    }

    private fun releaseMulticastLock() {
        try {
            multicastLock?.let {
                if (it.isHeld) it.release()
            }
            multicastLock = null
            Log.d(TAG, "Released Wi-Fi MulticastLock")
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MulticastLock", e)
        }
    }

    private fun registerService(serviceName: String, port: Int) {
        if (isRegistered || port <= 0) return

        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = SERVICE_TYPE
            this.port = port
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                registeredServiceName = info.serviceName
                isRegistered = true
                Log.i(TAG, "NSD Service registered: ${info.serviceName} on port ${info.port}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                isRegistered = false
                Log.e(TAG, "NSD Registration failed: errorCode=$errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                isRegistered = false
                Log.i(TAG, "NSD Service unregistered: ${info.serviceName}")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "NSD Unregistration failed: errorCode=$errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during registerService", e)
        }
    }

    private fun unregisterService() {
        if (!isRegistered || registrationListener == null) return
        try {
            nsdManager.unregisterService(registrationListener)
            isRegistered = false
            registrationListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
        }
    }

    private val resolveQueue = java.util.concurrent.ConcurrentLinkedQueue<NsdServiceInfo>()
    private val isResolving = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun discoverServices() {
        if (isDiscovering) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                isDiscovering = true
                Log.i(TAG, "Service discovery started: $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: ${service.serviceName}")

                // Ignore our own advertised service
                if (service.serviceName == registeredServiceName) {
                    Log.d(TAG, "Ignoring self: ${service.serviceName}")
                    return
                }

                if (!service.serviceType.contains("fitnessduel")) {
                    Log.d(TAG, "Unknown service type: ${service.serviceType}")
                    return
                }

                resolveQueue.add(service)
                processNextResolve()
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.i(TAG, "Service lost: ${service.serviceName}")
                onPeerLost(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering = false
                Log.i(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
                Log.e(TAG, "Start discovery failed: errorCode=$errorCode")
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: errorCode=$errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception during discoverServices", e)
        }
    }

    private fun processNextResolve() {
        if (!isResolving.compareAndSet(false, true)) return
        val nextService = resolveQueue.poll()
        if (nextService == null) {
            isResolving.set(false)
            return
        }
        resolveFoundService(nextService)
    }

    private fun resolveFoundService(service: NsdServiceInfo) {
        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: errorCode=$errorCode")
                isResolving.set(false)
                processNextResolve()
            }

            override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                try {
                    val host: InetAddress? = serviceInfo.host
                    val port = serviceInfo.port
                    val hostAddress = host?.hostAddress
                    if (hostAddress != null && port > 0) {
                        Log.i(TAG, "Resolved partner: ${serviceInfo.serviceName} at $hostAddress:$port")
                        onPeerDiscovered(hostAddress, port, serviceInfo.serviceName)
                    }
                } finally {
                    isResolving.set(false)
                    processNextResolve()
                }
            }
        }

        try {
            nsdManager.resolveService(service, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling resolveService", e)
            isResolving.set(false)
            processNextResolve()
        }
    }

    private fun stopDiscovery() {
        resolveQueue.clear()
        isResolving.set(false)
        if (!isDiscovering || discoveryListener == null) return
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
            isDiscovering = false
            discoveryListener = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping discovery", e)
        }
    }
}
