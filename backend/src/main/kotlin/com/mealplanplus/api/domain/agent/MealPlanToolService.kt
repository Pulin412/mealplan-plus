package com.mealplanplus.api.domain.agent

import com.mealplanplus.api.domain.diet.DietMealRepository
import com.mealplanplus.api.domain.diet.DietRepository
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.health.HealthMetricRepository
import com.mealplanplus.api.domain.log.DailyLog
import com.mealplanplus.api.domain.log.DailyLogRepository
import com.mealplanplus.api.domain.log.LoggedFood
import com.mealplanplus.api.domain.log.LoggedFoodRepository
import com.mealplanplus.api.domain.meal.MealFoodItemRepository
import com.mealplanplus.api.domain.meal.MealRepository
import com.mealplanplus.api.domain.user.UserRepository
import org.springframework.ai.tool.annotation.Tool
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class MealPlanToolService(
    private val foodService: FoodService,
    private val dailyLogRepo: DailyLogRepository,
    private val loggedFoodRepo: LoggedFoodRepository,
    private val dietRepo: DietRepository,
    private val dietMealRepo: DietMealRepository,
    private val mealRepo: MealRepository,
    private val mealFoodItemRepo: MealFoodItemRepository,
    private val healthMetricRepo: HealthMetricRepository,
    private val userRepo: UserRepository
) {

    private val uid: String
        get() = SecurityContextHolder.getContext().authentication?.name ?: ""

    @Tool(description = "Search the food database by name; returns up to 8 matches with id, calories " +
        "and macros per 100g. Use only for lookups; to log food prefer logFoodByName.")
    fun searchFoods(query: String): String {
        if (query.isBlank()) return "Please provide a food name to search."
        val page = foodService.search(query, uid, PageRequest.of(0, 8))
        if (page.content.isEmpty()) return "No foods found matching '$query'. Try a simpler name."
        return page.content.joinToString("\n") { f ->
            "id=${f.id} | ${f.name}${f.brand?.let { " ($it)" } ?: ""} | " +
            "${f.caloriesPer100.toInt()} kcal | ${f.proteinPer100}g protein | " +
            "${f.carbsPer100}g carbs | ${f.fatPer100}g fat (per 100g)"
        }
    }

    @Tool(description = "Summary of everything logged on a date (YYYY-MM-DD): slot, foodId, quantity.")
    fun getTodayLog(date: String): String {
        val localDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
        val logs = dailyLogRepo.findByFirebaseUidAndDateBetweenOrderByDateAsc(uid, localDate, localDate)
        if (logs.isEmpty()) return "Nothing logged yet for $date."
        val foods = loggedFoodRepo.findByDailyLogIdIn(logs.map { it.id })
        if (foods.isEmpty()) return "Nothing logged yet for $date."
        return foods.joinToString("\n") { f ->
            "${f.mealSlot}: foodId=${f.foodId} | ${f.quantity}${f.unit.lowercase()}"
        }
    }

    @Tool(description = "Log food the user names, in ONE step: searches, picks the best match, and logs it. " +
        "unit is GRAM/PIECE/CUP/TBSP/TSP (default GRAM); slot is BREAKFAST/MORNING_SNACK/LUNCH/DINNER/" +
        "EVENING_SNACK; date is YYYY-MM-DD. If the match is ambiguous it returns candidates instead of " +
        "logging — then call logFood with the chosen id. Prefer this over searchFoods+logFood.")
    @Transactional
    fun logFoodByName(name: String, quantity: Double, unit: String, slot: String, date: String): String {
        if (name.isBlank()) return "Please tell me which food to log."
        val matches = foodService.search(name, uid, PageRequest.of(0, 8)).content
        if (matches.isEmpty()) return "No foods found matching '$name'. Try a simpler name."
        val exact = matches.filter { it.name.equals(name, ignoreCase = true) }
        val chosen = when {
            exact.size == 1 -> exact.first()
            exact.isEmpty() && matches.size == 1 -> matches.first()
            else -> null
        }
        if (chosen == null) {
            val list = matches.joinToString("\n") { "id=${it.id} | ${it.name}${it.brand?.let { b -> " ($b)" } ?: ""}" }
            return "Multiple foods match '$name' — ambiguous. Ask the user or call logFood with one of:\n$list"
        }
        val chosenId = chosen.id ?: return "Matched '${chosen.name}' but it has no id; try searchFoods."
        return persistLog(chosenId, quantity, unit, slot, date)
    }

    @Tool(description = "Log a food by its exact id (from searchFoods/logFoodByName candidates). " +
        "unit GRAM/PIECE/CUP/TBSP/TSP; slot BREAKFAST/MORNING_SNACK/LUNCH/DINNER/EVENING_SNACK; date YYYY-MM-DD.")
    @Transactional
    fun logFood(foodId: Long, quantity: Double, unit: String, slot: String, date: String): String =
        persistLog(foodId, quantity, unit, slot, date)

    /** Shared persistence + calorie confirmation for both logging tools. */
    private fun persistLog(foodId: Long, quantity: Double, unit: String, slot: String, date: String): String {
        val localDate = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
        val food = runCatching { foodService.get(foodId, uid) }.getOrNull()
            ?: return "Food with id=$foodId not found. Please search again."

        // Reuse existing daily log for this date or create a new one
        val log = dailyLogRepo.findFirstByFirebaseUidAndDateOrderByIdDesc(uid, localDate)
            ?: dailyLogRepo.save(DailyLog(firebaseUid = uid, date = localDate))

        loggedFoodRepo.save(
            LoggedFood(dailyLogId = log.id, foodId = foodId,
                mealSlot = slot, quantity = quantity, unit = unit.uppercase())
        )

        val grams = when (unit.uppercase()) {
            "GRAM"  -> quantity
            "PIECE" -> food.gramsPerPiece?.times(quantity) ?: quantity
            "CUP"   -> food.gramsPerCup?.times(quantity) ?: (quantity * 240)
            "TBSP"  -> food.gramsPerTbsp?.times(quantity) ?: (quantity * 15)
            "TSP"   -> quantity * 5
            else    -> quantity
        }
        val kcal = (food.caloriesPer100 * grams / 100).toInt()
        return "Logged ${food.name} — ${quantity}${unit.lowercase()} (~${kcal} kcal) to $slot on ${localDate}."
    }

    @Tool(description = "List the user's saved diets with macro targets and meal counts.")
    fun getDiets(): String {
        val diets = dietRepo.findByFirebaseUid(uid)
        if (diets.isEmpty()) return "The user has no saved diets."
        return diets.joinToString("\n") { d ->
            val mealCount = dietMealRepo.findByDietId(d.id).size
            val targets = listOfNotNull(
                d.targetCalories?.let { "${it.toInt()} kcal" },
                d.targetProtein?.let { "${it.toInt()}g protein" },
                d.targetCarbs?.let { "${it.toInt()}g carbs" },
                d.targetFat?.let { "${it.toInt()}g fat" }
            ).joinToString(", ").ifBlank { "no targets set" }
            "id=${d.id} | ${d.name}${if (d.isFavorite) " ⭐" else ""} | $targets | $mealCount meals" +
                (d.description?.let { " | $it" } ?: "")
        }
    }

    @Tool(description = "List the user's saved meals with the slots they fit and item counts.")
    fun getMeals(): String {
        val meals = mealRepo.findByFirebaseUid(uid)
        if (meals.isEmpty()) return "The user has no saved meals."
        return meals.joinToString("\n") { m ->
            val itemCount = mealFoodItemRepo.findByMealId(m.id).size
            val slots = m.slots.joinToString("/").ifBlank { "any slot" }
            "id=${m.id} | ${m.name}${if (m.isFavorite) " ⭐" else ""} | $slots | $itemCount items" +
                (m.notes?.let { " | $it" } ?: "")
        }
    }

    @Tool(description = "The user's latest health metric readings (weight, glucose, BP, …), one per type.")
    fun getMetrics(): String {
        val all = healthMetricRepo.findByFirebaseUid(uid)
        if (all.isEmpty()) return "The user has no recorded health metrics."
        return all.groupBy { it.type to it.subType }
            .map { (_, readings) -> readings.maxBy { it.recordedAt } }
            .sortedBy { it.type }
            .joinToString("\n") { mt ->
                val label = mt.subType?.let { "${mt.type} ($it)" } ?: mt.type
                "$label: ${mt.value}${mt.secondaryValue?.let { "/$it" } ?: ""} ${mt.unit} (on ${mt.recordedAt})"
            }
    }

    @Tool(description = "Everything the user logged over the last N days (default 7): date, slot, foodId, quantity.")
    fun getRecentLogs(days: Int): String {
        val window = if (days in 1..90) days else 7
        val to = LocalDate.now()
        val from = to.minusDays((window - 1).toLong())
        val logs = dailyLogRepo.findByFirebaseUidAndDateBetweenOrderByDateAsc(uid, from, to)
        if (logs.isEmpty()) return "Nothing logged between $from and $to."
        val logsById = logs.associateBy { it.id }
        val foods = loggedFoodRepo.findByDailyLogIdIn(logs.map { it.id })
        if (foods.isEmpty()) return "Nothing logged between $from and $to."
        return foods.joinToString("\n") { f ->
            val date = logsById[f.dailyLogId]?.date
            "$date | ${f.mealSlot}: foodId=${f.foodId} | ${f.quantity}${f.unit.lowercase()}"
        }
    }

    @Tool(description = "The user's profile: age, weight, height, activity level, goal, calorie/macro " +
        "targets and preferred units. Call this before making dietary suggestions so they fit the user's goal.")
    fun getProfile(): String {
        val u = userRepo.findByFirebaseUid(uid) ?: return "No profile found for this user."
        val bits = listOfNotNull(
            u.age?.let { "age ${it}" },
            u.weightKg?.let { "weight ${it}kg" },
            u.heightCm?.let { "height ${it}cm" },
            u.gender?.let { "gender $it" },
            u.activityLevel?.let { "activity $it" },
            u.goalType?.let { "goal $it" },
            u.targetWeightKg?.let { "target weight ${it}kg" },
            u.targetCalories?.let { "target ${it} kcal" },
            u.targetProtein?.let { "${it}g protein" },
            u.targetCarbs?.let { "${it}g carbs" },
            u.targetFat?.let { "${it}g fat" },
            "units ${u.preferredUnits}"
        )
        return if (bits.isEmpty()) "The user's profile has no details set yet."
        else bits.joinToString(", ")
    }
}
