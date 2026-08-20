package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.dashboard.DashboardService
import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.grocery.GroceryService
import com.mealplanplus.api.domain.health.HealthMetricService
import com.mealplanplus.api.domain.log.LoggingService
import com.mealplanplus.api.domain.meal.MealService
import com.mealplanplus.api.domain.plan.DayPlanService
import com.mealplanplus.api.domain.user.UserService
import com.mealplanplus.api.domain.workout.WorkoutService
import com.mealplanplus.api.generated.model.AddLoggedFoodRequest
import com.mealplanplus.api.generated.model.FoodUnit
import com.mealplanplus.api.generated.model.HealthMetricDto
import com.mealplanplus.api.generated.model.MealDto
import com.mealplanplus.api.generated.model.MealFoodItemDto
import org.springframework.ai.tool.annotation.Tool
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
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
    private val groceryService: GroceryService,
    private val healthService: HealthMetricService,
    private val dayPlanService: DayPlanService,
    private val workoutService: WorkoutService,
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

    @Tool(description = """
        Get the individual foods the user logged on a day (each line's id, food name, quantity, unit, and
        slot). Pass an optional date as YYYY-MM-DD; omit for today. Use this to see exactly what was eaten
        (todayDashboard only gives per-slot totals) or to find a logged-food id to remove.
    """)
    fun getLoggedFoods(date: String?): String {
        if (uid.isBlank()) return NOT_AUTHED
        val day = parseDate(date)
        val foods = loggingService.getFoods(uid, day)
        if (foods.isEmpty()) return "Nothing logged on $day."
        return foods.joinToString("\n") { f ->
            val name = f.mealName ?: runCatching { foodService.get(f.foodId, uid).name }.getOrNull() ?: "food #${f.foodId}"
            "id=${f.id} | ${f.mealSlot} | $name | ${f.quantity} ${f.unit.value}"
        }
    }

    @Tool(description = """
        List the current user's reusable meals (named groups of foods). Returns each meal's id, name,
        whether it's a favorite, and how many foods it contains. Use this to find a meal id before
        referencing one, or to see what meals already exist before creating a new one.
    """)
    fun listMeals(): String {
        if (uid.isBlank()) return NOT_AUTHED
        val meals = mealService.list(uid)
        if (meals.isEmpty()) return "You have no meals yet."
        return meals.joinToString("\n") { m ->
            "id=${m.id} | ${m.name}${if (m.isFavorite == true) " (favorite)" else ""} | ${m.items.orEmpty().size} food(s)"
        }
    }

    @Tool(description = """
        Get the full details of one diet by id: its description, daily targets, the meals it schedules
        (day-of-week + slot + meal name) and any standalone food items. Get the id from listDiets first.
    """)
    fun getDietDetails(dietId: Long): String {
        if (uid.isBlank()) return NOT_AUTHED
        val diet = runCatching { dietService.get(dietId, uid) }.getOrNull()
            ?: return "Diet id=$dietId not found. Use listDiets to find the right id."
        val mealNames = mealService.list(uid).associate { it.id to it.name }
        return buildString {
            appendLine("${diet.name}${if (diet.isFavorite == true) " (favorite)" else ""}")
            diet.description?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            appendLine("Targets: ${diet.targetCalories?.toInt() ?: "—"} kcal, ${diet.targetProtein?.toInt() ?: "—"}g P, ${diet.targetCarbs?.toInt() ?: "—"}g C, ${diet.targetFat?.toInt() ?: "—"}g F")
            val meals = diet.meals.orEmpty()
            if (meals.isEmpty()) append("No meals scheduled.") else {
                appendLine("Meals:")
                append(meals.joinToString("\n") { dm ->
                    "  ${DAYS.getOrElse(dm.dayOfWeek) { "Day ${dm.dayOfWeek}" }} ${dm.slot}: ${mealNames[dm.mealId] ?: "meal #${dm.mealId}"}"
                })
            }
            val items = diet.foodItems.orEmpty()
            if (items.isNotEmpty()) append("\n${items.size} standalone food item(s).")
        }
    }

    @Tool(description = """
        List the user's health readings, most useful ones first. Optional type filters to one metric
        (e.g. WEIGHT, GLUCOSE, STEPS, CALORIES_BURNED, or a custom type name); from and to bound the date
        range as YYYY-MM-DD. Returns up to 20 readings with value, unit, and when they were recorded.
    """)
    fun listHealthMetrics(type: String?, from: String?, to: String?): String {
        if (uid.isBlank()) return NOT_AUTHED
        val fromDate = from?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val toDate = to?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val metrics = healthService.list(uid, type?.takeIf { it.isNotBlank() }?.uppercase(), fromDate, toDate)
        if (metrics.isEmpty()) return "No health readings found${type?.let { " for $it" } ?: ""}."
        return metrics.sortedByDescending { it.recordedAt }.take(20).joinToString("\n") { hm ->
            "${hm.recordedAt} | ${hm.type} | ${hm.value}${hm.secondaryValue?.let { "/$it" } ?: ""} ${hm.unit}"
        }
    }

    @Tool(description = """
        Get the user's plan for a day: the diet assigned to that day (with its targets), the meals planned
        per slot, and any planned workouts. Pass an optional date as YYYY-MM-DD; omit for today. Use this
        to see the full picture of what the user intends to follow, eat, and train on a date.
    """)
    fun getDayPlan(date: String?): String {
        if (uid.isBlank()) return NOT_AUTHED
        val day = parseDate(date)
        val plan = dayPlanService.get(uid, day) ?: return "No plan for $day."
        val mealNames = mealService.list(uid).associate { it.id to it.name }
        val diet = plan.dietId?.let { id -> runCatching { dietService.get(id, uid) }.getOrNull() }
        val meals = plan.plannedMeals.orEmpty()
        val workouts = plan.plannedWorkouts.orEmpty()
        if (diet == null && meals.isEmpty() && workouts.isEmpty()) return "Plan for $day is empty."
        return buildString {
            appendLine("Plan for $day:")
            if (diet != null) {
                appendLine("Diet: ${diet.name} (id=${diet.id}) — ${diet.targetCalories?.toInt() ?: "—"} kcal, ${diet.targetProtein?.toInt() ?: "—"}g P, ${diet.targetCarbs?.toInt() ?: "—"}g C, ${diet.targetFat?.toInt() ?: "—"}g F")
            }
            if (meals.isNotEmpty()) {
                appendLine("Meals:")
                appendLine(meals.joinToString("\n") { "  ${it.slot}: ${mealNames[it.mealId] ?: "meal #${it.mealId}"}" })
            }
            if (workouts.isNotEmpty()) {
                append("Workouts:\n")
                append(workouts.joinToString("\n") { "  ${it.activityName}" })
            }
        }.trimEnd()
    }

    @Tool(description = """
        List the user's workout templates (reusable routines). Returns each template's id, name, and how
        many exercises it contains. Use this to see what routines exist.
    """)
    fun listWorkoutTemplates(): String {
        if (uid.isBlank()) return NOT_AUTHED
        val templates = workoutService.listTemplates(uid)
        if (templates.isEmpty()) return "You have no workout templates yet."
        return templates.joinToString("\n") { t ->
            "id=${t.id} | ${t.name} | ${t.exercises.orEmpty().size} exercise(s)"
        }
    }

    @Tool(description = """
        List the user's workout sessions (logged workouts). Optional from and to bound the date range as
        YYYY-MM-DD. Returns up to 20 sessions with id, name, date, duration, and whether completed.
    """)
    fun listWorkoutSessions(from: String?, to: String?): String {
        if (uid.isBlank()) return NOT_AUTHED
        val fromDate = from?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val toDate = to?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val sessions = workoutService.listSessions(uid, fromDate, toDate)
        if (sessions.isEmpty()) return "No workout sessions found."
        return sessions.sortedByDescending { it.date }.take(20).joinToString("\n") { s ->
            "id=${s.id} | ${s.date ?: "—"} | ${s.name} | ${s.durationMinutes?.let { "$it min" } ?: "—"} | ${if (s.isCompleted == true) "completed" else "in progress"}"
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

    @Tool(description = """
        Remove a previously logged food. Get loggedFoodId from getLoggedFoods (the id= on each line, NOT
        the foodId). Requires a read-write connector. Use this to undo a mistaken or unwanted log entry.
    """)
    @Transactional
    fun removeLoggedFood(loggedFoodId: Long): String {
        val guard = guardWrite(); if (guard != null) return guard
        return runCatching { loggingService.removeFood(uid, loggedFoodId); "Removed logged food id=$loggedFoodId." }
            .getOrElse { "Could not remove id=$loggedFoodId — it doesn't exist or isn't yours. Use getLoggedFoods to find valid ids." }
    }

    @Tool(description = """
        Record a health reading. type is e.g. WEIGHT, GLUCOSE, STEPS, CALORIES_BURNED (or a custom type
        name); value is the number. unit is optional and defaults sensibly per type (WEIGHT→kg, GLUCOSE→
        mg/dL, STEPS→steps, CALORIES_BURNED→kcal). date is YYYY-MM-DD (omit for now). Requires a
        read-write connector.
    """)
    @Transactional
    fun logHealthMetric(type: String, value: Double, unit: String?, date: String?): String {
        val guard = guardWrite(); if (guard != null) return guard
        val theType = type.trim().uppercase()
        if (theType.isEmpty()) return "A metric type is required (e.g. WEIGHT)."
        if (value.isNaN() || value.isInfinite()) return "value must be a real number."
        val theUnit = unit?.takeIf { it.isNotBlank() } ?: DEFAULT_UNITS[theType] ?: ""
        if (theUnit.isEmpty()) return "Please provide a unit for '$type'."
        val recordedAt: Instant = if (date.isNullOrBlank()) Instant.now() else {
            val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                ?: return "Invalid date '$date'. Use YYYY-MM-DD."
            parsed.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        }
        val saved = healthService.create(HealthMetricDto(type = theType, value = value, unit = theUnit, recordedAt = recordedAt), uid)
        return "Recorded ${saved.type} = ${saved.value} ${saved.unit} at ${saved.recordedAt}."
    }

    @Tool(description = """
        Create a shopping list from a diet by aggregating all its meals' foods into quantities. Get dietId
        from listDiets. Returns the new list's name and items. Requires a read-write connector.
    """)
    @Transactional
    fun createGroceryFromDiet(dietId: Long): String {
        val guard = guardWrite(); if (guard != null) return guard
        val list = runCatching { groceryService.createFromDiet(dietId, uid) }.getOrNull()
            ?: return "Diet id=$dietId not found. Use listDiets to find the right id."
        val items = list.items.orEmpty()
        val body = if (items.isEmpty()) "  (no items — the diet has no meals with foods)"
        else items.joinToString("\n") { "  ${it.name} — ${it.quantity.toInt()} ${it.unit.value}" }
        return "Created '${list.name}' (id=${list.id}) with ${items.size} item(s):\n$body"
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
        // dayOfWeek is 0-based (Monday=0), matching DietMealDto.
        val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val DEFAULT_UNITS = mapOf(
            "WEIGHT" to "kg", "GLUCOSE" to "mg/dL", "STEPS" to "steps", "CALORIES_BURNED" to "kcal",
        )
    }
}
