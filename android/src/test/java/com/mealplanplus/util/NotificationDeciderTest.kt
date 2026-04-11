package com.mealplanplus.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [NotificationDecider].
 * All inputs are plain primitives — no mocking, no Android dependencies.
 */
class NotificationDeciderTest {

    // ── shouldSendMealReminder ────────────────────────────────────────────────

    @Test
    fun mealReminder_correctHourNotLogged_returnsTrue() {
        assertTrue(
            NotificationDecider.shouldSendMealReminder(
                currentHour = 8,
                notificationsEnabled = true,
                mealRemindersEnabled = true,
                targetHour = 8,
                isAlreadyLogged = false
            )
        )
    }

    @Test
    fun mealReminder_alreadyLogged_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendMealReminder(
                currentHour = 8,
                notificationsEnabled = true,
                mealRemindersEnabled = true,
                targetHour = 8,
                isAlreadyLogged = true
            )
        )
    }

    @Test
    fun mealReminder_wrongHour_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendMealReminder(
                currentHour = 10,
                notificationsEnabled = true,
                mealRemindersEnabled = true,
                targetHour = 8,
                isAlreadyLogged = false
            )
        )
    }

    @Test
    fun mealReminder_notificationsDisabled_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendMealReminder(
                currentHour = 8,
                notificationsEnabled = false,
                mealRemindersEnabled = true,
                targetHour = 8,
                isAlreadyLogged = false
            )
        )
    }

    @Test
    fun mealReminder_mealRemindersDisabled_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendMealReminder(
                currentHour = 13,
                notificationsEnabled = true,
                mealRemindersEnabled = false,
                targetHour = 13,
                isAlreadyLogged = false
            )
        )
    }

    @Test
    fun mealReminder_lunchSlot_correctHour_returnsTrue() {
        assertTrue(
            NotificationDecider.shouldSendMealReminder(
                currentHour = 13,
                notificationsEnabled = true,
                mealRemindersEnabled = true,
                targetHour = 13,
                isAlreadyLogged = false
            )
        )
    }

    @Test
    fun mealReminder_dinnerSlot_correctHour_returnsTrue() {
        assertTrue(
            NotificationDecider.shouldSendMealReminder(
                currentHour = 19,
                notificationsEnabled = true,
                mealRemindersEnabled = true,
                targetHour = 19,
                isAlreadyLogged = false
            )
        )
    }

    // ── shouldSendStreakAlert ─────────────────────────────────────────────────

    @Test
    fun streakAlert_activeStreakNothingLoggedAtAlertHour_returnsTrue() {
        assertTrue(
            NotificationDecider.shouldSendStreakAlert(
                currentHour = 21,
                notificationsEnabled = true,
                streakProtectionEnabled = true,
                alertHour = 21,
                streakDays = 5,
                todayCalories = 0.0
            )
        )
    }

    @Test
    fun streakAlert_alreadyLoggedToday_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendStreakAlert(
                currentHour = 21,
                notificationsEnabled = true,
                streakProtectionEnabled = true,
                alertHour = 21,
                streakDays = 5,
                todayCalories = 1200.0
            )
        )
    }

    @Test
    fun streakAlert_noStreak_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendStreakAlert(
                currentHour = 21,
                notificationsEnabled = true,
                streakProtectionEnabled = true,
                alertHour = 21,
                streakDays = 0,
                todayCalories = 0.0
            )
        )
    }

    @Test
    fun streakAlert_tooEarlyInDay_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendStreakAlert(
                currentHour = 10,
                notificationsEnabled = true,
                streakProtectionEnabled = true,
                alertHour = 21,
                streakDays = 3,
                todayCalories = 0.0
            )
        )
    }

    @Test
    fun streakAlert_notificationsDisabled_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendStreakAlert(
                currentHour = 21,
                notificationsEnabled = false,
                streakProtectionEnabled = true,
                alertHour = 21,
                streakDays = 5,
                todayCalories = 0.0
            )
        )
    }

    @Test
    fun streakAlert_streakProtectionDisabled_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendStreakAlert(
                currentHour = 21,
                notificationsEnabled = true,
                streakProtectionEnabled = false,
                alertHour = 21,
                streakDays = 5,
                todayCalories = 0.0
            )
        )
    }

    @Test
    fun streakAlert_afterAlertHour_returnsTrue() {
        // Should still fire if we're past the alert hour (e.g., 22:00 > 21:00)
        assertTrue(
            NotificationDecider.shouldSendStreakAlert(
                currentHour = 22,
                notificationsEnabled = true,
                streakProtectionEnabled = true,
                alertHour = 21,
                streakDays = 7,
                todayCalories = 0.0
            )
        )
    }

    // ── shouldSendWeeklyPlan ──────────────────────────────────────────────────

    @Test
    fun weeklyPlan_mondayMorningNoPlans_returnsTrue() {
        assertTrue(
            NotificationDecider.shouldSendWeeklyPlan(
                dayOfWeek = 1,  // Monday
                currentHour = 8,
                notificationsEnabled = true,
                weeklyPlanEnabled = true,
                plansThisWeekCount = 0
            )
        )
    }

    @Test
    fun weeklyPlan_plansExist_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendWeeklyPlan(
                dayOfWeek = 1,
                currentHour = 8,
                notificationsEnabled = true,
                weeklyPlanEnabled = true,
                plansThisWeekCount = 3
            )
        )
    }

    @Test
    fun weeklyPlan_notMonday_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendWeeklyPlan(
                dayOfWeek = 3,  // Wednesday
                currentHour = 8,
                notificationsEnabled = true,
                weeklyPlanEnabled = true,
                plansThisWeekCount = 0
            )
        )
    }

    @Test
    fun weeklyPlan_mondayTooEarly_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendWeeklyPlan(
                dayOfWeek = 1,
                currentHour = 6,
                notificationsEnabled = true,
                weeklyPlanEnabled = true,
                plansThisWeekCount = 0
            )
        )
    }

    @Test
    fun weeklyPlan_notificationsDisabled_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendWeeklyPlan(
                dayOfWeek = 1,
                currentHour = 8,
                notificationsEnabled = false,
                weeklyPlanEnabled = true,
                plansThisWeekCount = 0
            )
        )
    }

    @Test
    fun weeklyPlan_weeklyPlanDisabled_returnsFalse() {
        assertFalse(
            NotificationDecider.shouldSendWeeklyPlan(
                dayOfWeek = 1,
                currentHour = 8,
                notificationsEnabled = true,
                weeklyPlanEnabled = false,
                plansThisWeekCount = 0
            )
        )
    }

    // ── computeStreak ─────────────────────────────────────────────────────────

    @Test
    fun computeStreak_emptyList_returnsZero() {
        val today = LocalDate.of(2026, 3, 30)
        assertEquals(0, NotificationDecider.computeStreak(emptyList(), today))
    }

    @Test
    fun computeStreak_todayOnly_returnsOne() {
        val today = LocalDate.of(2026, 3, 30)
        assertEquals(1, NotificationDecider.computeStreak(listOf(today), today))
    }

    @Test
    fun computeStreak_threeContinuousDays_returnsThree() {
        val today = LocalDate.of(2026, 3, 30)
        val dates = listOf(
            LocalDate.of(2026, 3, 28),
            LocalDate.of(2026, 3, 29),
            LocalDate.of(2026, 3, 30)
        )
        assertEquals(3, NotificationDecider.computeStreak(dates, today))
    }

    @Test
    fun computeStreak_gapYesterday_returnsOne() {
        val today = LocalDate.of(2026, 3, 30)
        // Today logged, yesterday missing, 2 days ago logged → streak = 1
        val dates = listOf(LocalDate.of(2026, 3, 28), LocalDate.of(2026, 3, 30))
        assertEquals(1, NotificationDecider.computeStreak(dates, today))
    }

    @Test
    fun computeStreak_yesterdayAndTodayLogged_returnsTwo() {
        val today = LocalDate.of(2026, 3, 30)
        val dates = listOf(LocalDate.of(2026, 3, 29), LocalDate.of(2026, 3, 30))
        assertEquals(2, NotificationDecider.computeStreak(dates, today))
    }

    @Test
    fun computeStreak_todayNotLogged_onlyYesterday_returnsZero() {
        // If today is not in the list and yesterday is, streak starts from yesterday
        // but since today has a gap, streak = 0
        val today = LocalDate.of(2026, 3, 30)
        val dates = listOf(LocalDate.of(2026, 3, 29))
        assertEquals(0, NotificationDecider.computeStreak(dates, today))
    }
}
