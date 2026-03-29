package com.mealplanplus.util

import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class CrashlyticsReporterTest {

    private lateinit var firebaseCrashlytics: FirebaseCrashlytics
    private lateinit var reporter: CrashlyticsReporter

    @Before
    fun setUp() {
        firebaseCrashlytics = mockk(relaxed = true)
        mockkStatic(FirebaseCrashlytics::class)
        every { FirebaseCrashlytics.getInstance() } returns firebaseCrashlytics
        reporter = CrashlyticsReporter()
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseCrashlytics::class)
    }

    // ── setUserId ─────────────────────────────────────────────────────────────

    @Test
    fun setUserId_delegatesToFirebase() {
        reporter.setUserId("42")
        verify(exactly = 1) { firebaseCrashlytics.setUserId("42") }
    }

    // ── clearUserId ───────────────────────────────────────────────────────────

    @Test
    fun clearUserId_setsEmptyString() {
        reporter.clearUserId()
        verify(exactly = 1) { firebaseCrashlytics.setUserId("") }
    }

    // ── recordNonFatal ────────────────────────────────────────────────────────

    @Test
    fun recordNonFatal_setsContextKeyAndRecordsException() {
        val exception = RuntimeException("boom")
        reporter.recordNonFatal(exception, context = "sync_push")

        verify(exactly = 1) { firebaseCrashlytics.setCustomKey("error_context", "sync_push") }
        verify(exactly = 1) { firebaseCrashlytics.recordException(exception) }
    }

    @Test
    fun recordNonFatal_withExtras_setsAllKeys() {
        val exception = IllegalStateException("bad state")
        reporter.recordNonFatal(
            exception,
            context = "usda_search",
            extras = mapOf("query" to "chicken", "page" to "1")
        )

        verify(exactly = 1) { firebaseCrashlytics.setCustomKey("error_context", "usda_search") }
        verify(exactly = 1) { firebaseCrashlytics.setCustomKey("query", "chicken") }
        verify(exactly = 1) { firebaseCrashlytics.setCustomKey("page", "1") }
        verify(exactly = 1) { firebaseCrashlytics.recordException(exception) }
    }

    @Test
    fun recordNonFatal_noExtras_doesNotSetExtraKeys() {
        val exception = RuntimeException("error")
        reporter.recordNonFatal(exception, context = "auth")

        // Only error_context key should be set, no others
        verify(exactly = 1) { firebaseCrashlytics.setCustomKey(any<String>(), any<String>()) }
        verify(exactly = 1) { firebaseCrashlytics.recordException(exception) }
    }

    // ── log ───────────────────────────────────────────────────────────────────

    @Test
    fun log_withDetail_formatsMessageWithDash() {
        reporter.log("sync_push", "accepted=3")
        verify(exactly = 1) { firebaseCrashlytics.log("sync_push — accepted=3") }
    }

    @Test
    fun log_withEmptyDetail_logsEventNameOnly() {
        reporter.log("sign_out")
        verify(exactly = 1) { firebaseCrashlytics.log("sign_out") }
    }

    @Test
    fun log_withBlankDetail_logsEventNameOnly() {
        reporter.log("app_start", "")
        verify(exactly = 1) { firebaseCrashlytics.log("app_start") }
    }

    // ── setKey (String) ───────────────────────────────────────────────────────

    @Test
    fun setKey_string_delegatesToFirebase() {
        reporter.setKey("screen", "home")
        verify(exactly = 1) { firebaseCrashlytics.setCustomKey("screen", "home") }
    }

    // ── setKey (Boolean) ──────────────────────────────────────────────────────

    @Test
    fun setKey_boolean_delegatesToFirebase() {
        reporter.setKey("is_premium", true)
        verify(exactly = 1) { firebaseCrashlytics.setCustomKey("is_premium", true) }
    }
}
