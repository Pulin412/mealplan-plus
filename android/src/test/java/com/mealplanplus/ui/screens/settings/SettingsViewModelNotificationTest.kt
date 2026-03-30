package com.mealplanplus.ui.screens.settings

import android.content.Context
import app.cash.turbine.test
import com.mealplanplus.data.local.CsvDataImporter
import com.mealplanplus.data.local.JsonDataImporter
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.util.NotificationPreferences
import com.mealplanplus.util.ThemePreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.Runs
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Tests for the notification-related state and setters in [SettingsViewModel].
 *
 * [NotificationPreferences] and [ThemePreferences] are mocked so the ViewModel
 * never touches a real DataStore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelNotificationTest {

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

    private lateinit var context: Context
    private lateinit var dailyLogRepo: DailyLogRepository
    private lateinit var healthRepo: HealthRepository
    private lateinit var jsonImporter: JsonDataImporter
    private lateinit var csvImporter: CsvDataImporter
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        dailyLogRepo = mockk(relaxed = true)
        healthRepo = mockk(relaxed = true)
        jsonImporter = mockk(relaxed = true)
        csvImporter = mockk(relaxed = true)

        // Stub ThemePreferences so loadThemePreferences() doesn't hit DataStore
        mockkObject(ThemePreferences)
        every { ThemePreferences.isDarkMode(context) } returns flowOf(false)
        every { ThemePreferences.isDynamicColor(context) } returns flowOf(true)
        every { ThemePreferences.isFollowSystem(context) } returns flowOf(true)

        // Stub NotificationPreferences reads with known defaults
        mockkObject(NotificationPreferences)
        every { NotificationPreferences.getMasterEnabled(context) } returns flowOf(false)
        every { NotificationPreferences.getMealRemindersEnabled(context) } returns flowOf(true)
        every { NotificationPreferences.getStreakProtectionEnabled(context) } returns flowOf(true)
        every { NotificationPreferences.getWeeklyPlanEnabled(context) } returns flowOf(true)
        every { NotificationPreferences.getBreakfastHour(context) } returns flowOf(NotificationPreferences.DEFAULT_BREAKFAST_HOUR)
        every { NotificationPreferences.getLunchHour(context) } returns flowOf(NotificationPreferences.DEFAULT_LUNCH_HOUR)
        every { NotificationPreferences.getDinnerHour(context) } returns flowOf(NotificationPreferences.DEFAULT_DINNER_HOUR)
        every { NotificationPreferences.getStreakAlertHour(context) } returns flowOf(NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR)
        // Stub writes so coVerify can capture them
        coEvery { NotificationPreferences.setMasterEnabled(context, any()) } just Runs
        coEvery { NotificationPreferences.setMealRemindersEnabled(context, any()) } just Runs
        coEvery { NotificationPreferences.setStreakProtectionEnabled(context, any()) } just Runs
        coEvery { NotificationPreferences.setWeeklyPlanEnabled(context, any()) } just Runs
        coEvery { NotificationPreferences.setBreakfastHour(context, any()) } just Runs
        coEvery { NotificationPreferences.setLunchHour(context, any()) } just Runs
        coEvery { NotificationPreferences.setDinnerHour(context, any()) } just Runs
        coEvery { NotificationPreferences.setStreakAlertHour(context, any()) } just Runs

        // Stub repository flows so loadDataForExport() exits cleanly
        every { dailyLogRepo.getLogsByUser() } returns flowOf(emptyList())
        every { healthRepo.getRecentMetrics(any()) } returns flowOf(emptyList())

        viewModel = SettingsViewModel(context, dailyLogRepo, healthRepo, jsonImporter, csvImporter)
    }

    @After
    fun tearDown() {
        unmockkObject(NotificationPreferences)
        unmockkObject(ThemePreferences)
    }

    // ── initial state ──────────────────────────────────────────────────────────

    @Test
    fun notificationState_masterEnabled_defaultIsFalse() = runTest {
        viewModel.notificationState.test {
            assertFalse(awaitItem().masterEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_mealRemindersEnabled_defaultIsTrue() = runTest {
        viewModel.notificationState.test {
            assertTrue(awaitItem().mealRemindersEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_streakProtectionEnabled_defaultIsTrue() = runTest {
        viewModel.notificationState.test {
            assertTrue(awaitItem().streakProtectionEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_weeklyPlanEnabled_defaultIsTrue() = runTest {
        viewModel.notificationState.test {
            assertTrue(awaitItem().weeklyPlanEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_breakfastHour_defaultIs8() = runTest {
        viewModel.notificationState.test {
            assertEquals(NotificationPreferences.DEFAULT_BREAKFAST_HOUR, awaitItem().breakfastHour)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_lunchHour_defaultIs13() = runTest {
        viewModel.notificationState.test {
            assertEquals(NotificationPreferences.DEFAULT_LUNCH_HOUR, awaitItem().lunchHour)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_dinnerHour_defaultIs19() = runTest {
        viewModel.notificationState.test {
            assertEquals(NotificationPreferences.DEFAULT_DINNER_HOUR, awaitItem().dinnerHour)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_streakAlertHour_defaultIs21() = runTest {
        viewModel.notificationState.test {
            assertEquals(NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR, awaitItem().streakAlertHour)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── setters ────────────────────────────────────────────────────────────────

    @Test
    fun setMasterEnabled_callsPreferences() = runTest {
        viewModel.setMasterEnabled(true)
        coVerify { NotificationPreferences.setMasterEnabled(context, true) }
    }

    @Test
    fun setMasterEnabled_false_callsPreferences() = runTest {
        viewModel.setMasterEnabled(false)
        coVerify { NotificationPreferences.setMasterEnabled(context, false) }
    }

    @Test
    fun setMealRemindersEnabled_callsPreferences() = runTest {
        viewModel.setMealRemindersEnabled(false)
        coVerify { NotificationPreferences.setMealRemindersEnabled(context, false) }
    }

    @Test
    fun setStreakProtectionEnabled_callsPreferences() = runTest {
        viewModel.setStreakProtectionEnabled(false)
        coVerify { NotificationPreferences.setStreakProtectionEnabled(context, false) }
    }

    @Test
    fun setWeeklyPlanEnabled_callsPreferences() = runTest {
        viewModel.setWeeklyPlanEnabled(false)
        coVerify { NotificationPreferences.setWeeklyPlanEnabled(context, false) }
    }

    @Test
    fun setBreakfastHour_callsPreferences() = runTest {
        viewModel.setBreakfastHour(7)
        coVerify { NotificationPreferences.setBreakfastHour(context, 7) }
    }

    @Test
    fun setLunchHour_callsPreferences() = runTest {
        viewModel.setLunchHour(12)
        coVerify { NotificationPreferences.setLunchHour(context, 12) }
    }

    @Test
    fun setDinnerHour_callsPreferences() = runTest {
        viewModel.setDinnerHour(20)
        coVerify { NotificationPreferences.setDinnerHour(context, 20) }
    }

    @Test
    fun setStreakAlertHour_callsPreferences() = runTest {
        viewModel.setStreakAlertHour(22)
        coVerify { NotificationPreferences.setStreakAlertHour(context, 22) }
    }
}
