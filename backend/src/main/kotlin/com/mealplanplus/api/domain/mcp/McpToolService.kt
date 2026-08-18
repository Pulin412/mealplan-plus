package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.dashboard.DashboardService
import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.log.LoggingService
import com.mealplanplus.api.domain.meal.MealService
import com.mealplanplus.api.domain.user.UserService
import com.mealplanplus.api.generated.model.AddLoggedFoodRequest
import com.mealplanplus.api.generated.model.FoodUnit
import com.mealplanplus.api.generated.model.MealDto
import com.mealplanplus.api.generated.model.MealFoodItemDto
import org.springframework.ai.tool.annotation.Tool
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/** A food line for [McpToolService.createMeal]; kept minimal so the generated tool schema is simple. */
data class McpMealFoodInput(val foodId: Long, val quantity: Double, val unit: String? = null)

/**
 * Tools exposed to a user's OWN AI agent (e.g. Claude) over the MCP server. Deliberately isolated in
 * `domain/mcp` and calling core services directly — nothing else depends on this package, so the whole
 * MCP surface can be removed by deleting `domain/mcp`, its config, and the `mcp_server` flag gate.
 *
 * `uid` is resolved from the SecurityContext, which [McpAuthFilter] populates from the connector token
 * (propagated to the tool-execution thread by [McpContextPropagationConfig]) — every tool is scoped to
 * the calling user. Reads work on any valid token; WRITES additionally require a read-write scoped token
 * and go through [guardWrite]/validation (scope, valid slot/unit, sane quantity, caps, idempotency).
 */
