package com.mealplanplus.notification

import android.content.Context
import com.mealplanplus.util.AlarmScheduler
import com.mealplanplus.util.NotificationAlarmType
import com.mealplanplus.util.NotificationPreferences
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
import org.junit.BeforeClass
import org.junit.Test

/**
 * Unit tests for [NotificationAlarmBootstrapper].
 * Both [AlarmScheduler] and [NotificationPreferences] are mocked as objects.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotificationAlarmBootstrapperTest {

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

    private val context: Context = mockk(relaxed = true)

    private fun stubPrefs(
        master: Boolean = true,
        mealReminders: Boolean = true,
        streak: Boolean = true,
        weekly: Boolean = true,
        breakfastH: Int = 8, breakfastM: Int = 0,
        lunchH: Int = 13, lunchM: Int = 0,
        dinnerH: Int = 19, dinnerM: Int = 0,
        streakH: Int = 21, streakM: Int = 0
    ) {
        every { NotificationPreferences.getMasterEnabled(context) } returns flowOf(master)
        every { NotificationPreferences.getMealRemindersEnabled(context) } returns flowOf(mealReminders)
        every { NotificationPreferences.getStreakProtectionEnabled(context) } returns flowOf(streak)
        every { NotificationPreferences.getWeeklyPlanEnabled(context) } returns flowOf(weekly)
        every { NotificationPreferences.getBreakfastHour(context) } returns flowOf(breakfastH)
        every { NotificationPreferences.getBreakfastMinute(context) } returns flowOf(breakfastM)
        every { NotificationPreferences.getLunchHour(context) } returns flowOf(lunchH)
        every { NotificationPreferences.getLunchMinute(context) } returns flowOf(lunchM)
        every { NotificationPreferences.getDinnerHour(context) } returns flowOf(dinnerH)
        every { NotificationPreferences.getDinnerMinute(context) } returns flowOf(dinnerM)
        every { NotificationPreferences.getStreakAlertHour(context) } returns flowOf(streakH)
        every { NotificationPreferences.getStreakAlertMinute(context) } returns flowOf(streakM)
    }

    @After
    fun tearDown() {
        unmockkObject(NotificationPreferences)
        unmockkObject(AlarmScheduler)
    }

    @Test
    fun scheduleAll_masterDisabled_cancelsAllAlarms() = runTest {
        mockkObject(NotificationPreferences)
        mockkObject(AlarmScheduler)
        stubPrefs(master = false)
        every { AlarmScheduler.cancelAllNotificationAlarms(context) } just Runs

        NotificationAlarmBootstrapper.scheduleAll(context)

        coVerify { AlarmScheduler.cancelAllNotificationAlarms(context) }
    }

    @Test
    fun scheduleAll_allEnabled_schedulesAllFiveAlarms() = runTest {
        mockkObject(NotificationPreferences)
        mockkObject(AlarmScheduler)
        stubPrefs()
        every { AlarmScheduler.scheduleMealAlarm(context, any(), any(), any()) } just Runs
        every { AlarmScheduler.scheduleWeeklyPlanAlarm(context) } just Runs

        NotificationAlarmBootstrapper.scheduleAll(context)

        coVerify { AlarmScheduler.scheduleMealAlarm(context, NotificationAlarmType.BREAKFAST, 8, 0) }
        coVerify { AlarmScheduler.scheduleMealAlarm(context, NotificationAlarmType.LUNCH, 13, 0) }
        coVerify { AlarmScheduler.scheduleMealAlarm(context, NotificationAlarmType.DINNER, 19, 0) }
        coVerify { AlarmScheduler.scheduleMealAlarm(context, NotificationAlarmType.STREAK, 21, 0) }
        coVerify { AlarmScheduler.scheduleWeeklyPlanAlarm(context) }
    }

    @Test
    fun scheduleAll_mealRemindersDisabled_cancelsMealAlarms() = runTest {
        mockkObject(NotificationPreferences)
        mockkObject(AlarmScheduler)
        stubPrefs(mealReminders = false)
        every { AlarmScheduler.cancelAlarm(context, any()) } just Runs
        every { AlarmScheduler.scheduleMealAlarm(context, any(), any(), any()) } just Runs
        every { AlarmScheduler.scheduleWeeklyPlanAlarm(context) } just Runs

        NotificationAlarmBootstrapper.scheduleAll(context)

        coVerify { AlarmScheduler.cancelAlarm(context, NotificationAlarmType.BREAKFAST) }
        coVerify { AlarmScheduler.cancelAlarm(context, NotificationAlarmType.LUNCH) }
        coVerify { AlarmScheduler.cancelAlarm(context, NotificationAlarmType.DINNER) }
    }

    @Test
    fun rescheduleForType_cancelsOldThenSchedulesNew() = runTest {
        mockkObject(NotificationPreferences)
        mockkObject(AlarmScheduler)
        stubPrefs(master = true, mealReminders = true, dinnerH = 20, dinnerM = 30)
        every { AlarmScheduler.cancelAlarm(context, NotificationAlarmType.DINNER) } just Runs
        every { AlarmScheduler.scheduleMealAlarm(context, NotificationAlarmType.DINNER, 20, 30) } just Runs

        NotificationAlarmBootstrapper.rescheduleForType(context, NotificationAlarmType.DINNER)

        coVerify { AlarmScheduler.cancelAlarm(context, NotificationAlarmType.DINNER) }
        coVerify { AlarmScheduler.scheduleMealAlarm(context, NotificationAlarmType.DINNER, 20, 30) }
    }
}
