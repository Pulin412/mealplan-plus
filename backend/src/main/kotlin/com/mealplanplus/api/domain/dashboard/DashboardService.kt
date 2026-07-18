package com.mealplanplus.api.domain.dashboard

import com.mealplanplus.api.generated.model.CalorieRingDto
import com.mealplanplus.api.generated.model.DashboardDto
import com.mealplanplus.api.generated.model.LoggedFoodResponseDto
import com.mealplanplus.api.generated.model.MacroPanelDto
import com.mealplanplus.api.generated.model.SlotStatusDto
import com.mealplanplus.api.generated.model.StreakDto
import com.mealplanplus.api.generated.model.TodayMealItemDto
import com.mealplanplus.api.domain.diet.DietMealRepository
import com.mealplanplus.api.domain.diet.DietRepository
import com.mealplanplus.api.domain.food.Food
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.health.HealthMetricRepository
import com.mealplanplus.api.domain.health.toDto
import com.mealplanplus.api.domain.log.DailyLogRepository
import com.mealplanplus.api.domain.log.LoggedFood
import com.mealplanplus.api.domain.log.LoggedFoodRepository
import com.mealplanplus.api.domain.log.LoggedMealSlotRepository
import com.mealplanplus.api.domain.meal.MealFoodItemRepository
import com.mealplanplus.api.domain.meal.MealRepository
import com.mealplanplus.api.domain.plan.DayPlanRepository
import com.mealplanplus.api.domain.user.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
class DashboardService(
    private val logRepo: DailyLogRepository,
    private val loggedFoodRepo: LoggedFoodRepository,
    private val slotRepo: LoggedMealSlotRepository,
    private val foodRepo: FoodRepository,
    private val dietRepo: DietRepository,
    private val dietMealRepo: DietMealRepository,
    private val healthRepo: HealthMetricRepository,
    private val dayPlanRepo: DayPlanRepository,
    private val mealRepo: MealRepository,
    private val mealFoodItemRepo: MealFoodItemRepository,
    private val userRepo: UserRepository,
) {
    fun get(firebaseUid: String, clientDate: LocalDate? = null): DashboardDto {
        val today = clientDate ?: LocalDate.now()
        val user  = userRepo.findByFirebaseUid(firebaseUid)

        // ── Plan for today ────────────────────────────────────────────────────
        val plan = dayPlanRepo.findByFirebaseUidAndDate(firebaseUid, today)
        val diet = plan?.dietId?.let { dietRepo.findById(it).orElse(null) }

        // ── Build slot list ───────────────────────────────────────────────────
        val loggedSlots = slotRepo.findByFirebaseUidAndDate(firebaseUid, today)
            .associateBy { it.slot }

        val slots = buildSlots(diet?.id, loggedSlots)

        // ── Additional individually logged foods ──────────────────────────────
        val todayLog = logRepo.findFirstByFirebaseUidAndDateOrderByIdDesc(firebaseUid, today)
        val additionalFoods: List<LoggedFoodResponseDto> = if (todayLog != null) {
            loggedFoodRepo.findByDailyLogId(todayLog.id).map { lf ->
                LoggedFoodResponseDto(id = lf.id, dailyLogId = lf.dailyLogId,
                    date = today.toString(), foodId = lf.foodId,
                    mealSlot = lf.mealSlot, quantity = lf.quantity, unit = lf.unit)
            }
        } else emptyList()

        // ── Calorie + macro totals ────────────────────────────────────────────
        val loggedFoods = if (todayLog != null) loggedFoodRepo.findByDailyLogId(todayLog.id) else emptyList()
        val foodIds = loggedFoods.map { it.foodId }.toSet()
        val foodsById = if (foodIds.isEmpty()) emptyMap()
                        else foodRepo.findAllById(foodIds).associateBy { it.id }

        val slotConsumed = slots.filter { it.isLogged }.fold(NutritionTotals()) { acc, s ->
            acc.copy(kcal = acc.kcal + s.kcal, protein = acc.protein + s.protein,
                     carbs = acc.carbs + s.carbs, fat = acc.fat + s.fat)
        }
        val extraConsumed  = computeNutrition(loggedFoods, foodsById)
        val totalConsumed  = slotConsumed + extraConsumed

        val target     = diet?.targetCalories?.toInt() ?: user?.targetCalories ?: 2000
        val remaining  = (target - totalConsumed.kcal).coerceAtLeast(-9999.0)

        val calorieRing = CalorieRingDto(
            target    = target,
            consumed  = round1(totalConsumed.kcal),
            remaining = round1(remaining),
            isOver    = totalConsumed.kcal > target
        )
        val macros = MacroPanelDto(
            targetProtein    = user?.targetProtein,
            targetCarbs      = user?.targetCarbs,
            targetFat        = user?.targetFat,
            consumedProtein  = round1(totalConsumed.protein),
            consumedCarbs    = round1(totalConsumed.carbs),
            consumedFat      = round1(totalConsumed.fat)
        )

        // ── Streak ────────────────────────────────────────────────────────────
        val streak = computeStreak(firebaseUid, today)

        // ── Health Connect ────────────────────────────────────────────────────
        val todayStart = today.atStartOfDay(ZoneOffset.UTC).toInstant()
        val todaySteps = healthRepo
            .findTop1ByFirebaseUidAndTypeAndRecordedAtAfterOrderByRecordedAtDesc(firebaseUid, "STEPS", todayStart)
            ?.value
        val todayCaloriesBurned = healthRepo
            .findTop1ByFirebaseUidAndTypeAndRecordedAtAfterOrderByRecordedAtDesc(firebaseUid, "CALORIES_BURNED", todayStart)
            ?.value
        val latestWeight = healthRepo
            .findTop1ByFirebaseUidAndTypeOrderByRecordedAtDesc(firebaseUid, "WEIGHT")?.toDto()

        return DashboardDto(
            date                = today.format(DateTimeFormatter.ISO_LOCAL_DATE),
            dietId              = diet?.id,
            dietName            = diet?.name,
            calorieRing         = calorieRing,
            macros              = macros,
            slots               = slots,
            additionalFoods     = additionalFoods,
            streak              = streak,
            todaySteps          = todaySteps,
            todayCaloriesBurned = todayCaloriesBurned,
            latestWeight        = latestWeight,
            dietCount           = dietRepo.countByFirebaseUid(firebaseUid)
        )
    }

    private fun buildSlots(
        dietId: Long?,
        loggedSlots: Map<String, com.mealplanplus.api.domain.log.LoggedMealSlot>
    ): List<SlotStatusDto> {
        if (dietId == null) return emptyList()
        val dietMeals   = dietMealRepo.findByDietId(dietId)
        if (dietMeals.isEmpty()) return emptyList()
        val mealIds     = dietMeals.map { it.mealId }.distinct()
        val mealsById   = mealRepo.findAllById(mealIds).associateBy { it.id }
        val itemsByMealId = mealFoodItemRepo.findByMealIdIn(mealIds).groupBy { it.mealId }
        val foodIds     = itemsByMealId.values.flatten().map { it.foodId }.toSet()
        val foodsById   = if (foodIds.isEmpty()) emptyMap()
                          else foodRepo.findAllById(foodIds).associateBy { it.id }

        return dietMeals.map { dm ->
            val meal  = mealsById[dm.mealId]
            val items = (itemsByMealId[dm.mealId] ?: emptyList()).mapNotNull { item ->
                val food = foodsById[item.foodId] ?: return@mapNotNull null
                TodayMealItemDto(foodId = food.id, foodName = food.name,
                    quantity = item.quantity, unit = item.unit,
                    caloriesPer100 = food.caloriesPer100, proteinPer100 = food.proteinPer100,
                    carbsPer100 = food.carbsPer100, fatPer100 = food.fatPer100, notes = item.notes)
            }
            val slotNutrition = items.fold(NutritionTotals()) { acc, i ->
                val g = if (i.unit == "GRAM") i.quantity else i.quantity * 100.0
                val f = g / 100.0
                acc.copy(kcal = acc.kcal + i.caloriesPer100 * f,
                    protein = acc.protein + i.proteinPer100 * f,
                    carbs   = acc.carbs   + i.carbsPer100   * f,
                    fat     = acc.fat     + i.fatPer100     * f)
            }
            SlotStatusDto(
                slot     = dm.slot,
                mealId   = meal?.id,
                mealName = meal?.name,
                isLogged = loggedSlots[dm.slot]?.isLogged == true,
                kcal     = round1(slotNutrition.kcal),
                protein  = round1(slotNutrition.protein),
                carbs    = round1(slotNutrition.carbs),
                fat      = round1(slotNutrition.fat),
                items    = items
            )
        }
    }

    private fun computeStreak(firebaseUid: String, today: LocalDate): StreakDto {
        val since  = today.minusDays(89)
        val allSlots = slotRepo.findByFirebaseUidAndDateBetween(firebaseUid, since, today)
        val datesWithLog = allSlots.filter { it.isLogged }.map { it.date }.toSet()

        // Current streak — walk back from today
        var current = 0
        var cursor  = today
        while (cursor >= since && cursor in datesWithLog) { current++; cursor = cursor.minusDays(1) }

        // Best streak in the 90-day window
        var best = 0; var run = 0
        for (offset in 0L..89L) {
            val d = since.plusDays(offset)
            if (d in datesWithLog) { run++; if (run > best) best = run } else run = 0
        }

        // 7-day dots (index 0 = 6 days ago, index 6 = today)
        val dots = (6 downTo 0).map { today.minusDays(it.toLong()) in datesWithLog }

        return StreakDto(current = current, best = best, dots = dots)
    }

    private data class NutritionTotals(
        val kcal: Double = 0.0, val protein: Double = 0.0,
        val carbs: Double = 0.0, val fat: Double = 0.0
    ) {
        operator fun plus(other: NutritionTotals) = NutritionTotals(
            kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat)
    }

    private fun computeNutrition(loggedFoods: List<LoggedFood>, foodsById: Map<Long, Food>): NutritionTotals =
        loggedFoods.fold(NutritionTotals()) { acc, lf ->
            val food = foodsById[lf.foodId] ?: return@fold acc
            val g = if (lf.unit == "GRAM") lf.quantity else lf.quantity * 100.0
            val f = g / 100.0
            acc.copy(kcal = acc.kcal + food.caloriesPer100 * f,
                protein = acc.protein + food.proteinPer100 * f,
                carbs   = acc.carbs   + food.carbsPer100   * f,
                fat     = acc.fat     + food.fatPer100     * f)
        }

    private fun round1(v: Double) = Math.round(v * 10) / 10.0
}
