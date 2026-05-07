package com.mealplanplus.api.domain.dashboard

import com.mealplanplus.api.domain.diet.DietRepository
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.food.toDto
import com.mealplanplus.api.domain.health.HealthMetricRepository
import com.mealplanplus.api.domain.health.toDto
import com.mealplanplus.api.domain.log.DailyLog
import com.mealplanplus.api.domain.log.DailyLogRepository
import com.mealplanplus.api.domain.log.LoggedFood
import com.mealplanplus.api.domain.log.LoggedFoodRepository
import com.mealplanplus.api.domain.log.toDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class DashboardService(
    private val logRepo: DailyLogRepository,
    private val loggedFoodRepo: LoggedFoodRepository,
    private val foodRepo: FoodRepository,
    private val dietRepo: DietRepository,
    private val healthRepo: HealthMetricRepository,
) {
    fun get(firebaseUid: String): DashboardDto {
        val today = LocalDate.now()

        val todayEntity = logRepo.findFirstByFirebaseUidAndDateOrderByIdDesc(firebaseUid, today)
        val recentLogs  = logRepo.findTop5ByFirebaseUidOrderByDateDesc(firebaseUid)

        // Batch-fetch 7-day logs for streak + weekly calories in one shot
        val sevenDaysAgo = today.minusDays(6)
        val weeklyLogs = logRepo.findByFirebaseUidAndDateBetweenOrderByDateAsc(firebaseUid, sevenDaysAgo, today)

        // All log IDs we need foods for: today + recent 5 + weekly 7 days (distinct)
        val allLogIds = (listOfNotNull(todayEntity) + recentLogs + weeklyLogs).map { it.id }.distinct()
        val allLoggedFoods = if (allLogIds.isEmpty()) emptyList()
                             else loggedFoodRepo.findByDailyLogIdIn(allLogIds)
        val foodsByLogId = allLoggedFoods.groupBy { it.dailyLogId }

        val todayLog     = todayEntity?.toDto(foodsByLogId[todayEntity.id] ?: emptyList())
        val recentLogDtos = recentLogs.map { it.toDto(foodsByLogId[it.id] ?: emptyList()) }

        // Batch-fetch only referenced food rows
        val foodIds = allLoggedFoods.map { it.foodId }.toSet()
        val foodsById = if (foodIds.isEmpty()) emptyMap()
                        else foodRepo.findAllById(foodIds).associateBy { it.id }
        val foodDtos = foodsById.values.map { it.toDto() }

        // ── Streak ────────────────────────────────────────────────────────────
        val currentStreak = computeStreak(firebaseUid, today, foodsByLogId)

        // ── Weekly calories ───────────────────────────────────────────────────
        val weeklyCalories = (0..6).map { offset ->
            val date = sevenDaysAgo.plusDays(offset.toLong())
            val log  = weeklyLogs.find { it.date == date }
            val kcal = log?.let { sumCalories(foodsByLogId[it.id] ?: emptyList(), foodsById) } ?: 0.0
            DayCalories(date.format(DateTimeFormatter.ISO_LOCAL_DATE), kcal)
        }

        // ── Today's macros ────────────────────────────────────────────────────
        val todayMacros = if (todayEntity != null) {
            computeMacros(foodsByLogId[todayEntity.id] ?: emptyList(), foodsById)
        } else MacroTotals()

        val dietCount    = dietRepo.countByFirebaseUid(firebaseUid)
        val latestWeight = healthRepo
            .findTop1ByFirebaseUidAndTypeOrderByRecordedAtDesc(firebaseUid, "WEIGHT")
            ?.toDto()

        return DashboardDto(
            todayLog       = todayLog,
            recentLogs     = recentLogDtos,
            foods          = foodDtos,
            dietCount      = dietCount,
            latestWeight   = latestWeight,
            currentStreak  = currentStreak,
            weeklyCalories = weeklyCalories,
            todayMacros    = todayMacros,
        )
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun computeStreak(
        firebaseUid: String,
        today: LocalDate,
        foodsByLogId: Map<Long, List<LoggedFood>>
    ): Int {
        // Fetch recent 60 days of logs to cover any realistic streak
        val since = today.minusDays(59)
        val logs  = logRepo.findByFirebaseUidAndDateBetweenOrderByDateAsc(firebaseUid, since, today)
        val datesWithFood = logs
            .filter { (foodsByLogId[it.id]?.isNotEmpty() == true) }
            .map { it.date }
            .toSet()

        // Walk backwards from today; allow today to be empty (streak counts yesterday as anchor)
        var streak = 0
        var cursor = if (today in datesWithFood) today else today.minusDays(1)
        while (cursor in datesWithFood) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    private fun sumCalories(
        loggedFoods: List<LoggedFood>,
        foodsById: Map<Long, com.mealplanplus.api.domain.food.Food>
    ): Double = loggedFoods.sumOf { lf ->
        val food = foodsById[lf.foodId] ?: return@sumOf 0.0
        val grams = if (lf.unit == "GRAM") lf.quantity else lf.quantity * 100.0
        food.caloriesPer100 * grams / 100.0
    }

    private fun computeMacros(
        loggedFoods: List<LoggedFood>,
        foodsById: Map<Long, com.mealplanplus.api.domain.food.Food>
    ): MacroTotals {
        var cal = 0.0; var prot = 0.0; var carb = 0.0; var fat = 0.0
        for (lf in loggedFoods) {
            val food = foodsById[lf.foodId] ?: continue
            val grams = if (lf.unit == "GRAM") lf.quantity else lf.quantity * 100.0
            val factor = grams / 100.0
            cal  += food.caloriesPer100 * factor
            prot += food.proteinPer100  * factor
            carb += food.carbsPer100    * factor
            fat  += food.fatPer100      * factor
        }
        return MacroTotals(
            protein  = Math.round(prot * 10) / 10.0,
            carbs    = Math.round(carb * 10) / 10.0,
            fat      = Math.round(fat  * 10) / 10.0,
            calories = Math.round(cal  * 10) / 10.0
        )
    }
}
