package com.octarahq.beamshare

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReceiveViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    
    private val _visibilityMode = MutableStateFlow(settingsManager.visibilityMode)
    val visibilityMode = _visibilityMode.asStateFlow()
    
    private val _deviceName = MutableStateFlow(settingsManager.deviceName)
    val deviceName = _deviceName.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private val _everyoneTimeoutMinutes = MutableStateFlow(settingsManager.everyoneTimeoutMinutes)
    val everyoneTimeoutMinutes = _everyoneTimeoutMinutes.asStateFlow()

    private val _downloadPathDisplay = MutableStateFlow(getDisplayPath(settingsManager.downloadUri))
    val downloadPathDisplay = _downloadPathDisplay.asStateFlow()

    private val _transferMethod = MutableStateFlow(settingsManager.transferMethod)
    val transferMethod = _transferMethod.asStateFlow()

    private val _themeMode = MutableStateFlow(settingsManager.themeMode)
    val themeMode = _themeMode.asStateFlow()

    private var timeoutJob: Job? = null

    init {
        updateServiceState()
    }

    fun updateDownloadUri(uri: Uri?) {
        val context = getApplication<Application>()
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            settingsManager.downloadUri = it.toString()
            _downloadPathDisplay.value = getDisplayPath(it.toString())
        }
    }

    private fun getDisplayPath(uriString: String?): String {
        return uriString?.let {
            val uri = Uri.parse(it)
            uri.path?.split(":")?.lastOrNull() ?: it
        } ?: "Downloads/BeamShare"
    }

    fun setVisibilityMode(mode: VisibilityMode) {
        settingsManager.visibilityMode = mode
        if (mode == VisibilityMode.EVERYONE_TEMP) {
            _remainingSeconds.value = _everyoneTimeoutMinutes.value * 60
        } else {
            _remainingSeconds.value = 0
        }
        updateServiceState()
    }

    fun adjustTimeout(delta: Int) {
        val newVal = (_everyoneTimeoutMinutes.value + delta).coerceIn(1, 60)
        settingsManager.everyoneTimeoutMinutes = newVal
        _everyoneTimeoutMinutes.value = newVal
        
        if (_visibilityMode.value == VisibilityMode.EVERYONE_TEMP) {
            _remainingSeconds.value = newVal * 60
        }
    }

    fun updateDeviceName(newName: String) {
        settingsManager.deviceName = newName
        _deviceName.value = newName
        updateServiceState()
    }

    fun setTransferMethod(method: TransferMethod) {
        settingsManager.transferMethod = method
        _transferMethod.value = method
        updateServiceState()
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsManager.themeMode = mode
        _themeMode.value = mode
    }

    private fun updateServiceState() {
        _visibilityMode.value = settingsManager.visibilityMode
        _deviceName.value = settingsManager.deviceName
        
        val context = getApplication<Application>()
        BeamShareTileService.requestUpdate(context)

        val serviceIntent = Intent(context, BeamShareService::class.java)

        if (settingsManager.visibilityMode != VisibilityMode.DISABLED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
            
            timeoutJob?.cancel()
            if (settingsManager.visibilityMode == VisibilityMode.EVERYONE_TEMP) {
                startTimeoutCounter()
            }
        } else {
            context.stopService(serviceIntent)
            timeoutJob?.cancel()
        }
    }

    private fun startTimeoutCounter() {
        timeoutJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000)
                _remainingSeconds.update { it - 1 }
            }
            setVisibilityMode(VisibilityMode.TRUSTED)
        }
    }
}
