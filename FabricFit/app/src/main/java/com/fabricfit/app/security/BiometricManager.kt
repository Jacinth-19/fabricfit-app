package com.fabricfit.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.fabricfit.app.R

/**
 * Biometric authentication manager for app security.
 * Supports fingerprint, face unlock, and other biometric methods.
 */
class BiometricManager(private val context: Context) {
    
    /**
     * Check if biometric authentication is available on this device.
     */
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Check if strong biometric (Class 3) is available.
     */
    fun isStrongBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Show biometric prompt for authentication.
     * 
     * @param activity The FragmentActivity to show the prompt from
     * @param title Title of the biometric prompt
     * @param subtitle Subtitle of the biometric prompt
     * @param description Description of the biometric prompt
     * @param onSuccess Callback when authentication succeeds
     * @param onError Callback when authentication fails with error code and message
     * @param onFailed Callback when authentication fails (wrong fingerprint, etc.)
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = context.getString(R.string.biometric_title),
        subtitle: String = context.getString(R.string.biometric_subtitle),
        description: String = context.getString(R.string.biometric_description),
        onSuccess: () -> Unit,
        onError: (Int, String) -> Unit = { _, _ -> },
        onFailed: () -> Unit = {}
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onError(errorCode, errString.toString())
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onFailed()
            }
        }
        
        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setNegativeButtonText(context.getString(R.string.action_cancel))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    /**
     * Get a user-friendly description of available biometric methods.
     */
    fun getBiometricDescription(): String {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> context.getString(R.string.biometric_available_strong)
            else -> when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
                BiometricManager.BIOMETRIC_SUCCESS -> context.getString(R.string.biometric_available_weak)
                else -> context.getString(R.string.biometric_not_available)
            }
        }
    }
}
