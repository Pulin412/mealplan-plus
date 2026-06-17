package com.mealplanplus.api.domain.agent

import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.log.DailyLog
import com.mealplanplus.api.domain.log.DailyLogRepository
import com.mealplanplus.api.domain.log.LoggedFood
import com.mealplanplus.api.domain.log.LoggedFoodRepository
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
    private val loggedFoodRepo: LoggedFoodRepository
) {

    private val uid: String
        get() = SecurityContextHolder.getContext().authentication?.name ?: ""

    @Tool(description = """
        Search the food database by name. Returns up to 8 matching foods with their IDs,
        calories, protein, carbs, and fat per 100g. Always call this before logFood to get
        the correct food ID and nutritional data.
    """)
    fun searchFoods(query: String): String {
        if (query.isBlank()) return "Please provide a food name to search."
        val page = foodService.search(query, uid, PageRequest.of(0, 8))
        if (page.isEmpty) return "No foods found matching '$query'. Try a simpler name."
        return page.content.joinToString("\n") { f ->
            "id=${f.id} | ${f.name}${f.brand?.let { " ($it)" } ?: ""} | " +
            "${f.caloriesPer100.toInt()} kcal | ${f.proteinPer100}g protein | " +
            "${f.carbsPer100}g carbs | ${f.fatPer100}g fat (per 100g)"
        }
    }

    @Tool(description = """
        Get a summary of all foods already logged for a given date (format: YYYY-MM-DD).
        Use this to understand what the user has already eaten before making suggestions
        or to avoid logging duplicates.
    """)
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

    @Tool(description = """
        Log a food item for the user. Parameters:
        - foodId: the ID returned by searchFoods
        - quantity: numeric amount (e.g. 100.0, 2.0)
        - unit: GRAM, PIECE, CUP, TBSP, or TSP
        - slot: meal slot — BREAKFAST, LUNCH, DINNER, MORNING_SNACK, or EVENING_SNACK
        - date: date in YYYY-MM-DD format
        Returns a confirmation with the food name and estimated calories.
    """)
    @Transactional
    fun logFood(foodId: Long, quantity: Double, unit: String, slot: String, date: String): String {
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
        return "Logged ${food.name} — ${quantity}${unit.lowercase()} (~${kcal} kcal) to $slot on $date."
    }
}
