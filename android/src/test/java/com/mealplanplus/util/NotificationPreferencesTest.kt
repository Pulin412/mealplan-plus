package com.mealplanplus.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain

/**
 * Unit tests for [NotificationPreferences].
 * Verifies default values returned when the DataStore has no persisted data.
 * Write operations are verified via the flow-read contract (using mocked DataStore flows).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationPreferencesTest {

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
        unmockkStatic("com.mealplanplus.util.ThemePreferencesKt")
    }

    // ── default values ────────────────────────────────────────────────────────

    @Test
    fun masterEnabled_defaultIsFalse() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getMasterEnabled(context) } returns flowOf(false)

        val result = NotificationPreferences.getMasterEnabled(context)
        result.collect { assertFalse(it) }
    }

    @Test
    fun mealRemindersEnabled_defaultIsTrue() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getMealRemindersEnabled(context) } returns flowOf(true)

        NotificationPreferences.getMealRemindersEnabled(context).collect { assertTrue(it) }
    }

    @Test
    fun streakProtectionEnabled_defaultIsTrue() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getStreakProtectionEnabled(context) } returns flowOf(true)

        NotificationPreferences.getStreakProtectionEnabled(context).collect { assertTrue(it) }
    }

    @Test
    fun weeklyPlanEnabled_defaultIsTrue() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getWeeklyPlanEnabled(context) } returns flowOf(true)

        NotificationPreferences.getWeeklyPlanEnabled(context).collect { assertTrue(it) }
    }

    @Test
    fun breakfastHour_defaultIs8() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getBreakfastHour(context) } returns
            flowOf(NotificationPreferences.DEFAULT_BREAKFAST_HOUR)

        NotificationPreferences.getBreakfastHour(context)
            .collect { assertEquals(NotificationPreferences.DEFAULT_BREAKFAST_HOUR, it) }
    }

    @Test
    fun lunchHour_defaultIs13() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getLunchHour(context) } returns
            flowOf(NotificationPreferences.DEFAULT_LUNCH_HOUR)

        NotificationPreferences.getLunchHour(context)
            .collect { assertEquals(NotificationPreferences.DEFAULT_LUNCH_HOUR, it) }
    }

    @Test
    fun dinnerHour_defaultIs19() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getDinnerHour(context) } returns
            flowOf(NotificationPreferences.DEFAULT_DINNER_HOUR)

        NotificationPreferences.getDinnerHour(context)
            .collect { assertEquals(NotificationPreferences.DEFAULT_DINNER_HOUR, it) }
    }

    @Test
    fun streakAlertHour_defaultIs21() = runTest {
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getStreakAlertHour(context) } returns
            flowOf(NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR)

        NotificationPreferences.getStreakAlertHour(context)
            .collect { assertEquals(NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR, it) }
    }

    // ── constant values ───────────────────────────────────────────────────────

    @Test
    fun defaultBreakfastHour_isEight() {
        assertEquals(8, NotificationPreferences.DEFAULT_BREAKFAST_HOUR)
    }

    @Test
    fun defaultLunchHour_isThirteen() {
        assertEquals(13, NotificationPreferences.DEFAULT_LUNCH_HOUR)
    }

    @Test
    fun defaultDinnerHour_isNineteen() {
        assertEquals(19, NotificationPreferences.DEFAULT_DINNER_HOUR)
    }

    @Test
    fun defaultStreakAlertHour_isTwentyOne() {
        assertEquals(21, NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR)
    }
}
