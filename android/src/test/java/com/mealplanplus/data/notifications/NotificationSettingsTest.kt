package com.mealplanplus.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsTest {

    private fun settings(quiet: Boolean, start: Int = 22 * 60, end: Int = 7 * 60) =
        NotificationSettings(
            mealSlots = listOf(MealSlotReminder("Breakfast", enabled = true, minute = 8 * 60)),
            waterEnabled = false,
            workoutEnabled = false, workoutMinute = 18 * 60,
            weighinEnabled = false, weighinDayIso = 7, weighinMinute = 8 * 60,
            quietHours = quiet,
            quietStartMin = start,
            quietEndMin = end,
        )

    @Test
    fun `quiet window wrapping midnight covers late night and early morning`() {
        val s = settings(quiet = true) // 22:00 → 07:00
        assertTrue(s.inQuietHours(23 * 60))     // 11 PM
        assertTrue(s.inQuietHours(2 * 60))      // 2 AM
        assertTrue(s.inQuietHours(22 * 60))     // exactly 10 PM (inclusive start)
        assertFalse(s.inQuietHours(7 * 60))     // exactly 7 AM (exclusive end)
        assertFalse(s.inQuietHours(12 * 60))    // noon
    }

    @Test
    fun `quiet hours disabled is never quiet`() {
        val s = settings(quiet = false)
        assertFalse(s.inQuietHours(2 * 60))
        assertFalse(s.inQuietHours(23 * 60))
    }

    @Test
    fun `non-wrapping window works within a single day`() {
        val s = settings(quiet = true, start = 13 * 60, end = 14 * 60) // 1 PM → 2 PM
        assertTrue(s.inQuietHours(13 * 60 + 30))
        assertFalse(s.inQuietHours(12 * 60))
        assertFalse(s.inQuietHours(14 * 60))
    }

    @Test
    fun `onCount reflects enabled categories`() {
        assertEquals(1, settings(quiet = true).onCount) // a meal slot on; water/workout/weighin off
    }

    @Test
    fun `isEnabled and timeFor read per meal slot and per type`() {
        val s = NotificationSettings(
            mealSlots = listOf(
                MealSlotReminder("Breakfast", enabled = true, minute = 8 * 60),
                MealSlotReminder("Lunch", enabled = false, minute = 13 * 60),
            ),
            waterEnabled = false,
            workoutEnabled = true, workoutMinute = 18 * 60,
            weighinEnabled = true, weighinDayIso = 3, weighinMinute = 9 * 60,
            quietHours = false, quietStartMin = 0, quietEndMin = 0,
        )
        assertTrue(s.isEnabled(NotificationType.MEAL, 0))
        assertFalse(s.isEnabled(NotificationType.MEAL, 1))
        assertEquals(8 * 60 to null, s.timeFor(NotificationType.MEAL, 0))
        assertEquals(18 * 60 to null, s.timeFor(NotificationType.WORKOUT, 0))
        assertEquals(9 * 60 to 3, s.timeFor(NotificationType.WEIGHIN, 0))
    }
}
