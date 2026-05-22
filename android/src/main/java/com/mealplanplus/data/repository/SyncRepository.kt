package com.mealplanplus.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.local.WorkoutSessionDao
import com.mealplanplus.data.model.*
import com.mealplanplus.data.remote.*
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.CrashlyticsReporter
import com.mealplanplus.util.toEpochMs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@Singleton
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MealPlanApi,
    private val mealDao: MealDao,
    private val dietDao: DietDao,
    private val healthMetricDao: HealthMetricDao,
    private val groceryDao: GroceryDao,
    private val foodDao: FoodDao,
    private val sessionDao: WorkoutSessionDao,
    private val crashlytics: CrashlyticsReporter
) {
    private val TAG = "SyncRepository"
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    /**
     * Resolves the Firebase UID.
     *
     * Firebase Auth initialises asynchronously — currentUser can be null briefly
     * after process start even when the user is signed in. Strategy:
     *  1. currentUser if already available (fast path)
     *  2. Stored UID in AuthPreferences (set on every login, survives restarts)
     *  3. Wait up to 3s for Firebase Auth state listener to fire (async init)
     */
    private suspend fun resolveFirebaseUid(): String? {
        FirebaseAuth.getInstance().currentUser?.uid?.let { return it }
        AuthPreferences.getFirebaseUid(context).firstOrNull()?.let { return it }
        Log.d(TAG, "Firebase Auth not ready yet — waiting up to 3s for auth state")
        return withTimeoutOrNull(3_000) {
            suspendCancellableCoroutine { cont ->
                val listener = FirebaseAuth.AuthStateListener { auth ->
                    if (cont.isActive) cont.resume(auth.currentUser?.uid)
                }
                FirebaseAuth.getInstance().addAuthStateListener(listener)
                cont.invokeOnCancellation {
                    FirebaseAuth.getInstance().removeAuthStateListener(listener)
                }
            }
        }
    }

    /** Push local foods, meals, and diets to backend. */
    suspend fun push(userId: Long): Result<Int> = runCatching {
        val firebaseUid = resolveFirebaseUid()
            ?: return Result.failure(Exception("Not signed in with Firebase"))

        val now = System.currentTimeMillis()

        // Assign stable UUIDs before push — null serverId causes response matching to fail,
        // leaving syncedAt unset and re-pushing the same entity on every sync.
        val rawFoods = foodDao.getAllFoodsOnce().filter { !it.isSystemFood }.map { food ->
            if (food.serverId != null) food
            else food.copy(serverId = UUID.randomUUID().toString()).also { foodDao.updateFood(it) }
        }
        val rawMeals = mealDao.getUnsyncedMeals().map { meal ->
            if (meal.serverId != null) meal
            else meal.copy(serverId = UUID.randomUUID().toString()).also { mealDao.updateMeal(it) }
        }
        val rawDiets = dietDao.getUnsyncedDiets().map { diet ->
            if (diet.serverId != null) diet
            else diet.copy(serverId = UUID.randomUUID().toString()).also { dietDao.updateDiet(it) }
        }

        val foods = rawFoods.map { f ->
            FoodDto(serverId = f.serverId, firebaseUid = firebaseUid, name = f.name,
                brand = f.brand, barcode = f.barcode, caloriesPer100 = f.caloriesPer100,
                proteinPer100 = f.proteinPer100, carbsPer100 = f.carbsPer100,
                fatPer100 = f.fatPer100, gramsPerPiece = f.gramsPerPiece,
                gramsPerCup = f.gramsPerCup, gramsPerTbsp = f.gramsPerTbsp,
                gramsPerTsp = f.gramsPerTsp, glycemicIndex = f.glycemicIndex,
                isSystemFood = false, isFavorite = f.isFavorite, updatedAt = f.updatedAt)
        }
        val localFoodServerIdMap = rawFoods.associate { it.id to it.serverId }
        val meals = rawMeals.map { m ->
            val foodItems = mealDao.getMealFoodItems(m.id).mapNotNull { item ->
                val serverId = localFoodServerIdMap[item.foodId]
                    ?: foodDao.getFoodById(item.foodId)?.serverId
                    ?: return@mapNotNull null
                MealFoodItemDto(foodServerId = serverId, quantity = item.quantity, unit = item.unit.name, notes = item.notes)
            }
            MealDto(serverId = m.serverId, firebaseUid = firebaseUid, name = m.name,
                items = foodItems, updatedAt = m.updatedAt)
        }
        val diets = rawDiets.map { d ->
            DietDto(serverId = d.serverId, firebaseUid = firebaseUid, name = d.name,
                description = d.description, updatedAt = d.updatedAt)
        }

        val resp = api.push(SyncPushRequest(foods = foods, meals = meals, diets = diets))
        Log.d(TAG, "Push accepted=${resp.accepted} foods=${foods.size} meals=${meals.size} diets=${diets.size}")

        // Match response by serverId so any server-side dedup doesn't corrupt local records.
        val respFoodsBySid = resp.foods.associateBy { it.serverId }
        val respMealsBySid = resp.meals.associateBy { it.serverId }
        val respDietsBySid = resp.diets.associateBy { it.serverId }

        rawFoods.forEach { food ->
            val dto = respFoodsBySid[food.serverId] ?: return@forEach
            foodDao.updateFood(food.copy(serverId = dto.serverId ?: food.serverId, syncedAt = now))
        }
        rawMeals.forEach { meal ->
            val dto = respMealsBySid[meal.serverId] ?: return@forEach
            mealDao.updateMeal(meal.copy(serverId = dto.serverId ?: meal.serverId, syncedAt = now))
        }
        rawDiets.forEach { diet ->
            val dto = respDietsBySid[diet.serverId] ?: return@forEach
            dietDao.updateDiet(diet.copy(serverId = dto.serverId ?: diet.serverId, syncedAt = now))
        }

        crashlytics.log("sync_push", "accepted=${resp.accepted}")
        resp.accepted
    }.onFailure { e ->
        crashlytics.recordNonFatal(e, context = "sync_push", extras = mapOf("userId" to userId.toString()))
        Log.e(TAG, "Push failed: ${e.message}")
    }

    /**
     * Pull backend changes since [since] (epoch ms) and merge into local DB.
     * Returns the server's clock at response time so the caller can store it as
     * the next pull's `since` value.
     */
    suspend fun pull(userId: Long, since: Long): Result<Long> = runCatching {
        val firebaseUid = resolveFirebaseUid() ?: error("Not authenticated")
        val sinceIso = isoFormatter.format(Instant.ofEpochMilli(since))
        val resp = api.pull(sinceIso)
        val now = System.currentTimeMillis()

        // ── Upsert changed foods ──────────────────────────────────────────────
        resp.foods.forEach { dto ->
            val existing = dto.serverId?.let { foodDao.getFoodByServerId(it) }
            if (existing == null) {
                foodDao.insertFood(FoodItem(name = dto.name, brand = dto.brand, barcode = dto.barcode,
                    caloriesPer100 = dto.caloriesPer100, proteinPer100 = dto.proteinPer100,
                    carbsPer100 = dto.carbsPer100, fatPer100 = dto.fatPer100,
                    gramsPerPiece = dto.gramsPerPiece, gramsPerCup = dto.gramsPerCup,
                    gramsPerTbsp = dto.gramsPerTbsp, gramsPerTsp = dto.gramsPerTsp,
                    glycemicIndex = dto.glycemicIndex, isSystemFood = dto.isSystemFood,
                    isFavorite = dto.isFavorite, serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                foodDao.updateFood(existing.copy(name = dto.name, brand = dto.brand,
                    barcode = dto.barcode, caloriesPer100 = dto.caloriesPer100,
                    proteinPer100 = dto.proteinPer100, carbsPer100 = dto.carbsPer100,
                    fatPer100 = dto.fatPer100, updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        // ── Upsert changed meals ──────────────────────────────────────────────
        resp.meals.forEach { dto ->
            val existing = dto.serverId?.let { mealDao.getMealByServerId(it) }
            if (existing == null) {
                mealDao.insertMeal(Meal(name = dto.name,
                    serverId = dto.serverId, updatedAt = dto.updatedAt ?: now, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                mealDao.updateMeal(existing.copy(name = dto.name,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        // ── Upsert changed diets ─────────────────────────────────────────────
        resp.diets.forEach { dto ->
            val existing = dto.serverId?.let { dietDao.getDietByServerId(it) }
            if (existing == null) {
                dietDao.insertDiet(Diet(name = dto.name,
                    description = dto.description, serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                dietDao.updateDiet(existing.copy(name = dto.name, description = dto.description,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        // ── Upsert changed health metrics ────────────────────────────────────
        resp.healthMetrics.forEach { dto ->
            val existing = dto.serverId?.let { healthMetricDao.getMetricByServerId(it) }
            if (existing == null) {
                healthMetricDao.insertHealthMetric(HealthMetric(userId = userId,
                    date = Instant.ofEpochMilli(dto.recordedAt ?: now)
                        .atOffset(ZoneOffset.UTC).toLocalDate().toEpochMs(),
                    metricType = dto.type, subType = dto.subType, value = dto.value,
                    secondaryValue = dto.secondaryValue, serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                healthMetricDao.updateHealthMetric(existing.copy(value = dto.value,
                    secondaryValue = dto.secondaryValue, subType = dto.subType,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        // ── Upsert changed grocery lists ─────────────────────────────────────
        resp.groceryLists.forEach { dto ->
            val existing = dto.serverId?.let { groceryDao.getGroceryListByServerId(it) }
            if (existing == null) {
                groceryDao.insertGroceryList(GroceryList(userId = userId, name = dto.name,
                    serverId = dto.serverId, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                groceryDao.updateGroceryList(existing.copy(name = dto.name,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        // ── Apply tombstones (server-side deletes) ───────────────────────────
        // ── Workout sessions ──────────────────────────────────────────────────
        resp.workoutSessions.forEach { dto ->
            val dateEpoch = try {
                LocalDate.parse(dto.date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } catch (_: Exception) { 0L }
            val existing = dto.serverId?.let { sessionDao.getByServerId(it) }
            if (existing == null) {
                sessionDao.upsert(WorkoutSession(
                    userId = firebaseUid, name = dto.name, date = dateEpoch,
                    durationMinutes = dto.durationMinutes, notes = dto.notes,
                    isCompleted = dto.isCompleted, serverId = dto.serverId,
                    syncedAt = now, updatedAt = dto.updatedAt ?: now
                ))
            } else if ((dto.updatedAt ?: 0L) > existing.updatedAt) {
                sessionDao.update(existing.copy(
                    name = dto.name, date = dateEpoch, durationMinutes = dto.durationMinutes,
                    notes = dto.notes, isCompleted = dto.isCompleted,
                    syncedAt = now, updatedAt = dto.updatedAt ?: now
                ))
            }
        }

        resp.tombstones.forEach { t ->
            when (t.entityType) {
                "food" -> foodDao.getFoodByServerId(t.serverId)
                    ?.let { foodDao.deleteFood(it) }
                "meal" -> mealDao.getMealByServerId(t.serverId)
                    ?.let { mealDao.deleteMeal(it) }
                "diet" -> dietDao.getDietByServerId(t.serverId)
                    ?.let { dietDao.deleteDiet(it) }
                "health_metric" -> healthMetricDao.getMetricByServerId(t.serverId)
                    ?.let { healthMetricDao.deleteMetric(it) }
                "grocery_list" -> groceryDao.getGroceryListByServerId(t.serverId)
                    ?.let { groceryDao.deleteList(it) }
                "workout_session" -> sessionDao.getByServerId(t.serverId)
                    ?.let { sessionDao.delete(it) }
                // TODO(CR-02): "daily_log" tombstone requires adding a serverId column to
                //  the DailyLog Room entity (currently uses composite PK userId+date).
                //  Track under a Room schema migration before implementing.
            }
        }

        Log.d(TAG, "Pull complete: meals=${resp.meals.size} diets=${resp.diets.size} " +
            "metrics=${resp.healthMetrics.size} lists=${resp.groceryLists.size} " +
            "tombstones=${resp.tombstones.size}")
        crashlytics.log("sync_pull", "meals=${resp.meals.size} diets=${resp.diets.size} " +
            "metrics=${resp.healthMetrics.size} tombstones=${resp.tombstones.size}")

        resp.serverTime ?: now
    }.onFailure { e ->
        crashlytics.recordNonFatal(e, context = "sync_pull", extras = mapOf("userId" to userId.toString()))
        Log.e(TAG, "Pull failed: ${e.message}")
    }
}
