package com.octarahq.beamshare

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.io.InputStream
import java.io.OutputStream
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object CryptoManager {
    private const val TAG = "CryptoManager"
    private const val PREFS_NAME = "beamshare_secure_prefs"
    private const val KEY_PRIVATE = "private_key"
    private const val KEY_PUBLIC = "public_key"
    
    private const val KEY_ALGO = "EC" 
    private const val SIG_ALGO = "SHA256withECDSA"
    private const val AGREEMENT_ALGO = "ECDH"
    private const val CURVE = "secp256r1" 

    private var keyPair: KeyPair? = null

    fun init(context: Context) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPrefs = try {
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create EncryptedSharedPreferences, clearing file...", e)
                try {
                    context.deleteSharedPreferences(PREFS_NAME)
                    EncryptedSharedPreferences.create(
                        context,
                        PREFS_NAME,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (e2: Exception) {
                    Log.e(TAG, "Still failed after deletion, using standard SharedPreferences as fallback (WARNING: UNSECURE)", e2)
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                }
            }

            val pubStr = sharedPrefs.getString(KEY_PUBLIC, null)
            val privStr = sharedPrefs.getString(KEY_PRIVATE, null)

            if (pubStr != null && privStr != null) {
                try {
                    val kf = KeyFactory.getInstance(KEY_ALGO)
                    val pubKey = kf.generatePublic(X509EncodedKeySpec(Base64.decode(pubStr, Base64.NO_WRAP)))
                    val privKey = kf.generatePrivate(PKCS8EncodedKeySpec(Base64.decode(privStr, Base64.NO_WRAP)))
                    keyPair = KeyPair(pubKey, privKey)
                    Log.d(TAG, "Clés ECDSA chargées avec succès.")
                } catch (e: Exception) {
                    Log.e(TAG, "Erreur lors du chargement des clés, régénération...", e)
                    generateAndSaveKeys(sharedPrefs)
                }
            } else {
                Log.d(TAG, "Pas de clés trouvées, génération...")
                generateAndSaveKeys(sharedPrefs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur fatale lors de l'initialisation de CryptoManager", e)
        }
    }

    private fun generateAndSaveKeys(prefs: android.content.SharedPreferences) {
        try {
            val kpg = KeyPairGenerator.getInstance(KEY_ALGO)
            kpg.initialize(ECGenParameterSpec(CURVE))
            val kp = kpg.generateKeyPair()
            keyPair = kp
            
            prefs.edit().apply {
                putString(KEY_PUBLIC, Base64.encodeToString(kp.public.encoded, Base64.NO_WRAP))
                putString(KEY_PRIVATE, Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
                apply()
            }
            Log.d(TAG, "Nouvelles clés ECDSA générées et sauvegardées.")
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de générer les clés sur cet appareil", e)
        }
    }

    fun getPublicKeyHex(): String {
        val bytes = keyPair?.public?.encoded ?: return "null_key"
        return bytes.toHex()
    }

    fun getVerificationCode(): String {
        return calculateVerificationCode(keyPair?.public?.encoded ?: return "000000")
    }

    fun calculateVerificationCode(pubKeyBytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(pubKeyBytes)

        val b28 = hash[28].toInt() and 0xFF
        val b29 = hash[29].toInt() and 0xFF
        val b30 = hash[30].toInt() and 0xFF
        val b31 = hash[31].toInt() and 0xFF

        val rawCode = (b28 shl 24) or (b29 shl 16) or (b30 shl 8) or b31
        val verificationCode = (rawCode.toLong() and 0xFFFFFFFFL) % 1000000
        return "%06d".format(verificationCode)
    }

    fun verifySignature(publicKeyHex: String, data: ByteArray, signatureHex: String): Boolean {
        return try {
            val kf = KeyFactory.getInstance(KEY_ALGO)
            val pubKey = kf.generatePublic(X509EncodedKeySpec(publicKeyHex.decodeHex()))
            val sig = Signature.getInstance(SIG_ALGO)
            sig.initVerify(pubKey)
            sig.update(data)
            sig.verify(signatureHex.decodeHex())
        } catch (e: Exception) {
            Log.e(TAG, "Signature verification error", e)
            false
        }
    }

    fun signChallenge(challengeHex: String): String {
        val kp = keyPair ?: throw IllegalStateException("CryptoManager n'a pas pu initialiser les clés")
        val bytesToSign = challengeHex.decodeHex()
        val sig = Signature.getInstance(SIG_ALGO)
        sig.initSign(kp.private)
        sig.update(bytesToSign)
        return sig.sign().toHex()
    }

    fun generateSharedSecret(remotePublicKeyHex: String): SecretKeySpec {
        val kp = keyPair ?: throw IllegalStateException("Clés non initialisées")
        val kf = KeyFactory.getInstance(KEY_ALGO)
        val remotePubKey = kf.generatePublic(X509EncodedKeySpec(remotePublicKeyHex.decodeHex()))
        
        val ka = KeyAgreement.getInstance(AGREEMENT_ALGO)
        ka.init(kp.private)
        ka.doPhase(remotePubKey, true)
        
        val secret = ka.generateSecret()
        val digest = MessageDigest.getInstance("SHA-256")
        val aesKey = digest.digest(secret)
        
        return SecretKeySpec(aesKey, "AES")
    }

    fun getEncryptingStream(outputStream: OutputStream, secretKey: SecretKeySpec): OutputStream {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        
        outputStream.write(iv)
        return CipherOutputStream(outputStream, cipher)
    }

    fun getDecryptingStream(inputStream: InputStream, secretKey: SecretKeySpec): InputStream {
        val iv = ByteArray(12)
        var read = 0
        while (read < 12) {
            val n = inputStream.read(iv, read, 12 - read)
            if (n == -1) throw Exception("Impossible de lire l'IV de chiffrement")
            read += n
        }
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        return CipherInputStream(inputStream, cipher)
    }

    fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}

fun String.decodeHex(): ByteArray {
    return try {
        val s = if (length % 2 != 0) "0$this" else this
        s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    } catch (e: Exception) {
        ByteArray(0)
    }
}
