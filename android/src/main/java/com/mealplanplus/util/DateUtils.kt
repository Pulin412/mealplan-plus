package com.mealplanplus.util

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Milliseconds per day, used for epoch-day ↔ epoch-ms conversions. */
private const val MS_PER_DAY = 86_400_000L

/**
 * Converts a [LocalDate] to its epoch-millisecond representation at midnight UTC.
 * All date columns in the Room database are stored as epoch milliseconds.
 */
fun LocalDate.toEpochMs(): Long = toEpochDay() * MS_PER_DAY

/**
 * Converts an epoch-millisecond timestamp (stored in Room) back to a [LocalDate].
 * Assumes the value was produced by [toEpochMs] — i.e. midnight UTC.
 */
fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this / MS_PER_DAY)

/**
 * Formats an epoch-millisecond date for chart axis labels (e.g. "08/04").
 */
fun Long.toChartLabel(pattern: String = "dd/MM"): String =
    toLocalDate().format(DateTimeFormatter.ofPattern(pattern))
