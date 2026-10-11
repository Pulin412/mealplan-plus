package com.mealplanplus.ui.screens.journal

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Journal note lock, backed by the device's own biometric/credential (no app PIN). Opt-in: a
 * SharedPreferences flag remembers whether the lock is on, so notes start hidden each visit.
 */
private const val PREFS = "journal_lock"
private const val KEY_ENABLED = "enabled"

fun isJournalLockEnabled(ctx: Context): Boolean =
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)

fun setJournalLockEnabled(ctx: Context, enabled: Boolean) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
}

// Device-credential can only be combined with biometrics from API 30+; below that fall back to a
// biometric-only prompt with a Cancel button.
private fun authenticators(): Int =
    if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_WEAK or DEVICE_CREDENTIAL else BIOMETRIC_WEAK

/** Prompt the device biometric/credential; [onSuccess] reveals the notes, [onError] reports why not. */
fun promptJournalUnlock(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    val auth = authenticators()
    if (BiometricManager.from(activity).canAuthenticate(auth) != BiometricManager.BIOMETRIC_SUCCESS) {
        onError("Set up a fingerprint, face, or screen lock on this device to use the journal lock.")
        return
    }
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) onError(errString.toString())
            }
        },
    )
    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock your journal")
        .setSubtitle("Verify it's you to view your notes")
        .setAllowedAuthenticators(auth)
    if (Build.VERSION.SDK_INT < 30) builder.setNegativeButtonText("Cancel") // required when not using DEVICE_CREDENTIAL
    prompt.authenticate(builder.build())
}
