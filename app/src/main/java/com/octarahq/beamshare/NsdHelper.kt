package com.octarahq.beamshare

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BeamDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
    val model: String = "Unknown"
)

class NsdHelper private constructor(context: Context) {
    private val TAG = "NsdHelper"
    private val SERVICE_TYPE = "_beamshare._tcp"
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val settingsManager = SettingsManager(context)
    private val appContext = context.applicationContext
    
    private var multicastLock: WifiManager.MulticastLock? = null
    private val _discoveredDevices = MutableStateFlow<List<BeamDevice>>(emptyList())
    val discoveredDevices = _discoveredDevices.asStateFlow()

    private val _localDevice = MutableStateFlow<BeamDevice?>(null)
    val localDevice = _localDevice.asStateFlow()

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    
    private var currentRegisteredName: String? = null
    private var currentRegisteredPort: Int? = null

    companion object {
        @Volatile
        private var instance: NsdHelper? = null

        fun getInstance(context: Context): NsdHelper {
            return instance ?: synchronized(this) {
                instance ?: NsdHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    fun registerService(deviceName: String, port: Int) {
        if (currentRegisteredName == deviceName && currentRegisteredPort == port) {
            return
        }

        unregisterService()
        acquireMulticastLock()
        
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = deviceName
            serviceType = SERVICE_TYPE
            this.port = port
            setAttribute("model", Build.MODEL)
            setAttribute("device_type", "smartphone")
            setAttribute("node_id", CryptoManager.getPublicKeyHex())
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                currentRegisteredName = info.serviceName
                currentRegisteredPort = info.port
                
                _localDevice.value = BeamDevice(
                    id = CryptoManager.getPublicKeyHex(),
                    name = "${info.serviceName} ${appContext.getString(R.string.me_label)}",
                    host = "127.0.0.1",
                    port = currentRegisteredPort ?: info.port,
                    model = Build.MODEL
                )
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                registrationListener = null
                _localDevice.value = null
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                currentRegisteredName = null
                currentRegisteredPort = null
                _localDevice.value = null
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error in registerService", e)
        }
    }

    fun unregisterService() {
        registrationListener?.let {
            try {
                nsdManager.unregisterService(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error in unregisterService", e)
            }
        }
        registrationListener = null
        currentRegisteredName = null
        currentRegisteredPort = null
        _localDevice.value = null
        if (discoveryListener == null) releaseMulticastLock()
    }

    fun startDiscovery() {
        if (discoveryListener != null) return
        acquireMulticastLock()
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {}
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType.contains("_beamshare")) {
                    val myName = settingsManager.deviceName
                    if (service.serviceName.startsWith(myName) || service.serviceName == currentRegisteredName) {
                        return
                    }

                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val nodeId = info.attributes["node_id"]?.let { String(it) } ?: "unknown"
                            val myNodeId = CryptoManager.getPublicKeyHex()
                            val myName = settingsManager.deviceName
                            
                            if (nodeId == myNodeId || 
                                info.serviceName.startsWith(myName) || 
                                info.serviceName == currentRegisteredName ||
                                info.host?.hostAddress == "127.0.0.1") return

                            val model = info.attributes["model"]?.let { String(it) } ?: "Unknown"
                            val device = BeamDevice(
                                id = nodeId,
                                name = info.serviceName,
                                host = info.host?.hostAddress ?: "",
                                port = info.port,
                                model = model
                            )
                            _discoveredDevices.update { current ->
                                if (current.none { it.name == device.name }) current + device else current
                            }
                        }
                    })
                }
            }
            override fun onServiceLost(service: NsdServiceInfo) {
                _discoveredDevices.update { current -> current.filterNot { it.name == service.serviceName } }
            }
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) = stopDiscovery()
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
    }

    fun stopDiscovery() {
        discoveryListener?.let { try { nsdManager.stopServiceDiscovery(it) } catch (_: Exception) {} }
        discoveryListener = null
        _discoveredDevices.value = emptyList()
        if (registrationListener == null) releaseMulticastLock()
    }

    private fun acquireMulticastLock() {
        if (multicastLock == null || !multicastLock!!.isHeld) {
            multicastLock = wifiManager.createMulticastLock("BeamShareLock").apply {
                setReferenceCounted(false)
                acquire()
            }
        }
    }

    private fun releaseMulticastLock() {
        multicastLock?.let { if (it.isHeld) it.release() }
        multicastLock = null
    }
}
