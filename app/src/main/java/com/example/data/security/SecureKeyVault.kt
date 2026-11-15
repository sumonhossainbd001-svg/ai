package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.domain.model.VideoProviderType
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts user-provided third-party API keys using Android Keystore AES-256-GCM
 * before persisting locally in MODE_PRIVATE SharedPreferences.
 * Keys are never hardcoded, never stored in plaintext, and never shipped with fake defaults.
 */
class SecureKeyVault(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun saveApiKey(provider: VideoProviderType, rawKey: String) {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty()) {
            removeApiKey(provider)
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        val encoded = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(encrypted, Base64.NO_WRAP)
        prefs.edit().putString(keyPrefName(provider), encoded).apply()
    }

    fun getApiKey(provider: VideoProviderType): String? {
        val stored = prefs.getString(keyPrefName(provider), null) ?: return null
        val parts = stored.split(":", limit = 2)
        if (parts.size != 2) return null
        return try {
            val iv = Base64.decode(parts[0], Base64.NO_WRAP)
            val encrypted = Base64.decode(parts[1], Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun hasApiKey(provider: VideoProviderType): Boolean = !getApiKey(provider).isNullOrBlank()

    fun getMaskedPreview(provider: VideoProviderType): String {
        val key = getApiKey(provider) ?: return ""
        return if (key.length <= 8) "••••" else "${key.take(4)}••••${key.takeLast(4)}"
    }

    fun removeApiKey(provider: VideoProviderType) {
        prefs.edit().remove(keyPrefName(provider)).apply()
    }

    fun saveProviderEndpoint(provider: VideoProviderType, baseUrl: String, model: String) {
        prefs.edit()
            .putString("url_${provider.id}", baseUrl.trim())
            .putString("model_${provider.id}", model.trim())
            .apply()
    }

    fun getProviderBaseUrl(provider: VideoProviderType): String =
        prefs.getString("url_${provider.id}", null)?.takeIf { it.isNotBlank() }
            ?: provider.defaultBaseUrl

    fun getProviderModel(provider: VideoProviderType): String =
        prefs.getString("model_${provider.id}", null)?.takeIf { it.isNotBlank() }
            ?: provider.defaultModel

    private fun keyPrefName(provider: VideoProviderType) = "enc_key_${provider.id}"

    private companion object {
        const val PREFS_NAME = "studio_secure_key_vault"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "ai_video_studio_api_aes_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
