package com.mealplanplus.data.model

/**
 * Full offline backup of all user-owned data from Room.
 *
 * Flat lists of every table make this easy to serialize with Gson and
 * compress with GZIP (JSON compresses ~85% in practice).
 *
 * [version] lets future releases detect and migrate older backups.
 *
 * System foods and system exercises are excluded — they are re-seeded
 * from bundled assets on every install, so backing them up wastes space.
 */
data class LocalBackupSnapshot(
    val version: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),

    // ── Foods (custom / user-added only) ─────────────────────────────────────
    val customFoods: List<FoodItem> = emptyList(),

    // ── Meals ─────────────────────────────────────────────────────────────────
    val meals: List<Meal> = emptyList(),
    val mealFoodItems: List<MealFoodItem> = emptyList(),

    // ── Diets ─────────────────────────────────────────────────────────────────
    val diets: List<Diet> = emptyList(),
    val dietMeals: List<DietMeal> = emptyList(),         // diet_slots table

    // ── Day planning ──────────────────────────────────────────────────────────
    val plans: List<Plan> = emptyList(),                 // which diet on which date
    val plannedSlots: List<PlannedSlot> = emptyList(),   // per-date slot overrides
    val plannedSlotFoods: List<PlannedSlotFood> = emptyList(),

    // ── Food logging ──────────────────────────────────────────────────────────
    val dailyLogs: List<DailyLog> = emptyList(),
    val loggedFoods: List<LoggedFood> = emptyList(),

    // ── Health metrics ────────────────────────────────────────────────────────
    val healthMetrics: List<HealthMetric> = emptyList(),

    // ── Grocery ───────────────────────────────────────────────────────────────
    val groceryLists: List<GroceryList> = emptyList(),
    val groceryItems: List<GroceryItem> = emptyList(),

    // ── Exercises (custom only) ───────────────────────────────────────────────
    val customExercises: List<Exercise> = emptyList(),

    // ── Workout templates ─────────────────────────────────────────────────────
    val workoutTemplates: List<WorkoutTemplate> = emptyList(),
    val workoutTemplateExercises: List<WorkoutTemplateExercise> = emptyList(),
    val workoutTemplateSets: List<WorkoutTemplateSet> = emptyList(),

    // ── Workout logs ──────────────────────────────────────────────────────────
    val workoutSessions: List<WorkoutSession> = emptyList(),
    val workoutSets: List<WorkoutSet> = emptyList(),

    // ── Planned workouts ──────────────────────────────────────────────────────
    val plannedWorkouts: List<PlannedWorkout> = emptyList()
) {
    /** Human-readable one-liner shown in success messages. */
    fun summary(): String = buildString {
        append("${meals.size} meals")
        append(", ${diets.size} diets")
        append(", ${dailyLogs.size} log days")
        append(", ${healthMetrics.size} metrics")
        append(", ${workoutSessions.size} workouts")
        append(", ${groceryLists.size} lists")
        if (customFoods.isNotEmpty()) append(", ${customFoods.size} custom foods")
        if (customExercises.isNotEmpty()) append(", ${customExercises.size} custom exercises")
    }
}
