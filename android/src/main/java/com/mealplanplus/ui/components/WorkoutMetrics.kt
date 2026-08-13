package com.mealplanplus.ui.components

/**
 * Shared logic for how an exercise is logged. An exercise's [ExerciseType] decides which set fields
 * the runner / builder / logs show — reps+weight (STRENGTH), duration+distance (CARDIO), or duration
 * only (TIMED). Kept UI-package-level so the runner and the exercise library render identically.
 */
object ExerciseType {
    const val STRENGTH = "STRENGTH"
    const val CARDIO = "CARDIO"
    const val TIMED = "TIMED"

    /** Order shown in the type picker. */
    val ALL = listOf(STRENGTH, CARDIO, TIMED)

    fun normalize(raw: String?): String = raw?.uppercase()?.takeIf { it in ALL } ?: STRENGTH

    fun label(type: String?): String = when (normalize(type)) {
        CARDIO -> "Cardio"
        TIMED -> "Timed"
        else -> "Strength"
    }

    fun tracksReps(type: String?): Boolean = normalize(type) == STRENGTH
    fun tracksWeight(type: String?): Boolean = normalize(type) == STRENGTH
    fun tracksDuration(type: String?): Boolean = normalize(type).let { it == CARDIO || it == TIMED }
    fun tracksDistance(type: String?): Boolean = normalize(type) == CARDIO
}

// ── Conversions between storage units (seconds / metres) and the friendlier input units the
//    Steppers use (minutes / kilometres). ──────────────────────────────────────────────────────
fun secondsToMinutes(seconds: Int?): Double = (seconds ?: 0) / 60.0
fun minutesToSeconds(minutes: Double): Int? = (minutes * 60).toInt().takeIf { it > 0 }
fun metresToKm(metres: Double?): Double = (metres ?: 0.0) / 1000.0
fun kmToMetres(km: Double): Double? = (km * 1000).takeIf { it > 0 }

// ── Display formatting. ─────────────────────────────────────────────────────────────────────
fun fmtDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return "–"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun fmtDistance(metres: Double?): String {
    if (metres == null || metres <= 0.0) return "–"
    return if (metres >= 1000) {
        val km = metres / 1000.0
        (if (km % 1.0 == 0.0) km.toInt().toString() else "%.2f".format(km)) + " km"
    } else "${metres.toInt()} m"
}

/** One-line recap of a logged/target set, tailored to the exercise [type]. */
fun setSummary(type: String?, reps: Int?, weightKg: Double?, durationSeconds: Int?, distanceMeters: Double?): String =
    when (ExerciseType.normalize(type)) {
        ExerciseType.CARDIO -> listOfNotNull(
            durationSeconds?.takeIf { it > 0 }?.let { fmtDuration(it) },
            distanceMeters?.takeIf { it > 0 }?.let { fmtDistance(it) },
        ).joinToString(" · ").ifBlank { "–" }
        ExerciseType.TIMED -> durationSeconds?.takeIf { it > 0 }?.let { fmtDuration(it) } ?: "–"
        else -> {
            val r = reps?.toString() ?: "–"
            if (weightKg != null && weightKg > 0.0) "$r×${fmtKgShort(weightKg)}" else r
        }
    }

private fun fmtKgShort(v: Double): String = (if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()) + "kg"
