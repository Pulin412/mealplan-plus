package com.mealplanplus.ui.screens.settings

import android.content.Context
import app.cash.turbine.test
import com.mealplanplus.data.local.CsvDataImporter
import com.mealplanplus.data.local.JsonDataImporter
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.HealthConnectRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.notification.NotificationAlarmBootstrapper
import com.mealplanplus.util.AlarmScheduler
import com.mealplanplus.util.NotificationAlarmType
import com.mealplanplus.util.NotificationPreferences
import com.mealplanplus.util.ThemePreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.Runs
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

/**
 * Tests for minute-precision notification preferences in [SettingsViewModel].
 * Verifies initial state defaults and setter methods (including rescheduling side-effects).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelMinuteTest {

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Dispatchers.setMain(UnconfinedTestDispatcher())
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {}
    }

    private lateinit var context: Context
    private lateinit var dailyLogRepo: DailyLogRepository
    private lateinit var healthRepo: HealthRepository
    private lateinit var healthConnectRepo: HealthConnectRepository
    private lateinit var jsonImporter: JsonDataImporter
    private lateinit var csvImporter: CsvDataImporter
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        dailyLogRepo = mockk(relaxed = true)
        healthRepo = mockk(relaxed = true)
        healthConnectRepo = mockk(relaxed = true)
        jsonImporter = mockk(relaxed = true)
        csvImporter = mockk(relaxed = true)

        mockkObject(ThemePreferences)
        every { ThemePreferences.isDarkMode(context) } returns flowOf(false)
        every { ThemePreferences.isDynamicColor(context) } returns flowOf(true)
        every { ThemePreferences.isFollowSystem(context) } returns flowOf(true)

        mockkObject(NotificationPreferences)
        // hour defaults
        every { NotificationPreferences.getMasterEnabled(context) } returns flowOf(false)
        every { NotificationPreferences.getMealRemindersEnabled(context) } returns flowOf(true)
        every { NotificationPreferences.getStreakProtectionEnabled(context) } returns flowOf(true)
        every { NotificationPreferences.getWeeklyPlanEnabled(context) } returns flowOf(true)
        every { NotificationPreferences.getBreakfastHour(context) } returns flowOf(NotificationPreferences.DEFAULT_BREAKFAST_HOUR)
        every { NotificationPreferences.getLunchHour(context) } returns flowOf(NotificationPreferences.DEFAULT_LUNCH_HOUR)
        every { NotificationPreferences.getDinnerHour(context) } returns flowOf(NotificationPreferences.DEFAULT_DINNER_HOUR)
        every { NotificationPreferences.getStreakAlertHour(context) } returns flowOf(NotificationPreferences.DEFAULT_STREAK_ALERT_HOUR)
        // minute defaults
        every { NotificationPreferences.getBreakfastMinute(context) } returns flowOf(NotificationPreferences.DEFAULT_BREAKFAST_MINUTE)
        every { NotificationPreferences.getLunchMinute(context) } returns flowOf(NotificationPreferences.DEFAULT_LUNCH_MINUTE)
        every { NotificationPreferences.getDinnerMinute(context) } returns flowOf(NotificationPreferences.DEFAULT_DINNER_MINUTE)
        every { NotificationPreferences.getStreakAlertMinute(context) } returns flowOf(NotificationPreferences.DEFAULT_STREAK_ALERT_MINUTE)
        // write stubs
        coEvery { NotificationPreferences.setBreakfastMinute(context, any()) } just Runs
        coEvery { NotificationPreferences.setLunchMinute(context, any()) } just Runs
        coEvery { NotificationPreferences.setDinnerMinute(context, any()) } just Runs
        coEvery { NotificationPreferences.setStreakAlertMinute(context, any()) } just Runs
        coEvery { NotificationPreferences.setMasterEnabled(context, any()) } just Runs
        coEvery { NotificationPreferences.setMealRemindersEnabled(context, any()) } just Runs
        coEvery { NotificationPreferences.setStreakProtectionEnabled(context, any()) } just Runs
        coEvery { NotificationPreferences.setWeeklyPlanEnabled(context, any()) } just Runs
        coEvery { NotificationPreferences.setBreakfastHour(context, any()) } just Runs
        coEvery { NotificationPreferences.setLunchHour(context, any()) } just Runs
        coEvery { NotificationPreferences.setDinnerHour(context, any()) } just Runs
        coEvery { NotificationPreferences.setStreakAlertHour(context, any()) } just Runs

        mockkObject(NotificationAlarmBootstrapper)
        coEvery { NotificationAlarmBootstrapper.scheduleAll(context) } just Runs
        coEvery { NotificationAlarmBootstrapper.rescheduleForType(context, any()) } just Runs

        every { dailyLogRepo.getLogsByUser() } returns flowOf(emptyList())
        every { healthRepo.getRecentMetrics(any()) } returns flowOf(emptyList())

        viewModel = SettingsViewModel(context, dailyLogRepo, healthRepo, healthConnectRepo, jsonImporter, csvImporter)
    }

    @After
    fun tearDown() {
        unmockkObject(NotificationPreferences)
        unmockkObject(ThemePreferences)
        unmockkObject(NotificationAlarmBootstrapper)
    }

    // ── initial state ──────────────────────────────────────────────────────────

    @Test
    fun notificationState_breakfastMinute_defaultIsZero() = runTest {
        viewModel.notificationState.test {
            assertEquals(NotificationPreferences.DEFAULT_BREAKFAST_MINUTE, awaitItem().breakfastMinute)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_lunchMinute_defaultIsZero() = runTest {
        viewModel.notificationState.test {
            assertEquals(NotificationPreferences.DEFAULT_LUNCH_MINUTE, awaitItem().lunchMinute)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_dinnerMinute_defaultIsZero() = runTest {
        viewModel.notificationState.test {
            assertEquals(NotificationPreferences.DEFAULT_DINNER_MINUTE, awaitItem().dinnerMinute)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun notificationState_streakAlertMinute_defaultIsZero() = runTest {
        viewModel.notificationState.test {
            assertEquals(NotificationPreferences.DEFAULT_STREAK_ALERT_MINUTE, awaitItem().streakAlertMinute)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── setters ────────────────────────────────────────────────────────────────

    @Test
    fun setBreakfastMinute_callsPreferencesAndReschedules() = runTest {
        viewModel.setBreakfastMinute(15)
        coVerify { NotificationPreferences.setBreakfastMinute(context, 15) }
        coVerify { NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.BREAKFAST) }
    }

    @Test
    fun setLunchMinute_callsPreferencesAndReschedules() = runTest {
        viewModel.setLunchMinute(30)
        coVerify { NotificationPreferences.setLunchMinute(context, 30) }
        coVerify { NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.LUNCH) }
    }

    @Test
    fun setDinnerMinute_callsPreferencesAndReschedules() = runTest {
        viewModel.setDinnerMinute(45)
        coVerify { NotificationPreferences.setDinnerMinute(context, 45) }
        coVerify { NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.DINNER) }
    }

    @Test
    fun setStreakAlertMinute_callsPreferencesAndReschedules() = runTest {
        viewModel.setStreakAlertMinute(0)
        coVerify { NotificationPreferences.setStreakAlertMinute(context, 0) }
        coVerify { NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.STREAK) }
    }

    @Test
    fun setBreakfastHour_alsoReschedules() = runTest {
        viewModel.setBreakfastHour(7)
        coVerify { NotificationPreferences.setBreakfastHour(context, 7) }
        coVerify { NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.BREAKFAST) }
    }

    @Test
    fun setMasterEnabled_false_callsScheduleAll() = runTest {
        viewModel.setMasterEnabled(false)
        coVerify { NotificationPreferences.setMasterEnabled(context, false) }
        coVerify { NotificationAlarmBootstrapper.scheduleAll(context) }
    }
}
