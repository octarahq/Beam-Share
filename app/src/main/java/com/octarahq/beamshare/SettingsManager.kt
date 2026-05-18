package com.octarahq.beamshare

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

enum class VisibilityMode {
    DISABLED,
    TRUSTED,
    EVERYONE_TEMP,
    EVERYONE_ALWAYS
}

enum class TransferMethod {
    WIFI_LOCAL_ONLY,
    BLUETOOTH_CLASSIC,
    WIFI_DIRECT_HYBRID,
    AUTO
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class HistoryItem(
    val id: String,
    val fileName: String,
    val deviceName: String,
    val timestamp: Long,
    val isIncoming: Boolean,
    val success: Boolean
)

class SettingsManager(context: Context) {
    private val contentResolver = context.contentResolver
    private val prefs = context.getSharedPreferences("beamshare_settings", Context.MODE_PRIVATE)

    var deviceName: String
        get() {
            val savedName = prefs.getString("device_name", null)
            if (savedName != null) return savedName
            
            val systemName = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    Settings.Global.getString(contentResolver, Settings.Global.DEVICE_NAME)
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }

            val bluetoothName = try {
                Settings.Secure.getString(contentResolver, "bluetooth_name")
            } catch (_: Exception) {
                null
            }

            return systemName ?: bluetoothName ?: Build.MODEL
        }
        set(value) = prefs.edit { putString("device_name", value) }

    var visibilityMode: VisibilityMode
        get() = try {
            VisibilityMode.valueOf(prefs.getString("visibility_mode", VisibilityMode.DISABLED.name) ?: VisibilityMode.DISABLED.name)
        } catch (e: Exception) {
            VisibilityMode.DISABLED
        }
        set(value) = prefs.edit { putString("visibility_mode", value.name) }

    var everyoneTimeoutMinutes: Int
        get() = prefs.getInt("everyone_timeout", 10)
        set(value) = prefs.edit { putInt("everyone_timeout", value) }

    var downloadUri: String?
        get() = prefs.getString("download_uri", null)
        set(value) = prefs.edit { putString("download_uri", value) }

    var transferMethod: TransferMethod
        get() = try {
            TransferMethod.valueOf(prefs.getString("transfer_method", TransferMethod.AUTO.name) ?: TransferMethod.AUTO.name)
        } catch (_: Exception) {
            TransferMethod.AUTO
        }
        set(value) = prefs.edit { putString("transfer_method", value.name) }

    var themeMode: ThemeMode
        get() = try {
            ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
        set(value) = prefs.edit { putString("theme_mode", value.name) }

    fun isDeviceTrusted(publicKey: String): Boolean {
        return getTrustedDevicesMap().containsKey(publicKey)
    }

    fun getTrustedDevicesMap(): Map<String, String?> {
        val json = prefs.getString("trusted_devices_map", "{}") ?: "{}"
        val map = mutableMapOf<String, String?>()
        try {
            val obj = JSONObject(json)
            obj.keys().forEach { key ->
                map[key] = if (obj.isNull(key)) null else obj.getString(key)
            }
        } catch (_: Exception) {}
        return map
    }

    fun addTrustedDevice(publicKey: String, deviceName: String?) {
        removeBlacklistedDevice(publicKey)
        val map = getTrustedDevicesMap().toMutableMap()
        map[publicKey] = deviceName
        
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v ?: JSONObject.NULL) }
        prefs.edit { putString("trusted_devices_map", obj.toString()) }
    }

    fun removeTrustedDevice(publicKey: String) {
        val map = getTrustedDevicesMap().toMutableMap()
        map.remove(publicKey)
        
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v ?: JSONObject.NULL) }
        prefs.edit { putString("trusted_devices_map", obj.toString()) }
    }

    fun getTrustedDevices(): Set<String> {
        return getTrustedDevicesMap().keys
    }

    fun isDeviceBlacklisted(publicKey: String): Boolean {
        return getBlacklistedDevices().contains(publicKey)
    }

    fun getBlacklistedDevices(): Set<String> {
        return prefs.getStringSet("blacklisted_devices", emptySet()) ?: emptySet()
    }

    fun blacklistDevice(publicKey: String) {
        removeTrustedDevice(publicKey)
        val blacklisted = getBlacklistedDevices().toMutableSet()
        blacklisted.add(publicKey)
        prefs.edit { putStringSet("blacklisted_devices", blacklisted) }
    }

    fun removeBlacklistedDevice(publicKey: String) {
        val blacklisted = getBlacklistedDevices().toMutableSet()
        blacklisted.remove(publicKey)
        prefs.edit { putStringSet("blacklisted_devices", blacklisted) }
    }

    fun addHistoryItem(item: HistoryItem) {
        val history = getHistory().toMutableList()
        history.add(0, item)
        if (history.size > 50) history.removeAt(history.size - 1)
        
        val array = JSONArray()
        history.forEach {
            val obj = JSONObject().apply {
                put("id", it.id)
                put("fileName", it.fileName)
                put("deviceName", it.deviceName)
                put("timestamp", it.timestamp)
                put("isIncoming", it.isIncoming)
                put("success", it.success)
            }
            array.put(obj)
        }
        prefs.edit { putString("transfer_history", array.toString()) }
    }

    fun getHistory(): List<HistoryItem> {
        val json = prefs.getString("transfer_history", null) ?: return emptyList()
        val list = mutableListOf<HistoryItem>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(HistoryItem(
                    obj.getString("id"),
                    obj.getString("fileName"),
                    obj.getString("deviceName"),
                    obj.getLong("timestamp"),
                    obj.getBoolean("isIncoming"),
                    obj.getBoolean("success")
                ))
            }
        } catch (_: Exception) {}
        return list
    }
    
    fun clearHistory() {
        prefs.edit { remove("transfer_history") }
    }
}
