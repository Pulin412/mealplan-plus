package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.dashboard.DashboardService
import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.diet.TagEntityType
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.grocery.GroceryService
import com.mealplanplus.api.domain.health.HealthMetricService
import com.mealplanplus.api.domain.log.LoggingService
import com.mealplanplus.api.domain.meal.MealService
import com.mealplanplus.api.domain.plan.DayPlanService
import com.mealplanplus.api.domain.user.UserService
import com.mealplanplus.api.domain.workout.WorkoutService
import com.mealplanplus.api.generated.model.AddLoggedFoodRequest
import com.mealplanplus.api.generated.model.DietDto
import com.mealplanplus.api.generated.model.DietFoodItemDto
import com.mealplanplus.api.generated.model.DietMealDto
import com.mealplanplus.api.generated.model.ExerciseDto
import com.mealplanplus.api.generated.model.FoodDto
import com.mealplanplus.api.generated.model.FoodUnit
import com.mealplanplus.api.generated.model.HealthMetricDto
import com.mealplanplus.api.generated.model.MealDto
import com.mealplanplus.api.generated.model.MealFoodItemDto
import com.mealplanplus.api.generated.model.TagDto
import com.mealplanplus.api.generated.model.TemplateExerciseDto
import com.mealplanplus.api.generated.model.WorkoutTemplateDto
import org.springframework.ai.tool.annotation.Tool
import org.springframework.data.domain.PageRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate

/** A food line for [McpToolService.createMeal]; kept minimal so the generated tool schema is simple. */
data class McpMealFoodInput(val foodId: Long, val quantity: Double, val unit: String? = null)

/**
 * One entry in a diet built via [McpToolService.createDiet]: a meal OR a loose food assigned to a slot.
 * Provide `mealId` for a whole meal, or `foodId` (+ quantity/unit) for a single food.
 */
