package com.mealplanplus.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.data.local.*
import com.mealplanplus.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and restores a [LocalBackupSnapshot] directly from/to Room.
 * No network calls — works 100% offline.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val dietDao: DietDao,
    private val dailyLogDao: DailyLogDao,
    private val planDao: PlanDao,
    private val plannedSlotDao: PlannedSlotDao,
    private val healthMetricDao: HealthMetricDao,
    private val groceryDao: GroceryDao,
    private val exerciseDao: ExerciseDao,
    private val workoutTemplateDao: WorkoutTemplateDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutSetDao: WorkoutSetDao,
    private val plannedWorkoutDao: PlannedWorkoutDao
) {

    /** Reads ALL user data from Room. [userId] is the local DB Long id; [firebaseUid] is the Firebase UID string. */
    suspend fun buildSnapshot(userId: Long, firebaseUid: String): LocalBackupSnapshot =
        withContext(Dispatchers.IO) {

            // Foods — custom only; system foods are re-seeded from assets on install
            val customFoods = foodDao.getAllFoodsOnce().filter { !it.isSystemFood }

            // Meals
            val meals = mealDao.getAllMeals().first()
            val mealFoodItems = meals.flatMap { mealDao.getMealFoodItems(it.id) }

            // Diets
            val diets = dietDao.getAllDietsOnce()
            val dietMeals = diets.flatMap { dietDao.getDietMeals(it.id) }

            // Day planning
            val plans = planDao.getPlansByUser(userId).first()
            val plannedSlots = plannedSlotDao.getAllSlotsOnce(userId)
            val plannedSlotFoods = plannedSlotDao.getAllSlotFoodsForUser(userId)

            // Food logging
            val dailyLogs = dailyLogDao.getAllLogsOnce(userId)
            val loggedFoods = dailyLogDao.getAllLoggedFoodsOnce(userId)

            // Health metrics
            val healthMetrics = healthMetricDao.getAllMetrics(userId)

            // Grocery
            val groceryLists = groceryDao.getListsByUser(userId).first()
            val groceryItems = groceryLists.flatMap { groceryDao.getItemsByList(it.id).first() }

            // Exercises — custom only
            val customExercises = exerciseDao.getCustomExercisesOnce(firebaseUid)

            // Workout templates (user-created only)
            val workoutTemplates = workoutTemplateDao.getAllTemplatesOnce(firebaseUid)
            val workoutTemplateExercises = workoutTemplateDao.getAllTemplateExercisesOnce()
            val workoutTemplateSets = workoutTemplateDao.getAllTemplateSetsOnce()

            // Workout logs
            val workoutSessions = workoutSessionDao.getAllSessionsOnce(firebaseUid)
            val workoutSets = workoutSetDao.getAllSetsOnce()

            // Planned workouts
            val plannedWorkouts = plannedWorkoutDao.getAllPlannedOnce(firebaseUid)

            LocalBackupSnapshot(
                customFoods = customFoods,
                meals = meals,
                mealFoodItems = mealFoodItems,
                diets = diets,
                dietMeals = dietMeals,
                plans = plans,
                plannedSlots = plannedSlots,
                plannedSlotFoods = plannedSlotFoods,
                dailyLogs = dailyLogs,
                loggedFoods = loggedFoods,
                healthMetrics = healthMetrics,
                groceryLists = groceryLists,
                groceryItems = groceryItems,
                customExercises = customExercises,
                workoutTemplates = workoutTemplates,
                workoutTemplateExercises = workoutTemplateExercises,
                workoutTemplateSets = workoutTemplateSets,
                workoutSessions = workoutSessions,
                workoutSets = workoutSets,
                plannedWorkouts = plannedWorkouts
            )
        }

    /**
     * Upserts every record from [snapshot] into Room.
     * Existing records with the same PK are overwritten (REPLACE strategy throughout).
     * [userId] / [firebaseUid] replace whatever userId was in the backup so the data
     * is always owned by the currently logged-in user on this device.
     */
    suspend fun restoreSnapshot(
        userId: Long,
        firebaseUid: String,
        snapshot: LocalBackupSnapshot
    ) = withContext(Dispatchers.IO) {

        // Custom foods
        snapshot.customFoods.forEach { foodDao.insertFood(it) }

        // Meals + food items
        snapshot.meals.forEach { mealDao.insertMeal(it.copy(userId = userId)) }
        snapshot.mealFoodItems.forEach { mealDao.insertMealFoodItem(it) }

        // Diets + slots
        snapshot.diets.forEach { dietDao.insertDiet(it.copy(userId = userId)) }
        snapshot.dietMeals.forEach { dietDao.insertDietMeal(it) }

        // Day planning
        snapshot.plans.forEach { planDao.upsertPlan(it.copy(userId = userId)) }
        snapshot.plannedSlots.forEach {
            plannedSlotDao.upsertSlot(it.copy(userId = userId))
        }
        snapshot.plannedSlotFoods.forEach { plannedSlotDao.upsertSlotFood(it) }

        // Food logging
        snapshot.dailyLogs.forEach { dailyLogDao.insertLog(it.copy(userId = userId)) }
        snapshot.loggedFoods.forEach { dailyLogDao.insertLoggedFood(it.copy(userId = userId)) }

        // Health metrics
        snapshot.healthMetrics.forEach {
            healthMetricDao.insertHealthMetric(it.copy(userId = userId))
        }

        // Grocery
        snapshot.groceryLists.forEach {
            groceryDao.insertGroceryList(it.copy(userId = userId))
        }
        snapshot.groceryItems.forEach { groceryDao.upsertItem(it) }

        // Custom exercises
        snapshot.customExercises.forEach {
            exerciseDao.upsertAll(listOf(it.copy(userId = firebaseUid)))
        }

        // Workout templates
        snapshot.workoutTemplates.forEach {
            workoutTemplateDao.upsertTemplate(it.copy(userId = firebaseUid))
        }
        workoutTemplateDao.upsertTemplateExercises(snapshot.workoutTemplateExercises)
        workoutTemplateDao.insertTemplateSets(snapshot.workoutTemplateSets)

        // Workout sessions + sets
        snapshot.workoutSessions.forEach {
            workoutSessionDao.upsert(it.copy(userId = firebaseUid))
        }
        snapshot.workoutSets.forEach { workoutSetDao.upsert(it) }

        // Planned workouts
        snapshot.plannedWorkouts.forEach {
            plannedWorkoutDao.plan(it.copy(userId = firebaseUid))
        }
    }
}
