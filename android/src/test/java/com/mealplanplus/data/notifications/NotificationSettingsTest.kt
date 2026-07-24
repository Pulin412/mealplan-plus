package com.mealplanplus.data.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationSettingsTest {

    private fun settings(quiet: Boolean, start: Int = 22 * 60, end: Int = 7 * 60) =
        NotificationSettings(
            enabled = mapOf(NotificationType.MEAL to true, NotificationType.WATER to false),
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
    fun `onCount reflects enabled entries`() {
        assertEquals(1, settings(quiet = true).onCount)
    }
}
