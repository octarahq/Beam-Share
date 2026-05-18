package com.octarahq.beamshare

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID

class TransferService(context: Context) {
    private val TAG = "TransferService"
    private val contentResolver = context.contentResolver
    private val settingsManager = SettingsManager(context)

    suspend fun sendFile(
        device: BeamDevice,
        uri: Uri,
        onProgress: (String, Float) -> Unit
    ): Long = withContext(Dispatchers.IO) {
        val host = device.host
        val port = device.port
        val remotePublicKey = device.id
        var fileName = "unknown"
        var fileSize = 0L
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex)
                fileSize = cursor.getLong(sizeIndex)
            }
        }

        val socket = Socket()
        try {
            onProgress("Connexion...", 0.1f)
            socket.connect(InetSocketAddress(host, port), 5000)
            socket.soTimeout = 30000 
            
            val output = socket.getOutputStream()
            val writer = PrintWriter(output, true)
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

            if (!isActive) return@withContext 0L
            onProgress("Métadonnées...", 0.2f)
            val metadata = JSONObject().apply {
                put("name", fileName)
                put("size", fileSize)
                put("type", mimeType)
                put("sender_id", CryptoManager.getPublicKeyHex())
                put("sender_name", settingsManager.deviceName)
                put("encryption_supported", true)
            }
            writer.println(metadata.toString())

            if (!isActive) return@withContext 0L
            onProgress("Défi...", 0.3f)
            val challenge = reader.readLine() ?: throw Exception("Pas de défi")

            if (!isActive) return@withContext 0L
            onProgress("Signature...", 0.4f)
            val signatureHex = CryptoManager.signChallenge(challenge)
            writer.println(signatureHex)

            if (!isActive) return@withContext 0L
            onProgress("Attente consentement...", -1f)
            val authResponse = reader.readLine()
            if (authResponse != "OK") {
                throw Exception(authResponse ?: "Refusé")
            }

            val uploadStartTime = System.currentTimeMillis()
            
            onProgress("Upload en cours...", 0.5f)
            
            val rawOutput = socket.getOutputStream()
            
            contentResolver.openInputStream(uri)?.use { input ->
                val secretKey = CryptoManager.generateSharedSecret(remotePublicKey)
                
                CryptoManager.getEncryptingStream(rawOutput, secretKey).use { encryptedOutput ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesSent = 0L
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!isActive) break
                        encryptedOutput.write(buffer, 0, bytesRead)
                        totalBytesSent += bytesRead
                        val progress = 0.5f + (totalBytesSent.toFloat() / fileSize.toFloat() * 0.5f)
                        onProgress("Envoi : ${(progress * 100).toInt()}%", progress)
                    }
                }
            }
            
            val duration = System.currentTimeMillis() - uploadStartTime
            if (!isActive) throw Exception("Annulé")
            
            settingsManager.addHistoryItem(HistoryItem(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                deviceName = host,
                timestamp = System.currentTimeMillis(),
                isIncoming = false,
                success = true
            ))

            onProgress("Terminé", 1.0f)
            return@withContext duration
            
        } catch (e: Exception) {
            Log.e(TAG, "Erreur transfert", e)
            if (isActive) {
                onProgress("Erreur : ${e.message}", -1f)
                settingsManager.addHistoryItem(HistoryItem(
                    id = UUID.randomUUID().toString(),
                    fileName = fileName,
                    deviceName = host,
                    timestamp = System.currentTimeMillis(),
                    isIncoming = false,
                    success = false
                ))
            }
            throw e
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }
}
