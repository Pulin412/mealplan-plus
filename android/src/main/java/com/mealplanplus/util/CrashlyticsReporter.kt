package com.mealplanplus.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around FirebaseCrashlytics.
 *
 * All crash/non-fatal reporting flows through this class so that:
 *  - Call sites stay decoupled from the Firebase SDK.
 *  - Unit tests can inject a no-op substitute without needing to mock static singletons.
 *
 * Usage:
 *   reporter.recordNonFatal(e, context = "sync")
 *   reporter.log("diet_assigned", "dietId=$dietId")
 *   reporter.setUserId("42")
 */
@Singleton
class CrashlyticsReporter @Inject constructor() {

    private val crashlytics: FirebaseCrashlytics
        get() = FirebaseCrashlytics.getInstance()

    // ── User identity ─────────────────────────────────────────────────────────

    /**
     * Associate a local user ID with crash reports.
     * Call after successful sign-in.
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    /**
     * Clear the user identity (call on sign-out).
     */
    fun clearUserId() {
        crashlytics.setUserId("")
    }

    // ── Non-fatal errors ──────────────────────────────────────────────────────

    /**
     * Report a caught exception as a non-fatal issue.
     *
     * @param throwable  The exception to record.
     * @param context    Short string identifying where the error occurred (e.g. "sync", "auth").
     * @param extras     Optional key-value pairs added as custom keys to the report.
     */
    fun recordNonFatal(
        throwable: Throwable,
        context: String,
        extras: Map<String, String> = emptyMap()
    ) {
        crashlytics.setCustomKey("error_context", context)
        extras.forEach { (k, v) -> crashlytics.setCustomKey(k, v) }
        crashlytics.recordException(throwable)
    }

    // ── Breadcrumb logging ────────────────────────────────────────────────────

    /**
     * Add a breadcrumb log line visible in Crashlytics console alongside any crash.
     *
     * @param event   Short event name (e.g. "diet_assigned").
     * @param detail  Optional detail string (e.g. "dietId=42").
     */
    fun log(event: String, detail: String = "") {
        val message = if (detail.isEmpty()) event else "$event — $detail"
        crashlytics.log(message)
    }

    // ── Custom keys ───────────────────────────────────────────────────────────

    /** Set an arbitrary string key that appears in every crash report. */
    fun setKey(key: String, value: String) {
        crashlytics.setCustomKey(key, value)
    }

    /** Set an arbitrary boolean key that appears in every crash report. */
    fun setKey(key: String, value: Boolean) {
        crashlytics.setCustomKey(key, value)
    }
}
