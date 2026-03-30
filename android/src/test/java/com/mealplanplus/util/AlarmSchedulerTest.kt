package com.mealplanplus.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure JVM tests for [AlarmScheduler]'s time-calculation functions.
 * No Android dependencies — uses [Clock] injection for deterministic results.
 */
class AlarmSchedulerTest {

    companion object {
        private val ZONE = ZoneId.of("UTC")

        /** Builds a fixed [Clock] anchored to the given local date-time in UTC. */
        fun clockAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Clock {
            val instant = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZONE).toInstant()
            return Clock.fixed(instant, ZONE)
        }

        @JvmStatic @BeforeClass fun setUpClass() {}
        @JvmStatic @AfterClass fun tearDownClass() {}
    }

    // ── nextTriggerMillis ─────────────────────────────────────────────────────

    @Test
    fun nextTriggerMillis_futureTimeToday_returnsTodayEpoch() {
        // current = 07:00, target = 08:30 → fires today at 08:30
        val clock = clockAt(2026, 1, 15, 7, 0)
        val result = AlarmScheduler.nextTriggerMillis(8, 30, clock)
        val expected = ZonedDateTime.of(2026, 1, 15, 8, 30, 0, 0, ZONE).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun nextTriggerMillis_pastTimeToday_returnsTomorrowEpoch() {
        // current = 09:00, target = 08:30 → already passed, fires tomorrow at 08:30
        val clock = clockAt(2026, 1, 15, 9, 0)
        val result = AlarmScheduler.nextTriggerMillis(8, 30, clock)
        val expected = ZonedDateTime.of(2026, 1, 16, 8, 30, 0, 0, ZONE).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun nextTriggerMillis_exactlyNow_returnsTomorrowEpoch() {
        // current = 08:30 exactly, target = 08:30 → not strictly after, fires tomorrow
        val clock = clockAt(2026, 1, 15, 8, 30)
        val result = AlarmScheduler.nextTriggerMillis(8, 30, clock)
        val expected = ZonedDateTime.of(2026, 1, 16, 8, 30, 0, 0, ZONE).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    // ── nextMondayTriggerMillis ───────────────────────────────────────────────

    @Test
    fun nextMondayTriggerMillis_todayIsMonday_returnsNextWeekMonday() {
        // 2026-01-19 is a Monday; should return 2026-01-26
        val clock = clockAt(2026, 1, 19, 9, 0)
        val result = AlarmScheduler.nextMondayTriggerMillis(8, 0, clock)
        val expected = ZonedDateTime.of(2026, 1, 26, 8, 0, 0, 0, ZONE).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun nextMondayTriggerMillis_todayIsWednesday_returnsNextMonday() {
        // 2026-01-21 is a Wednesday; next Monday is 2026-01-26
        val clock = clockAt(2026, 1, 21, 9, 0)
        val result = AlarmScheduler.nextMondayTriggerMillis(8, 0, clock)
        val expected = ZonedDateTime.of(2026, 1, 26, 8, 0, 0, 0, ZONE).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun nextMondayTriggerMillis_todayIsSunday_returnsTomorrow() {
        // 2026-01-18 is a Sunday; next Monday is 2026-01-19 (tomorrow)
        val clock = clockAt(2026, 1, 18, 9, 0)
        val result = AlarmScheduler.nextMondayTriggerMillis(8, 0, clock)
        val expected = ZonedDateTime.of(2026, 1, 19, 8, 0, 0, 0, ZONE).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }
}
