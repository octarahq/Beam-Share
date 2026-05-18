package com.octarahq.beamshare

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DeviceTransferState(
    val status: String = "",
    val progress: Float = 0f,
    val isTransferring: Boolean = false,
    val verificationCode: String? = null,
    val finishedMessage: String? = null,
    val isError: Boolean = false
)

class SendViewModel(application: Application) : AndroidViewModel(application) {
    private val nsdHelper = NsdHelper.getInstance(application)
    private val transferService = TransferService(application)
    private val appContext = application.applicationContext
    
    val devices = combine(nsdHelper.discoveredDevices, nsdHelper.localDevice) { discovered, local ->
        if (local != null) {
            listOf(local) + discovered.filter { it.name != local.name.removeSuffix(" (Moi)") }
        } else {
            discovered
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    var selectedFileUri by mutableStateOf<Uri?>(null)
    var selectedFileName by mutableStateOf<String?>(null)
    
    val transferStates = mutableStateMapOf<String, DeviceTransferState>()
    private val activeJobs = mutableMapOf<String, Job>()

    val isAnyTransferActive get() = transferStates.values.any { it.isTransferring }

    fun startDiscovery() {
        nsdHelper.startDiscovery()
    }

    fun stopDiscovery() {
        nsdHelper.stopDiscovery()
    }

    fun selectFile(uri: Uri) {
        selectedFileUri = uri
        selectedFileName = getFileName(uri)
    }

    fun selectDevice(device: BeamDevice) {
        val uri = selectedFileUri ?: return
        send(device, uri)
    }

    fun cancelTransfer(deviceName: String) {
        activeJobs[deviceName]?.cancel()
        activeJobs.remove(deviceName)
        transferStates[deviceName] = DeviceTransferState(
            isTransferring = false,
            finishedMessage = appContext.getString(R.string.status_cancelled),
            isError = true
        )
    }

    private fun send(device: BeamDevice, uri: Uri) {
        val myId = CryptoManager.getVerificationCode()
        val job = viewModelScope.launch {
            try {
                transferStates[device.name] = DeviceTransferState(
                    status = appContext.getString(R.string.status_preparing),
                    progress = -1f,
                    isTransferring = true,
                    verificationCode = myId
                )

                val uploadDurationMs = transferService.sendFile(device, uri) { status, progress ->
                    transferStates[device.name] = DeviceTransferState(
                        status = status,
                        progress = progress,
                        isTransferring = true,
                        verificationCode = myId
                    )
                }

                val formattedTime = if (uploadDurationMs < 1000) {
                    "${uploadDurationMs}ms"
                } else {
                    "%.1fs".format(uploadDurationMs / 1000f)
                }

                transferStates[device.name] = DeviceTransferState(
                    isTransferring = false,
                    finishedMessage = appContext.getString(R.string.status_finished_time, formattedTime),
                    verificationCode = myId
                )
            } catch (e: Exception) {
                if (activeJobs.containsKey(device.name)) {
                    transferStates[device.name] = DeviceTransferState(
                        isTransferring = false,
                        finishedMessage = appContext.getString(R.string.status_error, e.message),
                        isError = true,
                        verificationCode = myId
                    )
                }
            } finally {
                activeJobs.remove(device.name)
            }
        }
        activeJobs[device.name] = job
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
