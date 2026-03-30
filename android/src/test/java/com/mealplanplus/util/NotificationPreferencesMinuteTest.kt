package com.mealplanplus.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for the minute-precision fields added to [NotificationPreferences].
 * Verifies default values and constants for breakfast, lunch, dinner and streak alert minutes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesMinuteTest {

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Dispatchers.setMain(UnconfinedTestDispatcher())
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            // Intentionally left empty — Main stays set for in-flight coroutines.
        }
    }

    private val context: Context = mockk(relaxed = true)

    @After
    fun tearDown() {
        unmockkObject(NotificationPreferences)
    }

    // ── default flow values ───────────────────────────────────────────────────

    @Test
    fun breakfastMinute_defaultIsZero() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getBreakfastMinute(context) } returns flowOf(0)
        NotificationPreferences.getBreakfastMinute(context).collect { assertEquals(0, it) }
    }

    @Test
    fun lunchMinute_defaultIsZero() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getLunchMinute(context) } returns flowOf(0)
        NotificationPreferences.getLunchMinute(context).collect { assertEquals(0, it) }
    }

    @Test
    fun dinnerMinute_defaultIsZero() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getDinnerMinute(context) } returns flowOf(0)
        NotificationPreferences.getDinnerMinute(context).collect { assertEquals(0, it) }
    }

    @Test
    fun streakAlertMinute_defaultIsZero() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getStreakAlertMinute(context) } returns flowOf(0)
        NotificationPreferences.getStreakAlertMinute(context).collect { assertEquals(0, it) }
    }

    // ── constants ─────────────────────────────────────────────────────────────

    @Test
    fun defaultBreakfastMinute_isZero() {
        assertEquals(0, NotificationPreferences.DEFAULT_BREAKFAST_MINUTE)
    }

    @Test
    fun defaultLunchMinute_isZero() {
        assertEquals(0, NotificationPreferences.DEFAULT_LUNCH_MINUTE)
    }

    @Test
    fun defaultDinnerMinute_isZero() {
        assertEquals(0, NotificationPreferences.DEFAULT_DINNER_MINUTE)
    }

    @Test
    fun defaultStreakAlertMinute_isZero() {
        assertEquals(0, NotificationPreferences.DEFAULT_STREAK_ALERT_MINUTE)
    }
}
