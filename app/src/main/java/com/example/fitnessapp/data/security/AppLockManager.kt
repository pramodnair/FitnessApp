package com.example.fitnessapp.data.security

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppLockManager {

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private var lastBackgroundTimestampMs: Long = 0L
    private const val LOCK_TIMEOUT_MS = 2000L // 2 seconds grace period to prevent re-locking during system popups

    fun setUnlocked(unlocked: Boolean) {
        _isUnlocked.value = unlocked
    }

    fun lock() {
        _isUnlocked.value = false
    }

    fun onActivityStopped() {
        lastBackgroundTimestampMs = System.currentTimeMillis()
    }

    fun onActivityResumed(isLockEnabled: Boolean) {
        if (!isLockEnabled) {
            _isUnlocked.value = true
            return
        }
        val elapsed = System.currentTimeMillis() - lastBackgroundTimestampMs
        if (lastBackgroundTimestampMs > 0L && elapsed > LOCK_TIMEOUT_MS) {
            _isUnlocked.value = false
        }
    }

    /**
     * Checks whether the device has biometrics or system lock credentials (PIN/Pattern/Password) configured.
     */
    fun canAuthenticate(context: Context): Boolean {
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguardManager?.isDeviceSecure == true) {
            return true
        }
        val biometricManager = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Launches the system default authentication prompt.
     * Uses BIOMETRIC_STRONG or DEVICE_CREDENTIAL so Fingerprint, Face, PIN, Pattern, or Password
     * are automatically supported based on device defaults.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Unlock NutriFit AI",
        subtitle: String = "Confirm your identity to access fitness logs",
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    _isUnlocked.value = true
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed. Please try again.")
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError("Biometric authentication error: ${e.message}")
        }
    }
}
