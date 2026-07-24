package com.mealplanplus.data.export

import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.DietUi
import com.mealplanplus.data.repository.ExerciseRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.MealUi
import com.mealplanplus.data.repository.WorkoutSessionRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gathers a full [ExportData] snapshot from the existing feature repositories, applying the export
 * scope: all meals & diets, completed workout sessions from the **last 7 days**, and health readings
 * from the **last 90 days**. Data spans both Room (meals/diets) and the server-backed REST domains
 * (workouts/health), so this is the one place that fans the sources together for CSV.
 */
@Singleton
class ExportRepository @Inject constructor(
    private val mealRepository: MealRepository,
    private val dietRepository: DietRepository,
    private val sessionRepository: WorkoutSessionRepository,
    private val healthRepository: HealthRepository,
    private val exerciseRepository: ExerciseRepository,
) {
    suspend fun collect(
        today: LocalDate = LocalDate.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): ExportData {
        val meals = mealRepository.getMeals().first().map { it.toRow() }
        val diets = dietRepository.getDiets().first().map { it.toRow() }

        // Workouts — completed sessions within the last 7 days, one row per logged set.
        val exerciseNames = exerciseRepository.list().mapNotNull { e -> e.id?.let { it to e.name } }.toMap()
        val workoutCutoff = today.minusDays(7)
        val workoutSets = sessionRepository.list()
            .filter { it.isCompleted == true && it.date?.let { d -> d >= workoutCutoff } == true }
            .sortedBy { it.date }
            .flatMap { session ->
                val date = session.date.toString()
                // Group by exercise and number 1-based, mirroring the Logs screen (setNumber is 0-based on the wire).
                (session.sets ?: emptyList())
                    .groupBy { it.exerciseId }
                    .flatMap { (exerciseId, sets) ->
                        sets.sortedBy { it.setNumber }.mapIndexed { i, set ->
                            WorkoutSetRow(
                                date = date,
                                workout = session.name,
                                exercise = exerciseNames[exerciseId] ?: "Exercise",
                                setNumber = i + 1,
                                reps = set.reps,
                                weightKg = set.weightKg,
                            )
                        }
                    }
            }

        // Health — all built-in metric types within the last 90 days.
        val healthCutoff = today.minusDays(90).atStartOfDay(zone).toInstant()
        val health = HEALTH_TYPES
            .flatMap { healthRepository.list(it) }
            .filter { !it.recordedAt.isBefore(healthCutoff) }
            .sortedBy { it.recordedAt }
            .map { HealthRow(it.type, it.recordedAt.toString(), it.value, it.secondaryValue, it.unit) }

        return ExportData(meals, diets, workoutSets, health)
    }

    private fun MealUi.toRow() = MealRow(
        name = meal.name,
        slots = meal.slots,
        kcal = totalKcal,
        proteinG = totalProtein,
        carbsG = totalCarbs,
        fatG = totalFat,
        items = itemsSummary,
    )

    private fun DietUi.toRow() = DietRow(
        name = diet.name,
        tags = diet.tags.map { it.name },
        kcal = totalKcal,
        proteinG = totalProtein,
        carbsG = totalCarbs,
        fatG = totalFat,
        entryCount = entryCount,
    )

    private companion object {
        val HEALTH_TYPES = listOf("GLUCOSE", "WEIGHT", "BLOOD_PRESSURE")
    }
}
