package com.mealplanplus.util

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Pure stateless logic for deciding whether each type of notification should fire.
 *
 * All inputs are plain primitives — no Android dependencies, fully unit-testable.
 * Workers collect the necessary state and delegate the firing decision here.
 */
object NotificationDecider {

    /**
     * Returns true if a meal-slot reminder should be posted.
     *
     * Fires when:
     * - Both master [notificationsEnabled] and [mealRemindersEnabled] are on
     * - [currentHour] matches [targetHour] (worker runs hourly, so this fires once per hour)
     * - The slot has not already been logged ([isAlreadyLogged] == false)
     */
    fun shouldSendMealReminder(
        currentHour: Int,
        notificationsEnabled: Boolean,
        mealRemindersEnabled: Boolean,
        targetHour: Int,
        isAlreadyLogged: Boolean
    ): Boolean {
        if (!notificationsEnabled || !mealRemindersEnabled) return false
        if (isAlreadyLogged) return false
        return currentHour == targetHour
    }

    /**
     * Returns true if a streak-protection alert should be posted.
     *
     * Fires when:
     * - Both master [notificationsEnabled] and [streakProtectionEnabled] are on
     * - [streakDays] > 0 (there is an active streak worth protecting)
     * - [todayCalories] == 0 (nothing has been logged today yet)
     * - [currentHour] >= [alertHour] (we are at or past the configured alert time)
     */
    fun shouldSendStreakAlert(
        currentHour: Int,
        notificationsEnabled: Boolean,
        streakProtectionEnabled: Boolean,
        alertHour: Int,
        streakDays: Int,
        todayCalories: Double
    ): Boolean {
        if (!notificationsEnabled || !streakProtectionEnabled) return false
        if (streakDays <= 0) return false
        if (todayCalories > 0) return false
        return currentHour >= alertHour
    }

    /**
     * Returns true if the weekly-plan reminder should be posted.
     *
     * Fires when:
     * - Both master [notificationsEnabled] and [weeklyPlanEnabled] are on
     * - [dayOfWeek] == 1 (Monday, using [DayOfWeek.value] convention)
     * - [currentHour] >= 8 (not fired in the early hours)
     * - [plansThisWeekCount] == 0 (no meal plans exist for the current week)
     */
    fun shouldSendWeeklyPlan(
        dayOfWeek: Int,
        currentHour: Int,
        notificationsEnabled: Boolean,
        weeklyPlanEnabled: Boolean,
        plansThisWeekCount: Int
    ): Boolean {
        if (!notificationsEnabled || !weeklyPlanEnabled) return false
        if (dayOfWeek != DayOfWeek.MONDAY.value) return false
        if (currentHour < 8) return false
        return plansThisWeekCount == 0
    }

    /**
     * Computes the current consecutive-day streak from a list of ISO date strings.
     *
     * A streak is the number of consecutive days ending on [today] where each day
     * has at least one logged entry. If today is not in the list the streak is 0.
     *
     * @param completedDateStrings Distinct ISO-format date strings ("yyyy-MM-dd") with any calories > 0.
     * @param today The reference date (usually [LocalDate.now]).
     */
    fun computeStreak(completedDateStrings: List<String>, today: LocalDate): Int {
        if (completedDateStrings.isEmpty()) return 0
        val dates = completedDateStrings
            .mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }
            .toSortedSet()
        if (!dates.contains(today)) return 0
        var streak = 0
        var expected = today
        while (dates.contains(expected)) {
            streak++
            expected = expected.minusDays(1)
        }
        return streak
    }
}
