package com.keepnc.data.auth

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Encapsulates Biometric & Device Credential authentication checks and prompt execution.
 *
 * Uses AOSP / AndroidX Biometric library (100% open source, F-Droid compliant).
 * Supports fingerprint, face, and device credentials (PIN/pattern/password).
 */
object BiometricAuthHelper {

    /** Authenticators allowed: strong biometrics (fingerprint/face) + device credentials (PIN/pattern/password). */
    const val AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    /**
     * Checks if the device can authenticate using biometrics or screen lock credentials.
     *
     * Returns [BiometricManager.BIOMETRIC_SUCCESS] if enrolled and ready,
     * or an error code like [BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED] if no lock is set.
     */
    fun canAuthenticate(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(AUTHENTICATORS)
    }

    /** Returns true if the device has biometrics or a PIN/pattern/password set up. */
    fun isDeviceSecure(context: Context): Boolean =
        canAuthenticate(context) == BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Displays system authentication prompt.
     *
     * IMPORTANT: When [AUTHENTICATORS] includes [DEVICE_CREDENTIAL], do NOT set
     * negative button text, as the system provides the fallback PIN/pattern button instead.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        onResult: (success: Boolean) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(true)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onResult(false)
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Intermediate failure (e.g. wrong finger), prompt remains visible for retry
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply {
                if (!subtitle.isNullOrBlank()) {
                    setSubtitle(subtitle)
                }
            }
            .setAllowedAuthenticators(AUTHENTICATORS)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
