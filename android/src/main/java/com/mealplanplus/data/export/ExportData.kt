package com.mealplanplus.data.export

/**
 * Flat, Android-free row models for CSV export. The repositories map their domain/UI types down to
 * these so [CsvExporter] stays a pure, unit-testable function with no dependency on Room, Retrofit,
 * or Compose. One [ExportData] is a full snapshot ready to serialise.
 */
data class ExportData(
    val meals: List<MealRow>,
    val diets: List<DietRow>,
    val workoutSets: List<WorkoutSetRow>,
    val health: List<HealthRow>,
)

data class MealRow(
    val name: String,
    val slots: List<String>,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val items: String,
)

data class DietRow(
    val name: String,
    val tags: List<String>,
    val kcal: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val entryCount: Int,
)

/** One row per logged set (a session fans out into many rows). */
data class WorkoutSetRow(
    val date: String,      // ISO-8601 date, e.g. 2026-07-22
    val workout: String,
    val exercise: String,
    val setNumber: Int,
    val reps: Int?,
    val weightKg: Double?,
)

data class HealthRow(
    val type: String,
    val recordedAt: String, // ISO-8601 instant
    val value: Double,
    val secondaryValue: Double?, // diastolic for blood pressure
    val unit: String,
)
