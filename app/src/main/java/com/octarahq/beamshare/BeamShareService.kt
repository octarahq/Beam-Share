package com.octarahq.beamshare

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import java.util.UUID
import com.octarahq.beamshare.MainActivity

class BeamShareService : Service() {
    private val tag = "BeamShareService"
    private val channelId = "beamshare_service_channel"
    private val transferChannelId = "beamshare_transfer_channel"
    private val notificationId = 1

    private lateinit var nsdHelper: NsdHelper
    private lateinit var settingsManager: SettingsManager
    private var serverSocket: ServerSocket? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val secureRandom = SecureRandom()

    override fun onCreate() {
        super.onCreate()
        nsdHelper = NsdHelper.getInstance(this)
        settingsManager = SettingsManager(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val visibilityMode = settingsManager.visibilityMode

        BeamShareTileService.requestUpdate(this)

        if (visibilityMode == VisibilityMode.DISABLED) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(notificationId, notification)
        }

        startServer()

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val mode = settingsManager.visibilityMode
        val method = settingsManager.transferMethod

        val modeText = when (mode) {
            VisibilityMode.TRUSTED -> "Appareils de confiance"
            VisibilityMode.EVERYONE_TEMP, VisibilityMode.EVERYONE_ALWAYS -> "Tout le monde"
            else -> "Inactif"
        }

        val methodText = when (method) {
            TransferMethod.WIFI_LOCAL_ONLY -> "Wi-Fi local"
            TransferMethod.BLUETOOTH_CLASSIC -> "Bluetooth Classic"
            TransferMethod.WIFI_DIRECT_HYBRID -> "Wi-Fi Direct Hybride"
            TransferMethod.AUTO -> "Automatique"
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Beam Share est actif")
            .setContentText("Mode : $methodText • Visibilité : $modeText")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startServer() {
        if (serverSocket == null) {
            try {
                serverSocket = ServerSocket(0)
                serviceScope.launch {
                    while (isActive) {
                        try {
                            val clientSocket = serverSocket?.accept()
                            clientSocket?.let { handleIncomingConnection(it) }
                        } catch (e: Exception) {
                            if (isActive) Log.e(tag, "Accept error", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to create server socket", e)
                return
            }
        }

        val port = serverSocket?.localPort ?: 8888
        nsdHelper.registerService(settingsManager.deviceName, port)
    }

    private fun handleIncomingConnection(socket: Socket) {
        serviceScope.launch {
            val transactionId = System.currentTimeMillis().toInt()

            try {
                socket.soTimeout = 60000
                val input = socket.getInputStream()
                val output = socket.getOutputStream()
                val reader = BufferedReader(InputStreamReader(input))
                val writer = PrintWriter(output, true)

                val metaLine = reader.readLine() ?: return@launch
                val meta = JSONObject(metaLine)
                val fileName = meta.getString("name")
                val fileSize = meta.getLong("size")
                val senderId = meta.getString("sender_id")
                val senderName = meta.optString("sender_name", "Appareil distant")
                val encryptionRequested = meta.optBoolean("encryption_supported", false)

                if (settingsManager.isDeviceBlacklisted(senderId)) {
                    socket.close()
                    return@launch
                }

                val nonce = ByteArray(32)
                secureRandom.nextBytes(nonce)
                val nonceHex = nonce.joinToString("") { "%02x".format(it) }
                writer.println(nonceHex)

                val signatureHex = reader.readLine() ?: return@launch
                if (!CryptoManager.verifySignature(senderId, nonce, signatureHex)) {
                    writer.println("ERROR_CRYPTO_INVALID")
                    socket.close()
                    return@launch
                }

                val isTrusted = settingsManager.isDeviceTrusted(senderId)
                val verificationCode = CryptoManager.calculateVerificationCode(senderId.decodeHex())

                showIncomingNotification(senderName, fileName, isTrusted, senderId, transactionId)

                val request = IncomingTransferRequest(
                    senderName = senderName,
                    senderId = senderId,
                    fileName = fileName,
                    fileSize = fileSize,
                    fileType = meta.optString("type", "application/octet-stream"),
                    verificationCode = verificationCode,
                    isTrusted = isTrusted,
                    isEncrypted = encryptionRequested
                )

                val accepted = IncomingTransferManager.waitForUserDecision(request)
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (accepted) {
                    writer.println("OK")
                    settingsManager.addTrustedDevice(senderId, senderName)

                    notificationManager.cancel(transactionId)

                    val savedFileUriStr = settingsManager.downloadUri
                    var finalSavedPath: String? = null

                    try {
                        val rawInput = socket.getInputStream()
                        val secretKey = if (encryptionRequested) CryptoManager.generateSharedSecret(senderId) else null
                        
                        val inputStreamToUse = if (secretKey != null) {
                            CryptoManager.getDecryptingStream(rawInput, secretKey)
                        } else {
                            rawInput
                        }

                        val outputStream: OutputStream? = if (!savedFileUriStr.isNullOrEmpty()) {
                            val savedFileUri = Uri.parse(savedFileUriStr)
                            val pickedDir = DocumentFile.fromTreeUri(this@BeamShareService, savedFileUri)
                            val file = pickedDir?.createFile(meta.optString("type", "*/*"), fileName)
                            finalSavedPath = file?.uri?.toString()
                            file?.uri?.let { contentResolver.openOutputStream(it) }
                        } else {
                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            val beamShareDir = File(downloadsDir, "BeamShare")
                            if (!beamShareDir.exists()) beamShareDir.mkdirs()
                            val receivedFile = File(beamShareDir, fileName)
                            finalSavedPath = receivedFile.absolutePath
                            receivedFile.outputStream()
                        }

                        if (outputStream == null) throw Exception("Error creating file")

                        inputStreamToUse.use { encryptedInput ->
                            outputStream.use { fileOut ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalReceived = 0L

                                while (totalReceived < fileSize) {
                                    val toRead = if ((fileSize - totalReceived) < 8192) (fileSize - totalReceived).toInt() else 8192
                                    bytesRead = encryptedInput.read(buffer, 0, toRead)
                                    if (bytesRead == -1) break
                                    fileOut.write(buffer, 0, bytesRead)
                                    totalReceived += bytesRead

                                    val progress = (totalReceived.toFloat() / fileSize.toFloat())
                                    IncomingTransferManager.updateProgress(progress)
                                    updateTransferNotification(senderName, fileName, progress, transactionId)
                                }
                            }
                        }

                        settingsManager.addHistoryItem(HistoryItem(
                            id = UUID.randomUUID().toString(),
                            fileName = fileName,
                            deviceName = senderName,
                            timestamp = System.currentTimeMillis(),
                            isIncoming = true,
                            success = true
                        ))
                        IncomingTransferManager.updateStatus(TransferStatus.COMPLETED, finalSavedPath)
                        showFinishedNotification(senderName, fileName, true, transactionId)

                    } catch (e: Exception) {
                        settingsManager.addHistoryItem(HistoryItem(
                            id = UUID.randomUUID().toString(),
                            fileName = fileName,
                            deviceName = senderName,
                            timestamp = System.currentTimeMillis(),
                            isIncoming = true,
                            success = false
                        ))
                        IncomingTransferManager.updateStatus(TransferStatus.FAILED, error = e.message)
                        showFinishedNotification(senderName, fileName, false, transactionId)
                    }
                } else {
                    writer.println("CANCEL")
                    IncomingTransferManager.clear()
                    notificationManager.cancel(transactionId)
                }

            } catch (e: Exception) {
                IncomingTransferManager.updateStatus(TransferStatus.FAILED, error = e.message)
            } finally {
                socket.close()
            }
        }
    }

    private fun showIncomingNotification(sender: String, fileName: String, isTrusted: Boolean, senderId: String, notificationId: Int) {
        val acceptIntent = Intent(this, TransferReceiver::class.java).apply {
            action = "com.octarahq.beamshare.ACTION_ACCEPT"
        }
        val declineIntent = Intent(this, TransferReceiver::class.java).apply {
            action = "com.octarahq.beamshare.ACTION_DECLINE"
        }
        val blockIntent = Intent(this, TransferReceiver::class.java).apply {
            action = "com.octarahq.beamshare.ACTION_BLOCK"
            putExtra("sender_id", senderId)
        }

        val acceptPending = PendingIntent.getBroadcast(this, notificationId + 1, acceptIntent, PendingIntent.FLAG_IMMUTABLE)
        val declinePending = PendingIntent.getBroadcast(this, notificationId + 2, declineIntent, PendingIntent.FLAG_IMMUTABLE)
        val blockPending = PendingIntent.getBroadcast(this, notificationId + 3, blockIntent, PendingIntent.FLAG_IMMUTABLE)

        val intent = Intent(this, IncomingTransferActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(this, transferChannelId)
            .setContentTitle("Demande de transfert")
            .setContentText("$sender veut vous envoyer un fichier")
            .setSubText(fileName)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setDefaults(Notification.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Expéditeur : $sender\nFichier : $fileName"
            ))
            .addAction(android.R.drawable.ic_input_add, "Accepter", acceptPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Refuser", declinePending)

        if (!isTrusted) {
            notificationBuilder.addAction(android.R.drawable.ic_lock_lock, "Bloquer", blockPending)
        } else {
            notificationBuilder.setFullScreenIntent(pendingIntent, true)
        }

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notificationBuilder.build())
    }

    private fun updateTransferNotification(sender: String, fileName: String, progress: Float, notificationId: Int) {
        val intent = Intent(this, IncomingTransferActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, transferChannelId)
            .setContentTitle("Réception de $fileName")
            .setContentText("Depuis $sender")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, (progress * 100).toInt(), false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }

    private fun showFinishedNotification(sender: String, fileName: String, success: Boolean, notificationId: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (!success) {
            val notification = NotificationCompat.Builder(this, transferChannelId)
                .setContentTitle("Échec du transfert")
                .setContentText("Le fichier $fileName n'a pas pu être reçu.")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .build()
            manager.notify(notificationId, notification)
            return
        }

        val intent = Intent(this, IncomingTransferActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, notificationId, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, transferChannelId)
            .setContentTitle("Transfert terminé")
            .setContentText("$fileName est prêt.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(notificationId, notification)
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                channelId,
                "BeamShare Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service d'arrière-plan"
                setShowBadge(false)
            }
            manager?.createNotificationChannel(serviceChannel)

            val transferChannel = NotificationChannel(
                transferChannelId,
                "Demandes de transfert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager?.createNotificationChannel(transferChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        nsdHelper.unregisterService()
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        super.onDestroy()
    }
}
