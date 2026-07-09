package com.neosharks.locky

import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper over the system BiometricPrompt.
 *
 * Fingerprint only — BIOMETRIC_STRONG and nothing else. There is no PIN /
 * pattern / password fallback. A "Cancel" button is required by the API.
 */
object BiometricHelper {

    fun prompt(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled, hit back, or a hard error — treat as failure.
                    onFailure()
                }
                // onAuthenticationFailed (a single bad attempt) is intentionally ignored;
                // the prompt lets the user retry.
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .setNegativeButtonText(activity.getString(R.string.cancel))
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(info)
    }
}
