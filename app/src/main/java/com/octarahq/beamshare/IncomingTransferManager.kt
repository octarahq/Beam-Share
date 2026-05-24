package beamshare.octarahq.com

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.coroutines.resume

enum class TransferStatus {
    PENDING,
    TRANSFERRING,
    COMPLETED,
    FAILED
}

data class IncomingTransferRequest(
    val senderName: String,
    val senderId: String,
    val fileName: String,
    val fileSize: Long,
    val fileType: String,
    val verificationCode: String,
    val isTrusted: Boolean,
    val isEncrypted: Boolean = false,
    val status: TransferStatus = TransferStatus.PENDING,
    val progress: Float = 0f,
    val savedFilePath: String? = null,
    val errorMessage: String? = null
) {
    fun accept() = IncomingTransferManager.decide(true)
    fun decline() = IncomingTransferManager.decide(false)
}

object IncomingTransferManager {
    private val _currentRequest = MutableStateFlow<IncomingTransferRequest?>(null)
    val currentRequest = _currentRequest.asStateFlow()

    private var decisionContinuation: (Boolean) -> Unit = {}

    suspend fun waitForUserDecision(request: IncomingTransferRequest): Boolean {
        return kotlin.coroutines.suspendCoroutine { continuation ->
            decisionContinuation = { accepted ->
                if (!accepted) {
                    _currentRequest.value = null
                } else {
                    _currentRequest.update { it?.copy(status = TransferStatus.TRANSFERRING) }
                }
                continuation.resume(accepted)
            }
            _currentRequest.value = request
        }
    }

    fun decide(accepted: Boolean) {
        decisionContinuation(accepted)
    }

    fun updateProgress(progress: Float) {
        _currentRequest.update { it?.copy(progress = progress) }
    }

    fun updateStatus(status: TransferStatus, savedFilePath: String? = null, error: String? = null) {
        _currentRequest.update {
            it?.copy(status = status, savedFilePath = savedFilePath, errorMessage = error)
        }
    }

    fun clear() {
        _currentRequest.value = null
    }
}
