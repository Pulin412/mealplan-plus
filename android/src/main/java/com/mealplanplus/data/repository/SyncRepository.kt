package com.mealplanplus.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.data.local.DailyLogDao
import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.local.WorkoutSessionDao
import com.mealplanplus.data.local.WorkoutSetDao
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
    private val dailyLogDao: DailyLogDao,
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao,
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

    /** Push local changes to backend. Two-step: foods+meals+diets+metrics+groceries, then daily logs. */
    suspend fun push(userId: Long): Result<Int> = runCatching {
        val firebaseUid = resolveFirebaseUid()
            ?: return Result.failure(Exception("Not signed in with Firebase"))

        val now = System.currentTimeMillis()

        // ── Step 1: foods + meals + diets + metrics + groceries ───────────────
        // Assign stable UUIDs to any entity that never got one (null serverId
        // causes the response-matching step below to silently skip, leaving
        // syncedAt unset and creating a fresh duplicate on every push).
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
        val rawMetrics = healthMetricDao.getUnsyncedMetrics(userId).map { metric ->
            if (metric.serverId != null) metric
            else metric.copy(serverId = UUID.randomUUID().toString()).also { healthMetricDao.updateHealthMetric(it) }
        }
        val rawGroceries = groceryDao.getUnsyncedGroceryLists(userId).map { grocery ->
            if (grocery.serverId != null) grocery
            else grocery.copy(serverId = UUID.randomUUID().toString()).also { groceryDao.updateGroceryList(it) }
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
        val meals = rawMeals.map { m ->
            MealDto(serverId = m.serverId, firebaseUid = firebaseUid, name = m.name,
                updatedAt = m.updatedAt)
        }
        val diets = rawDiets.map { d ->
            DietDto(serverId = d.serverId, firebaseUid = firebaseUid, name = d.name,
                description = d.description, updatedAt = d.updatedAt)
        }
        val metrics = rawMetrics.map { h ->
            HealthMetricDto(serverId = h.serverId, firebaseUid = firebaseUid,
                type = h.metricType ?: "CUSTOM", subType = h.subType, value = h.value,
                secondaryValue = h.secondaryValue, unit = h.metricType ?: "",
                recordedAt = h.timestamp, updatedAt = h.updatedAt)
        }
        val groceryLists = rawGroceries.map { g ->
            GroceryListDto(serverId = g.serverId, firebaseUid = firebaseUid,
                name = g.name, updatedAt = g.updatedAt)
        }

        // ── Workout sessions ──────────────────────────────────────────────────
        val rawSessions = sessionDao.getUnsyncedSessions(firebaseUid).map { session ->
            if (session.serverId != null) session
            else session.copy(serverId = UUID.randomUUID().toString()).also { sessionDao.update(it) }
        }
        val allSets = setDao.getAllSetsOnce()
        val setsBySessionId = allSets.groupBy { it.sessionId }
        val sessions = rawSessions.map { s ->
            val dateStr = java.time.Instant.ofEpochMilli(s.date)
                .atOffset(ZoneOffset.UTC).toLocalDate().toString()
            WorkoutSessionSyncDto(
                serverId = s.serverId, firebaseUid = firebaseUid,
                name = s.name, date = dateStr, durationMinutes = s.durationMinutes,
                notes = s.notes, isCompleted = s.isCompleted,
                sets = (setsBySessionId[s.id] ?: emptyList()).map { ws ->
                    WorkoutSetSyncDto(exerciseId = ws.exerciseId, setNumber = ws.setNumber,
                        reps = ws.reps, weightKg = ws.weightKg,
                        durationSeconds = ws.durationSeconds, distanceMeters = ws.distanceMeters,
                        notes = ws.notes)
                },
                updatedAt = s.updatedAt
            )
        }

        val resp = api.push(SyncPushRequest(foods = foods, meals = meals, diets = diets,
            healthMetrics = metrics, groceryLists = groceryLists, workoutSessions = sessions))
        Log.d(TAG, "Push step1 accepted=${resp.accepted}")

        // Write backend-assigned serverIds back to local rows.
        // Match by serverId (stable cross-device identity) rather than array index,
        // so reordering or deduplication on the server side never writes the wrong serverId.
        val respFoodsBySid    = resp.foods.associateBy { it.serverId }
        val respMealsBySid    = resp.meals.associateBy { it.serverId }
        val respDietsBySid    = resp.diets.associateBy { it.serverId }
        val respMetricsBySid  = resp.healthMetrics.associateBy { it.serverId }
        val respGrocBySid     = resp.groceryLists.associateBy { it.serverId }
        val respSessionsBySid = resp.workoutSessions.associateBy { it.serverId }

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
        rawMetrics.forEach { metric ->
            val dto = respMetricsBySid[metric.serverId] ?: return@forEach
            healthMetricDao.updateHealthMetric(metric.copy(serverId = dto.serverId ?: metric.serverId, syncedAt = now))
        }
        rawGroceries.forEach { grocery ->
            val dto = respGrocBySid[grocery.serverId] ?: return@forEach
            groceryDao.updateGroceryList(grocery.copy(serverId = dto.serverId ?: grocery.serverId, syncedAt = now))
        }
        rawSessions.forEach { session ->
            val dto = respSessionsBySid[session.serverId] ?: return@forEach
            sessionDao.update(session.copy(serverId = dto.serverId ?: session.serverId, syncedAt = now))
        }

        // ── Step 2: daily logs (needs backend food IDs from step 1) ──────────
        // Build localFoodId → backendFoodId map via serverId
        val serverIdToBackendFoodId: Map<String, Long> = resp.foods
            .mapNotNull { dto -> dto.serverId?.let { it to dto.id } }.toMap()
        val localFoodIdToBackendId: Map<Long, Long> = rawFoods.mapNotNull { food ->
            val sid = food.serverId ?: return@mapNotNull null
            val bid = serverIdToBackendFoodId[sid] ?: return@mapNotNull null
            food.id to bid
        }.toMap()

        val allLogs = dailyLogDao.getAllLogsOnce(userId)
        val allLoggedFoods = dailyLogDao.getAllLoggedFoodsOnce(userId)
        val foodsByDate = allLoggedFoods.groupBy { it.logDate }

        val dailyLogDtos = allLogs.mapNotNull { log ->
            val loggedFoodDtos = (foodsByDate[log.date] ?: emptyList()).mapNotNull { lf ->
                val backendFoodId = localFoodIdToBackendId[lf.foodId] ?: return@mapNotNull null
                LoggedFoodDto(foodId = backendFoodId, mealSlot = lf.slotType,
                    quantity = lf.quantity, unit = lf.unit.name)
            }
            if (loggedFoodDtos.isEmpty()) return@mapNotNull null
            val dateStr = java.time.Instant.ofEpochMilli(log.date)
                .atOffset(java.time.ZoneOffset.UTC).toLocalDate().toString()
            DailyLogDto(firebaseUid = firebaseUid, date = dateStr,
                notes = log.notes, loggedFoods = loggedFoodDtos, updatedAt = log.createdAt)
        }

        var totalAccepted = resp.accepted
        var step2Error: Throwable? = null

        if (dailyLogDtos.isNotEmpty()) {
            runCatching {
                val resp2 = api.push(SyncPushRequest(dailyLogs = dailyLogDtos))
                Log.d(TAG, "Push step2 (daily logs) accepted=${resp2.accepted}")
                totalAccepted += resp2.accepted
            }.onFailure { e ->
                Log.w(TAG, "Daily log push failed (will retry next sync): ${e.message}")
                step2Error = e
            }
        }

        crashlytics.log("sync_push", "accepted=$totalAccepted step2Error=${step2Error?.message}")

        // Surface partial failure: caller (SyncWorker) can distinguish
        // step-2-only failures from full push failures and set a PARTIAL retry.
        if (step2Error != null && totalAccepted == 0) {
            throw SyncPartialFailureException(
                "Daily log push failed after step 1 succeeded",
                step2Error!!
            )
        }
        totalAccepted
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
