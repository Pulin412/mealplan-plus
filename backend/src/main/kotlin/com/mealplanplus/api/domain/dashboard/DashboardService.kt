package com.mealplanplus.api.domain.dashboard

import com.mealplanplus.api.domain.diet.DietMealRepository
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
import com.mealplanplus.api.domain.meal.MealFoodItemRepository
import com.mealplanplus.api.domain.meal.MealRepository
import com.mealplanplus.api.domain.plan.DayPlanRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class DashboardService(
    private val logRepo: DailyLogRepository,
    private val loggedFoodRepo: LoggedFoodRepository,
    private val foodRepo: FoodRepository,
    private val dietRepo: DietRepository,
    private val dietMealRepo: DietMealRepository,
    private val healthRepo: HealthMetricRepository,
    private val dayPlanRepo: DayPlanRepository,
    private val mealRepo: MealRepository,
    private val mealFoodItemRepo: MealFoodItemRepository,
) {
    fun get(firebaseUid: String): DashboardDto {
        val today = LocalDate.now()

        val todayEntity = logRepo.findFirstByFirebaseUidAndDateOrderByIdDesc(firebaseUid, today)
        val recentLogs  = logRepo.findTop5ByFirebaseUidOrderByDateDesc(firebaseUid)

        val sevenDaysAgo = today.minusDays(6)
        val weeklyLogs = logRepo.findByFirebaseUidAndDateBetweenOrderByDateAsc(firebaseUid, sevenDaysAgo, today)

        val allLogIds = (listOfNotNull(todayEntity) + recentLogs + weeklyLogs).map { it.id }.distinct()
        val allLoggedFoods = if (allLogIds.isEmpty()) emptyList()
                             else loggedFoodRepo.findByDailyLogIdIn(allLogIds)
        val foodsByLogId = allLoggedFoods.groupBy { it.dailyLogId }

        val todayLog     = todayEntity?.toDto(foodsByLogId[todayEntity.id] ?: emptyList())
        val recentLogDtos = recentLogs.map { it.toDto(foodsByLogId[it.id] ?: emptyList()) }

        val foodIds = allLoggedFoods.map { it.foodId }.toSet()
        val foodsById = if (foodIds.isEmpty()) emptyMap()
                        else foodRepo.findAllById(foodIds).associateBy { it.id }
        val foodDtos = foodsById.values.map { it.toDto() }

        val currentStreak = computeStreak(firebaseUid, today, foodsByLogId)

        val weeklyCalories = (0..6).map { offset ->
            val date = sevenDaysAgo.plusDays(offset.toLong())
            val log  = weeklyLogs.find { it.date == date }
            val kcal = log?.let { sumCalories(foodsByLogId[it.id] ?: emptyList(), foodsById) } ?: 0.0
            DayCalories(date.format(DateTimeFormatter.ISO_LOCAL_DATE), kcal)
        }

        val todayMacros = if (todayEntity != null) {
            computeMacros(foodsByLogId[todayEntity.id] ?: emptyList(), foodsById)
        } else MacroTotals()

        val dietCount    = dietRepo.countByFirebaseUid(firebaseUid)
        val latestWeight = healthRepo
            .findTop1ByFirebaseUidAndTypeOrderByRecordedAtDesc(firebaseUid, "WEIGHT")
            ?.toDto()

        val todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant()
        val todaySteps = healthRepo
            .findTop1ByFirebaseUidAndTypeAndRecordedAtAfterOrderByRecordedAtDesc(firebaseUid, "STEPS", todayStart)
            ?.value
        val todayCaloriesBurned = healthRepo
            .findTop1ByFirebaseUidAndTypeAndRecordedAtAfterOrderByRecordedAtDesc(firebaseUid, "CALORIES_BURNED", todayStart)
            ?.value

        val todayPlan = buildTodayPlan(firebaseUid, today)

        return DashboardDto(
            todayLog             = todayLog,
            recentLogs           = recentLogDtos,
            foods                = foodDtos,
            dietCount            = dietCount,
            latestWeight         = latestWeight,
            currentStreak        = currentStreak,
            weeklyCalories       = weeklyCalories,
            todayMacros          = todayMacros,
            todayPlan            = todayPlan,
            todaySteps           = todaySteps,
            todayCaloriesBurned  = todayCaloriesBurned,
        )
    }

    private fun buildTodayPlan(firebaseUid: String, today: LocalDate): TodayPlanDto? {
        val plan = dayPlanRepo.findByFirebaseUidAndDate(firebaseUid, today) ?: return null
        val diet = dietRepo.findById(plan.dietId).orElse(null) ?: return null
        val dietMeals = dietMealRepo.findByDietId(diet.id)
        if (dietMeals.isEmpty()) return null

        val mealIds = dietMeals.map { it.mealId }.distinct()
        val mealsById = mealRepo.findAllById(mealIds).associateBy { it.id }
        val itemsByMealId = mealFoodItemRepo.findByMealIdIn(mealIds).groupBy { it.mealId }

        val allFoodIds = itemsByMealId.values.flatten().map { it.foodId }.toSet()
        val planFoodsById = if (allFoodIds.isEmpty()) emptyMap()
                            else foodRepo.findAllById(allFoodIds).associateBy { it.id }

        val slots = dietMeals.mapNotNull { dm ->
            val meal = mealsById[dm.mealId] ?: return@mapNotNull null
            val items = (itemsByMealId[dm.mealId] ?: emptyList()).mapNotNull { item ->
                val food = planFoodsById[item.foodId] ?: return@mapNotNull null
                TodayMealItemDto(
                    foodId        = food.id,
                    foodName      = food.name,
                    quantity      = item.quantity,
                    unit          = item.unit,
                    caloriesPer100 = food.caloriesPer100,
                    proteinPer100  = food.proteinPer100,
                    carbsPer100    = food.carbsPer100,
                    fatPer100      = food.fatPer100,
                    glycemicIndex  = food.glycemicIndex,
                    notes          = item.notes
                )
            }
            TodaySlotDto(slot = dm.slot, mealId = meal.id, mealName = meal.name, items = items)
        }

        val giValues = slots.flatMap { it.items }.mapNotNull { it.glycemicIndex }
        val avgGi = if (giValues.isEmpty()) null else giValues.sum() / giValues.size

        return TodayPlanDto(
            dietId          = diet.id,
            dietName        = diet.name,
            description     = diet.description,
            targetCalories  = diet.targetCalories,
            targetProtein   = diet.targetProtein,
            targetCarbs     = diet.targetCarbs,
            targetFat       = diet.targetFat,
            avgGlycemicIndex = avgGi,
            slots           = slots
        )
    }

    private fun computeStreak(
        firebaseUid: String,
        today: LocalDate,
        foodsByLogId: Map<Long, List<LoggedFood>>
    ): Int {
        val since = today.minusDays(59)
        val logs  = logRepo.findByFirebaseUidAndDateBetweenOrderByDateAsc(firebaseUid, since, today)
        val datesWithFood = logs
            .filter { (foodsByLogId[it.id]?.isNotEmpty() == true) }
            .map { it.date }
            .toSet()

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