@Service
class McpToolService(
    private val dietService: DietService,
    private val dashboardService: DashboardService,
    private val userService: UserService,
    private val foodService: FoodService,
    private val loggingService: LoggingService,
    private val mealService: MealService,
) {
    private val uid: String
        get() = SecurityContextHolder.getContext().authentication?.name ?: ""

    private fun hasWriteScope(): Boolean =
        SecurityContextHolder.getContext().authentication?.authorities.orEmpty().any { it.authority == SCOPE_WRITE }

    // ── Reads ─────────────────────────────────────────────────────────────────

    @Tool(description = """
        List the current user's diets (their day-plan templates). Returns each diet's id, name,
        and whether it is marked a favorite. Use this to see what diets exist before referencing one.
    """)
    fun listDiets(): String {
        if (uid.isBlank()) return NOT_AUTHED
        val diets = dietService.list(uid)
        if (diets.isEmpty()) return "You have no diets yet."
        return diets.joinToString("\n") { d -> "id=${d.id} | ${d.name}${if (d.isFavorite == true) " (favorite)" else ""}" }
    }

    @Tool(description = """
        Get the user's dashboard for a day: calories consumed vs target, macros (protein/carbs/fat),
        and each meal slot with whether it's been logged. Pass an optional date as YYYY-MM-DD; omit it
        for today. Use this to understand what the user has eaten and how much room is left.
    """)
    fun todayDashboard(date: String?): String {
        if (uid.isBlank()) return NOT_AUTHED
        val d = dashboardService.get(uid, parseDate(date))
        val ring = d.calorieRing
        val m = d.macros
        val slots = d.slots.joinToString("\n") { s ->
            "  ${s.slot}: ${if (s.isLogged) "logged" else "not logged"} — ${s.kcal.toInt()} kcal" +
                (s.mealName?.let { " ($it)" } ?: "")
        }
        return buildString {
            appendLine("Date: ${d.date}")
            appendLine("Calories: ${ring.consumed.toInt()} / ${ring.target} kcal (${ring.remaining.toInt()} remaining${if (ring.isOver) ", OVER" else ""})")
            appendLine("Protein: ${m.consumedProtein.toInt()}g / ${m.targetProtein ?: "—"}g | Carbs: ${m.consumedCarbs.toInt()}g / ${m.targetCarbs ?: "—"}g | Fat: ${m.consumedFat.toInt()}g / ${m.targetFat ?: "—"}g")
            appendLine("Streak: ${d.streak.current} day(s)")
            append("Slots:\n$slots")
        }
    }

    @Tool(description = """
        Get the user's profile: body metrics (height, weight, age), goal, activity level, unit
        preference, and daily nutrition targets. Use this to personalize suggestions.
    """)
    fun getProfile(): String {
        if (uid.isBlank()) return NOT_AUTHED
        val u = userService.getOrCreate(uid)
        return buildString {
            u.displayName?.let { appendLine("Name: $it") }
            appendLine("Age: ${u.age ?: "—"} | Height: ${u.heightCm ?: "—"} cm | Weight: ${u.weightKg ?: "—"} kg | Units: ${u.units ?: "METRIC"}")
            appendLine("Goal: ${u.goalType ?: "—"} | Activity: ${u.activityLevel ?: "—"} | Gender: ${u.gender ?: "—"}")
            append("Targets: ${u.targetCalories ?: "—"} kcal, ${u.targetProtein ?: "—"}g protein, ${u.targetCarbs ?: "—"}g carbs, ${u.targetFat ?: "—"}g fat")
        }
    }

    @Tool(description = """
        Search the food database by name. Returns up to 8 matching foods with their ids and per-100g
        calories, protein, carbs, and fat. Use this to find a food (and its id) before logging it.
    """)
    fun searchFoods(query: String): String {
        if (uid.isBlank()) return NOT_AUTHED
        if (query.isBlank()) return "Please provide a food name to search."
        val page = foodService.search(query, uid, PageRequest.of(0, 8))
        if (page.content.isEmpty()) return "No foods found matching '$query'. Try a simpler name."
        return page.content.joinToString("\n") { f ->
            "id=${f.id} | ${f.name}${f.brand?.let { " ($it)" } ?: ""} | " +
                "${f.caloriesPer100.toInt()} kcal | ${f.proteinPer100}g P | ${f.carbsPer100}g C | ${f.fatPer100}g F (per 100g)"
        }
    }

    // ── Writes (require a read-write scoped token) ──────────────────────────────

    @Tool(description = """
        Log a food the user ate. Get foodId from searchFoods first. quantity must be positive; unit is
        one of GRAM, ML, PIECE, CUP, TBSP, TSP (default GRAM); slot is one of BREAKFAST, LUNCH, DINNER,
        MORNING_SNACK, EVENING_SNACK; date is YYYY-MM-DD (omit for today). Requires a read-write connector.
    """)
    @Transactional
    fun logFood(foodId: Long, quantity: Double, unit: String?, slot: String, date: String?): String {
        val guard = guardWrite(); if (guard != null) return guard

        val theSlot = slot.uppercase()
        if (theSlot !in VALID_SLOTS) return "Invalid slot '$slot'. Use one of: ${VALID_SLOTS.joinToString(", ")}."
        val theUnit = parseUnit(unit) ?: return "Invalid unit '$unit'. Use one of: ${FoodUnit.entries.joinToString(", ") { it.value }}."
        if (quantity <= 0 || quantity > MAX_QUANTITY) return "quantity must be between 0 and $MAX_QUANTITY."
        val day = parseDate(date)
        val food = runCatching { foodService.get(foodId, uid) }.getOrNull()
            ?: return "Food id=$foodId not found. Use searchFoods to find the right id."

        // Idempotency: don't double-log an identical entry (same food, slot, quantity, unit) on the same day.
        val duplicate = loggingService.getFoods(uid, day).any {
            it.foodId == foodId && it.mealSlot == theSlot && it.quantity == quantity && it.unit == theUnit
        }
        if (duplicate) return "Already logged ${food.name} (${quantity} ${theUnit.value}) to $theSlot on $day — skipped duplicate."

        loggingService.addFood(
            uid,
            AddLoggedFoodRequest(date = day, foodId = foodId, mealSlot = theSlot, quantity = quantity, unit = theUnit),
        )
        return "Logged ${food.name} — ${quantity} ${theUnit.value} to $theSlot on $day."
    }

    @Tool(description = """
        Create a reusable meal (a named group of foods) the user can add to their plan. name is required;
        foods is an optional list of { foodId, quantity, unit } (get foodId from searchFoods). Requires a
        read-write connector. If a meal with the same name already exists it is returned instead of duplicated.
    """)
    @Transactional
    fun createMeal(name: String, foods: List<McpMealFoodInput>?): String {
        val guard = guardWrite(); if (guard != null) return guard

        val trimmed = name.trim()
        if (trimmed.isEmpty()) return "A meal name is required."
        if (trimmed.length > MAX_NAME_LEN) return "Meal name is too long (max $MAX_NAME_LEN characters)."
        val items = foods.orEmpty()
        if (items.size > MAX_MEAL_FOODS) return "Too many foods (max $MAX_MEAL_FOODS per meal)."

        // Idempotency: reuse an existing meal with the same name rather than creating a duplicate.
        mealService.list(uid).firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.let {
            return "Meal '${it.name}' already exists (id=${it.id}) — returned instead of creating a duplicate."
        }

        val mealItems = items.map { f ->
            if (f.quantity <= 0 || f.quantity > MAX_QUANTITY) return "Each food quantity must be between 0 and $MAX_QUANTITY."
            val u = parseUnit(f.unit) ?: return "Invalid unit '${f.unit}' for food id=${f.foodId}."
            runCatching { foodService.get(f.foodId, uid) }.getOrNull()
                ?: return "Food id=${f.foodId} not found. Use searchFoods to find the right id."
            MealFoodItemDto(foodId = f.foodId, quantity = f.quantity, unit = u)
        }

        val created = mealService.create(MealDto(name = trimmed, items = mealItems), uid)
        return "Created meal '${created.name}' (id=${created.id}) with ${mealItems.size} food(s)."
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    /** Null when the write may proceed; otherwise a user-facing reason (not authenticated / read-only). */
    private fun guardWrite(): String? = when {
        uid.isBlank() -> NOT_AUTHED
        !hasWriteScope() -> "This connector is read-only. Reconnect with a read-write token to make changes."
        else -> null
    }

    private fun parseUnit(unit: String?): FoodUnit? =
        if (unit.isNullOrBlank()) FoodUnit.GRAM else FoodUnit.entries.firstOrNull { it.value.equals(unit, ignoreCase = true) }

    private fun parseDate(date: String?): LocalDate =
        date?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()

    private companion object {
        const val NOT_AUTHED = "Not authenticated."
        const val SCOPE_WRITE = "SCOPE_MCP_WRITE"
        const val MAX_QUANTITY = 100_000.0
        const val MAX_NAME_LEN = 100
        const val MAX_MEAL_FOODS = 50
        val VALID_SLOTS = setOf("BREAKFAST", "LUNCH", "DINNER", "MORNING_SNACK", "EVENING_SNACK")
    }
}
