package com.keepnc.data.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists Nextcloud credentials securely using EncryptedSharedPreferences (AES-256-GCM).
 *
 * BEGINNER NOTE: Never store passwords in plain SharedPreferences — they're readable
 * by anyone with a rooted device or ADB backup. EncryptedSharedPreferences encrypts
 * both the keys and values using the Android Keystore.
 *
 * This class is a @Singleton — one instance shared across the whole app lifetime.
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Lazy initialization so the Keystore isn't accessed until first use.
    private val prefs: SharedPreferences by lazy { createEncryptedPrefs() }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "keepnc_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCredentials(credentials: Credentials) {
        prefs.edit()
            .putString(KEY_SERVER_URL, credentials.serverUrl)
            .putString(KEY_LOGIN_NAME, credentials.loginName)
            .putString(KEY_APP_PASSWORD, credentials.appPassword)
            .apply()
    }

    fun getCredentials(): Credentials? {
        val serverUrl = prefs.getString(KEY_SERVER_URL, null) ?: return null
        val loginName = prefs.getString(KEY_LOGIN_NAME, null) ?: return null
        val appPassword = prefs.getString(KEY_APP_PASSWORD, null) ?: return null
        return Credentials(serverUrl, loginName, appPassword)
    }

    fun getServerUrl(): String? = prefs.getString(KEY_SERVER_URL, null)

    fun isLoggedIn(): Boolean = getCredentials() != null

    /** Wipe all stored credentials (logout). */
    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_SERVER_URL = "server_url"
        private const val KEY_LOGIN_NAME = "login_name"
        private const val KEY_APP_PASSWORD = "app_password"
    }
}