data class McpDietEntryInput(
    val slot: String,
    val mealId: Long? = null,
    val foodId: Long? = null,
    val quantity: Double? = null,
    val unit: String? = null,
)

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
        Search the user's diets by name. Returns up to 10 matches with id and name. Prefer this over
        listDiets when you know roughly what you want, and search here before createDiet to reuse an
        existing diet instead of duplicating it.
    """)
    fun searchDiets(query: String): String {
        if (uid.isBlank()) return NOT_AUTHED
        if (query.isBlank()) return "Please provide a diet name to search."
        val q = query.trim()
        val matches = dietService.list(uid).filter { it.name.contains(q, ignoreCase = true) }.take(10)
        if (matches.isEmpty()) return "No diets found matching '$query'."
        return matches.joinToString("\n") { d -> "id=${d.id} | ${d.name}${if (d.isFavorite == true) " (favorite)" else ""}" }
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
        Search the food database by name. Returns up to 8 matching foods with their ids, per-100g
        calories/protein/carbs/fat, and each food's natural unit (with grams-per-unit when it is a
        count unit like PIECE). You can log or add ANY food in GRAM regardless of its natural unit —
        the server converts — so prefer GRAM for recipes. Use this to find a food (and its id) before
        logging it or adding it to a meal.
    """)
    fun searchFoods(query: String): String {
        if (uid.isBlank()) return NOT_AUTHED
        if (query.isBlank()) return "Please provide a food name to search."
        val page = foodService.search(query, uid, PageRequest.of(0, 8))
        if (page.content.isEmpty()) return "No foods found matching '$query'. Try a simpler name."
        return page.content.joinToString("\n") { f ->
            "id=${f.id} | ${f.name}${f.brand?.let { " ($it)" } ?: ""} | " +
                "${f.caloriesPer100.toInt()} kcal | ${f.proteinPer100}g P | ${f.carbsPer100}g C | ${f.fatPer100}g F (per 100g) | ${unitNote(f)}"
        }
    }

    /** Human hint about a food's natural unit + grams-per-unit, so the agent can convert to/from grams. */
    private fun unitNote(f: FoodDto): String {
        val u = f.unit ?: FoodUnit.GRAM
        val gpu = when (u) {
            FoodUnit.PIECE -> f.gramsPerPiece
            FoodUnit.CUP -> f.gramsPerCup
            FoodUnit.TBSP -> f.gramsPerTbsp
            FoodUnit.TSP -> f.gramsPerTsp
            else -> null
        }
        return if (gpu != null) "unit=${u.value} (1 ${u.value.lowercase()} ≈ ${num(gpu)}g; also loggable in GRAM)"
        else "unit=${u.value}"
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
        Search the user's meals by name and/or meal slot. Both params are optional: pass query to match
        by name, and/or slot to only return meals tagged for that slot (meals can be tagged for slots
        like Breakfast, Lunch, Dinner, Pre-Workout, etc.); slot matching is case-insensitive. With
        neither, nothing is searched. Returns up to 10 matches with id, name, food count, and the meal's
        slot tags. Prefer this over listMeals, and search here before createMeal to reuse an existing meal.
    """)
    fun searchMeals(query: String?, slot: String?): String {
        if (uid.isBlank()) return NOT_AUTHED
        val q = query?.trim().orEmpty()
        val s = slot?.trim().orEmpty()
        if (q.isEmpty() && s.isEmpty()) return "Please provide a meal name and/or a slot to search."
        val matches = mealService.list(uid)
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
            .filter { s.isEmpty() || slotMatches(it.slots.orEmpty(), s) }
            .take(10)
        if (matches.isEmpty()) {
            val what = buildString {
                if (q.isNotEmpty()) append(" matching '$q'")
                if (s.isNotEmpty()) append(" for slot '$s'")
            }
            return "No meals found$what."
        }
        return matches.joinToString("\n") { m ->
            val slotTags = m.slots.orEmpty()
            "id=${m.id} | ${m.name}${if (m.isFavorite == true) " (favorite)" else ""} | ${m.items.orEmpty().size} food(s)" +
                (if (slotTags.isNotEmpty()) " | slots: ${slotTags.joinToString(", ")}" else "")
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
        List the user's exercises. Returns each exercise's id, name, type (STRENGTH/CARDIO/TIMED), and any
        tags (e.g. a body part like Chest or Legs). Use searchExercises to filter by name or tag.
    """)
    fun listExercises(): String {
        if (uid.isBlank()) return NOT_AUTHED
        val exercises = workoutService.listExercises(uid)
        if (exercises.isEmpty()) return "You have no exercises yet."
        return exercises.joinToString("\n") { exerciseLine(it) }
    }

    @Tool(description = """
        Search the user's exercises by name and/or tag (tags are categories like a body part — Chest,
        Legs, Back). Both params optional: query matches the name, tag matches an assigned tag
        (case-insensitive). With neither, nothing is searched. Returns up to 15 matches with id, name,
        type, and tags.
    """)
    fun searchExercises(query: String?, tag: String?): String {
        if (uid.isBlank()) return NOT_AUTHED
        val q = query?.trim().orEmpty()
        val t = tag?.trim().orEmpty()
        if (q.isEmpty() && t.isEmpty()) return "Please provide an exercise name and/or a tag to search."
        val matches = workoutService.listExercises(uid, q.ifEmpty { null })
            .filter { t.isEmpty() || tagsContain(it.tags, t) }
            .take(15)
        if (matches.isEmpty()) {
            val what = buildString {
                if (q.isNotEmpty()) append(" matching '$q'")
                if (t.isNotEmpty()) append(" with tag '$t'")
            }
            return "No exercises found$what."
        }
        return matches.joinToString("\n") { exerciseLine(it) }
    }

    @Tool(description = """
        Create a new exercise. name is required; type is STRENGTH (reps + weight), CARDIO (reps + weight,
        optional distance), or TIMED (minutes + seconds), default STRENGTH. tags is an optional list of tag
        names to categorise it (e.g. a body part like "Chest" or "Legs") — tags are created if they don't
        exist yet. Use a clear name; ask the user if unsure. Requires a read-write connector. Reuses an
        existing same-name exercise instead of duplicating.
    """)
    @Transactional
    fun createExercise(name: String, type: String?, tags: List<String>?): String {
        val guard = guardWrite(); if (guard != null) return guard
        val nm = name.trim()
        if (nm.isEmpty()) return "An exercise name is required."
        if (nm.length > MAX_NAME_LEN) return "Exercise name is too long (max $MAX_NAME_LEN characters)."
        val t = (type?.trim()?.ifBlank { null } ?: "STRENGTH").uppercase()
        if (t !in EXERCISE_TYPES) return "Invalid type '$type'. Use one of: ${EXERCISE_TYPES.joinToString(", ")}."
        workoutService.listExercises(uid, nm).firstOrNull { it.name.equals(nm, ignoreCase = true) && it.isSystem != true }?.let {
            return "Exercise '${it.name}' already exists (id=${it.id}) — reuse that instead of creating a duplicate."
        }
        val tagIds = resolveTagIds(tags.orEmpty(), TagEntityType.EXERCISE)
        val created = workoutService.createExercise(ExerciseDto(name = nm, type = t, tagIds = tagIds), uid)
        return "Created exercise '${created.name}' (id=${created.id}) — $t${if (tagIds.isNotEmpty()) ", ${tagIds.size} tag(s)" else ""}."
    }

    @Tool(description = """
        Delete one of the user's own exercises by id (get the id from searchExercises or listExercises).
        Shared system exercises are protected and can't be deleted. Requires a read-write connector.
    """)
    fun deleteExercise(exerciseId: Long): String {
        val guard = guardWrite(); if (guard != null) return guard
        val ex = runCatching { workoutService.getExercise(exerciseId) }.getOrNull()
            ?: return "No exercise found with id=$exerciseId."
        if (ex.isSystem == true)
            return "'${ex.name}' is a shared system exercise and can't be deleted — you can only delete exercises you added."
        return try {
            workoutService.deleteExercise(exerciseId, uid)
            "Deleted exercise '${ex.name}' (id=$exerciseId)."
        } catch (e: ResponseStatusException) {
            "Can't delete '${ex.name}' — it isn't one of your exercises."
        }
    }

    @Tool(description = """
        Search the user's workout templates by name and/or tag (case-insensitive). Both params optional;
        with neither, nothing is searched. Returns up to 15 matches with id, name, exercise count, and tags.
    """)
    fun searchWorkouts(query: String?, tag: String?): String {
        if (uid.isBlank()) return NOT_AUTHED
        val q = query?.trim().orEmpty()
        val t = tag?.trim().orEmpty()
        if (q.isEmpty() && t.isEmpty()) return "Please provide a workout name and/or a tag to search."
        // Workout DTOs carry only tagIds (not tag DTOs), so resolve names via the WORKOUT tag list.
        val workoutTags = dietService.listTags(uid, TagEntityType.WORKOUT)
        val nameById = workoutTags.mapNotNull { tg -> tg.id?.let { it to tg.name } }.toMap()
        val wantedTagId = if (t.isEmpty()) null
            else workoutTags.firstOrNull { it.name.equals(t, ignoreCase = true) || it.name.lowercase().contains(t.lowercase()) }?.id
        if (t.isNotEmpty() && wantedTagId == null) return "No workouts found with tag '$t'."
        val matches = workoutService.listTemplates(uid)
            .filter { q.isEmpty() || it.name.contains(q, ignoreCase = true) }
            .filter { wantedTagId == null || it.tagIds.orEmpty().contains(wantedTagId) }
            .take(15)
        if (matches.isEmpty()) {
            val what = buildString {
                if (q.isNotEmpty()) append(" matching '$q'")
                if (t.isNotEmpty()) append(" with tag '$t'")
            }
            return "No workouts found$what."
        }
        return matches.joinToString("\n") { workoutLine(it, nameById) }
    }

    @Tool(description = """
        Create a workout template (a reusable routine) from a list of exercise ids (get them from
        searchExercises). name is required; exerciseIds is the ordered list of exercises; tags is an
        optional list of tag names (created if missing). Use a clear name; ask the user if unsure.
        Requires a read-write connector. Reuses an existing same-name workout instead of duplicating.
    """)
    @Transactional
    fun createWorkout(name: String, exerciseIds: List<Long>?, tags: List<String>?): String {
        val guard = guardWrite(); if (guard != null) return guard
        val nm = name.trim()
        if (nm.isEmpty()) return "A workout name is required."
        if (nm.length > MAX_NAME_LEN) return "Workout name is too long (max $MAX_NAME_LEN characters)."
        val ids = exerciseIds.orEmpty()
        if (ids.size > MAX_MEAL_FOODS) return "Too many exercises (max $MAX_MEAL_FOODS)."
        workoutService.listTemplates(uid).firstOrNull { it.name.equals(nm, ignoreCase = true) }?.let {
            return "Workout '${it.name}' already exists (id=${it.id}) — returned instead of creating a duplicate."
        }
        val exercises = ids.mapIndexed { i, exId ->
            runCatching { workoutService.getExercise(exId) }.getOrNull()
                ?: return "Exercise id=$exId not found. Use searchExercises to find the right id."
            TemplateExerciseDto(exerciseId = exId, orderIndex = i)
        }
        val tagIds = resolveTagIds(tags.orEmpty(), TagEntityType.WORKOUT)
        val created = workoutService.createTemplate(WorkoutTemplateDto(name = nm, exercises = exercises, tagIds = tagIds), uid)
        return "Created workout '${created.name}' (id=${created.id}) with ${exercises.size} exercise(s)${if (tagIds.isNotEmpty()) ", ${tagIds.size} tag(s)" else ""}."
    }

    @Tool(description = """
        Delete one of the user's own workout templates by id (get the id from searchWorkouts or
        listWorkoutTemplates). Only the user's own workouts can be deleted. Requires a read-write connector.
    """)
    fun deleteWorkout(workoutId: Long): String {
        val guard = guardWrite(); if (guard != null) return guard
        val t = runCatching { workoutService.getTemplate(workoutId) }.getOrNull()
            ?: return "No workout found with id=$workoutId."
        return try {
            workoutService.deleteTemplate(workoutId, uid)
            "Deleted workout '${t.name}' (id=$workoutId)."
        } catch (e: ResponseStatusException) {
            "Can't delete '${t.name}' — it isn't one of your workouts."
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
        Create a new custom food (per-100g nutrition) in the user's library so it can be logged or
        added to meals. Only use this when searchFoods finds no suitable match — search first to avoid
        duplicates. name plus per-100g calories/protein/carbs/fat are required; fiberPer100 /
        sugarsPer100 / saturatedFatPer100 / sodiumPer100 (grams per 100g) are optional; unit is one of
        GRAM, ML, PIECE, CUP, TBSP, TSP (default GRAM). Returns the new food's id. Requires a
        read-write connector. Give the food a clear, specific name (e.g. "Arla Skyr Vanilla", not
        "food 1" or "test") — if the user hasn't named it and you're unsure, ask them before creating.
    """)
    fun createFood(
        name: String,
        caloriesPer100: Double,
        proteinPer100: Double,
        carbsPer100: Double,
        fatPer100: Double,
        fiberPer100: Double?,
        sugarsPer100: Double?,
        saturatedFatPer100: Double?,
        sodiumPer100: Double?,
        unit: String?,
    ): String {
        val guard = guardWrite(); if (guard != null) return guard
        val nm = name.trim()
        if (nm.isEmpty()) return "Please provide a food name."
        if (nm.length > MAX_NAME_LEN) return "Food name too long (max $MAX_NAME_LEN characters)."
        val u = parseUnit(unit) ?: return "Invalid unit '$unit'. Use one of: ${FoodUnit.entries.joinToString(", ") { it.value }}."
        for ((label, v) in listOf("calories" to caloriesPer100, "protein" to proteinPer100, "carbs" to carbsPer100, "fat" to fatPer100)) {
            if (v < 0 || v > MAX_QUANTITY) return "$label per 100g must be between 0 and $MAX_QUANTITY."
        }
        // Don't create a duplicate if the user already has a food with this exact name.
        val existing = foodService.search(nm, uid, PageRequest.of(0, 8)).content
            .firstOrNull { it.name.equals(nm, ignoreCase = true) && it.isSystemFood != true }
        if (existing != null) return "Food '${existing.name}' already exists (id=${existing.id}) — reuse that instead of creating a duplicate."
        val dto = FoodDto(
            name = nm,
            caloriesPer100 = caloriesPer100,
            proteinPer100 = proteinPer100,
            carbsPer100 = carbsPer100,
            fatPer100 = fatPer100,
            fiberPer100 = fiberPer100,
            sugarsPer100 = sugarsPer100,
            saturatedFatPer100 = saturatedFatPer100,
            sodiumPer100 = sodiumPer100,
            unit = u,
        )
        val saved = foodService.create(dto, uid)
        return "Created food '${saved.name}' (id=${saved.id}) — ${caloriesPer100.toInt()} kcal per 100g. Log or add it in GRAM."
    }

    @Tool(description = """
        Delete one of the user's OWN custom foods by id (get the id from searchFoods). Only foods the
        user manually added can be deleted — shared system-catalog foods are protected and are never
        removed. Requires a read-write connector.
    """)
    fun deleteFood(foodId: Long): String {
        val guard = guardWrite(); if (guard != null) return guard
        val food = runCatching { foodService.get(foodId, uid) }.getOrNull()
            ?: return "No food found with id=$foodId."
        if (food.isSystemFood == true)
            return "'${food.name}' is a shared system food and can't be deleted — you can only delete foods you added yourself."
        return try {
            foodService.delete(foodId, uid)
            "Deleted food '${food.name}' (id=$foodId)."
        } catch (e: ResponseStatusException) {
            "Can't delete '${food.name}' — it isn't one of your foods."
        }
    }

    @Tool(description = """
        Create a reusable meal (a named group of foods) the user can add to their plan. name is required;
        foods is an optional list of { foodId, quantity, unit } (get foodId from searchFoods). Requires a
        read-write connector. If a meal with the same name already exists it is returned instead of duplicated.
        Give the meal a clear, descriptive name (e.g. "Grilled Paneer Salad", not "meal 1" or "test") — if
        the user hasn't said what to call it and you're unsure, ask them before creating.
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
        Delete one of the user's own meals by id (get the id from searchMeals or listMeals). Only the
        user's own meals can be deleted. Requires a read-write connector.
    """)
    fun deleteMeal(mealId: Long): String {
        val guard = guardWrite(); if (guard != null) return guard
        val meal = runCatching { mealService.get(mealId, uid) }.getOrNull()
            ?: return "No meal found with id=$mealId."
        return try {
            mealService.delete(mealId, uid)
            "Deleted meal '${meal.name}' (id=$mealId)."
        } catch (e: ResponseStatusException) {
            "Can't delete '${meal.name}' — it isn't one of your meals."
        }
    }

    @Tool(description = """
        Create a reusable diet (a one-day plan template) — the same as building a diet in the app: meals
        and/or foods arranged by meal slot. name is required. entries is a list where each item is either
        { slot, mealId } to put a whole meal in a slot, or { slot, foodId, quantity, unit } to put a loose
        food in a slot (get ids from searchMeals / searchFoods; quantity defaults to 1, unit to GRAM).
        Valid slots (case-insensitive): Early Morning, Breakfast, Noon, Lunch, Evening, Pre-Workout,
        Post-Workout, Dinner, Post-Dinner. Optionally set target calories/protein/carbs/fat. Ask the user
        which meals or foods go in which slots before building, and give the diet a clear, descriptive
        name. Requires a read-write connector. Reuses an existing same-name diet instead of duplicating.
    """)
    @Transactional
    fun createDiet(
        name: String,
        entries: List<McpDietEntryInput>?,
        targetCalories: Double?,
        targetProtein: Double?,
        targetCarbs: Double?,
        targetFat: Double?,
    ): String {
        val guard = guardWrite(); if (guard != null) return guard
        val nm = name.trim()
        if (nm.isEmpty()) return "A diet name is required."
        if (nm.length > MAX_NAME_LEN) return "Diet name is too long (max $MAX_NAME_LEN characters)."
        val items = entries.orEmpty()
        if (items.size > MAX_DIET_ENTRIES) return "Too many entries (max $MAX_DIET_ENTRIES per diet)."

        dietService.list(uid).firstOrNull { it.name.equals(nm, ignoreCase = true) }?.let {
            return "Diet '${it.name}' already exists (id=${it.id}) — returned instead of creating a duplicate."
        }

        val meals = mutableListOf<DietMealDto>()
        val foodItems = mutableListOf<DietFoodItemDto>()
        for (e in items) {
            val slot = resolveSlot(e.slot) ?: return "Invalid slot '${e.slot}'. Use one of: ${CANONICAL_SLOTS.joinToString(", ")}."
            when {
                e.mealId != null -> {
                    runCatching { mealService.get(e.mealId, uid) }.getOrNull()
                        ?: return "Meal id=${e.mealId} not found. Use searchMeals to find the right id."
                    meals += DietMealDto(mealId = e.mealId, dayOfWeek = 0, slot = slot)
                }
                e.foodId != null -> {
                    val u = parseUnit(e.unit) ?: return "Invalid unit '${e.unit}' for food id=${e.foodId}."
                    val qty = e.quantity ?: 1.0
                    if (qty <= 0 || qty > MAX_QUANTITY) return "Each food quantity must be between 0 and $MAX_QUANTITY."
                    runCatching { foodService.get(e.foodId, uid) }.getOrNull()
                        ?: return "Food id=${e.foodId} not found. Use searchFoods to find the right id."
                    foodItems += DietFoodItemDto(foodId = e.foodId, slot = slot, quantity = qty, unit = u)
                }
                else -> return "Each entry needs a slot plus either a mealId or a foodId."
            }
        }

        val created = dietService.create(
            DietDto(
                name = nm, meals = meals, foodItems = foodItems,
                targetCalories = targetCalories, targetProtein = targetProtein,
                targetCarbs = targetCarbs, targetFat = targetFat,
            ),
            uid,
        )
        val slotCount = (meals.map { it.slot } + foodItems.map { it.slot }).distinct().size
        return "Created diet '${created.name}' (id=${created.id}) with ${meals.size} meal(s) and ${foodItems.size} food(s) across $slotCount slot(s)."
    }

    @Tool(description = """
        Delete one of the user's own diets by id (get the id from searchDiets or listDiets). Only the
        user's own diets can be deleted. Requires a read-write connector.
    """)
    fun deleteDiet(dietId: Long): String {
        val guard = guardWrite(); if (guard != null) return guard
        val diet = runCatching { dietService.get(dietId, uid) }.getOrNull()
            ?: return "No diet found with id=$dietId."
        return try {
            dietService.delete(dietId, uid)
            "Deleted diet '${diet.name}' (id=$dietId)."
        } catch (e: ResponseStatusException) {
            "Can't delete '${diet.name}' — it isn't one of your diets."
        }
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

    /** Trim a trailing .0 so "50.0" prints as "50". */
    private fun num(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

    /** Lenient slot-tag match: ignores case and non-letters so "PRE_WORKOUT" matches a "Pre-Workout" tag. */
    private fun slotMatches(mealSlots: List<String>, wanted: String): Boolean {
        fun norm(x: String) = x.uppercase().filter { it.isLetter() }
        val w = norm(wanted)
        if (w.isEmpty()) return false
        return mealSlots.any { val n = norm(it); n == w || n.contains(w) || w.contains(n) }
    }

    /** Resolve a free-form slot to its canonical app value (title case) so diets render like the app. */
    private fun resolveSlot(raw: String): String? {
        fun norm(x: String) = x.uppercase().filter { it.isLetter() }
        val n = norm(raw.trim())
        return CANONICAL_SLOTS.firstOrNull { norm(it) == n }
    }

    private fun exerciseLine(e: ExerciseDto) = "id=${e.id} | ${e.name} | ${e.type ?: "STRENGTH"}${tagSuffix(e.tags)}"

    private fun workoutLine(t: WorkoutTemplateDto, tagNames: Map<Long, String> = emptyMap()): String {
        val names = t.tagIds.orEmpty().mapNotNull { tagNames[it] }
        val suffix = if (names.isNotEmpty()) " | tags: ${names.joinToString(", ")}" else ""
        return "id=${t.id} | ${t.name} | ${t.exercises.orEmpty().size} exercise(s)$suffix"
    }

    private fun tagSuffix(tags: List<TagDto>?): String {
        val names = tags.orEmpty().map { it.name }
        return if (names.isNotEmpty()) " | tags: ${names.joinToString(", ")}" else ""
    }

    private fun tagsContain(tags: List<TagDto>?, wanted: String): Boolean {
        val w = wanted.trim().lowercase()
        if (w.isEmpty()) return false
        return tags.orEmpty().any { val n = it.name.lowercase(); n == w || n.contains(w) }
    }

    /** Map tag names to ids for the given entity type, creating any that don't exist yet. */
    private fun resolveTagIds(names: List<String>, entityType: TagEntityType): List<Long> {
        if (names.isEmpty()) return emptyList()
        val existing = dietService.listTags(uid, entityType).associateBy { it.name.lowercase() }
        return names.mapNotNull { raw ->
            val nm = raw.trim()
            if (nm.isEmpty()) return@mapNotNull null
            existing[nm.lowercase()]?.id ?: dietService.createTag(nm, null, uid, entityType).id
        }
    }

    private fun parseDate(date: String?): LocalDate =
        date?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()

    private companion object {
        const val NOT_AUTHED = "Not authenticated."
        const val SCOPE_WRITE = "SCOPE_MCP_WRITE"
        const val MAX_QUANTITY = 100_000.0
        const val MAX_NAME_LEN = 100
        const val MAX_MEAL_FOODS = 50
        const val MAX_DIET_ENTRIES = 60
        val EXERCISE_TYPES = setOf("STRENGTH", "CARDIO", "TIMED")
        val VALID_SLOTS = setOf("BREAKFAST", "LUNCH", "DINNER", "MORNING_SNACK", "EVENING_SNACK")
        // Canonical meal slots used by diets/plans (mirrors DashboardService.CANONICAL_SLOTS + client MEAL_SLOTS).
        val CANONICAL_SLOTS = listOf(
            "Early Morning", "Breakfast", "Noon", "Lunch", "Evening",
            "Pre-Workout", "Post-Workout", "Dinner", "Post-Dinner",
        )
        // dayOfWeek is 0-based (Monday=0), matching DietMealDto.
        val DAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val DEFAULT_UNITS = mapOf(
            "WEIGHT" to "kg", "GLUCOSE" to "mg/dL", "STEPS" to "steps", "CALORIES_BURNED" to "kcal",
        )
    }
}
