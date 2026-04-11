package com.mealplanplus.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.model.*
import com.mealplanplus.data.remote.*
import com.mealplanplus.util.CrashlyticsReporter
import com.mealplanplus.util.toEpochMs
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val api: MealPlanApi,
    private val mealDao: MealDao,
    private val dietDao: DietDao,
    private val healthMetricDao: HealthMetricDao,
    private val groceryDao: GroceryDao,
    private val crashlytics: CrashlyticsReporter
) {
    private val TAG = "SyncRepository"
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    /** Push local changes (updatedAt > syncedAt OR syncedAt == null) to backend */
    suspend fun push(userId: Long): Result<Int> = runCatching {
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(Exception("Not signed in with Firebase"))

        val now = System.currentTimeMillis()

        val meals = mealDao.getUnsyncedMeals(userId).map { m ->
            MealDto(serverId = m.serverId, firebaseUid = firebaseUid, name = m.name,
                slot = m.slotType, updatedAt = m.updatedAt)
        }

        val diets = dietDao.getUnsyncedDiets(userId).map { d ->
            DietDto(serverId = d.serverId, firebaseUid = firebaseUid, name = d.name,
                description = d.description, updatedAt = d.updatedAt)
        }

        val metrics = healthMetricDao.getUnsyncedMetrics(userId).map { h ->
            HealthMetricDto(serverId = h.serverId, firebaseUid = firebaseUid,
                type = h.metricType ?: "CUSTOM", subType = h.subType, value = h.value,
                secondaryValue = h.secondaryValue, unit = h.metricType ?: "",
                recordedAt = h.timestamp, updatedAt = h.updatedAt)
        }

        val groceryLists = groceryDao.getUnsyncedGroceryLists(userId).map { g ->
            GroceryListDto(serverId = g.serverId, firebaseUid = firebaseUid,
                name = g.name, updatedAt = g.updatedAt)
        }

        val req = SyncPushRequest(meals = meals, diets = diets,
            healthMetrics = metrics, groceryLists = groceryLists)

        val totalItems = meals.size + diets.size + metrics.size + groceryLists.size
        if (totalItems == 0) return Result.success(0)

        val resp = api.push(req)
        Log.d(TAG, "Push accepted=${resp.accepted}")

        // Mark as synced
        mealDao.getUnsyncedMeals(userId).forEach { m ->
            mealDao.updateMeal(m.copy(syncedAt = now))
        }
        dietDao.getUnsyncedDiets(userId).forEach { d ->
            dietDao.updateDiet(d.copy(syncedAt = now))
        }
        healthMetricDao.getUnsyncedMetrics(userId).forEach { h ->
            healthMetricDao.updateHealthMetric(h.copy(syncedAt = now))
        }
        groceryDao.getUnsyncedGroceryLists(userId).forEach { g ->
            groceryDao.updateGroceryList(g.copy(syncedAt = now))
        }

        crashlytics.log("sync_push", "accepted=${resp.accepted}")
        resp.accepted
    }.onFailure { e ->
        crashlytics.recordNonFatal(e, context = "sync_push", extras = mapOf("userId" to userId.toString()))
        Log.e(TAG, "Push failed: ${e.message}")
    }

    /** Pull backend changes since last pull and merge into local DB */
    suspend fun pull(userId: Long, since: Long): Result<Unit> = runCatching {
        val firebaseUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.failure(Exception("Not signed in with Firebase"))

        val sinceIso = isoFormatter.format(Instant.ofEpochMilli(since))
        val resp = api.pull(sinceIso)
        val now = System.currentTimeMillis()

        resp.meals.forEach { dto ->
            val existing = dto.serverId?.let { mealDao.getMealByServerId(it) }
            if (existing == null) {
                mealDao.insertMeal(Meal(userId = userId, name = dto.name, slotType = dto.slot,
                    serverId = dto.serverId, updatedAt = dto.updatedAt ?: now, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) >= existing.updatedAt) {
                mealDao.updateMeal(existing.copy(name = dto.name, slotType = dto.slot,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        resp.diets.forEach { dto ->
            val existing = dto.serverId?.let { dietDao.getDietByServerId(it) }
            if (existing == null) {
                dietDao.insertDiet(Diet(userId = userId, name = dto.name,
                    description = dto.description, serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) >= existing.updatedAt) {
                dietDao.updateDiet(existing.copy(name = dto.name, description = dto.description,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        resp.healthMetrics.forEach { dto ->
            val existing = dto.serverId?.let { healthMetricDao.getMetricByServerId(it) }
            if (existing == null) {
                healthMetricDao.insertHealthMetric(HealthMetric(userId = userId,
                    date = Instant.ofEpochMilli(dto.recordedAt ?: now)
                        .atOffset(ZoneOffset.UTC).toLocalDate().toEpochMs(),
                    metricType = dto.type, subType = dto.subType, value = dto.value,
                    secondaryValue = dto.secondaryValue, serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) >= existing.updatedAt) {
                healthMetricDao.updateHealthMetric(existing.copy(value = dto.value,
                    secondaryValue = dto.secondaryValue, subType = dto.subType,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        resp.groceryLists.forEach { dto ->
            val existing = dto.serverId?.let { groceryDao.getGroceryListByServerId(it) }
            if (existing == null) {
                groceryDao.insertGroceryList(GroceryList(userId = userId, name = dto.name,
                    serverId = dto.serverId, syncedAt = now))
            } else if ((dto.updatedAt ?: 0L) >= existing.updatedAt) {
                groceryDao.updateGroceryList(existing.copy(name = dto.name,
                    updatedAt = dto.updatedAt ?: now, syncedAt = now))
            }
        }

        Log.d(TAG, "Pull complete: meals=${resp.meals.size} diets=${resp.diets.size} " +
            "metrics=${resp.healthMetrics.size} lists=${resp.groceryLists.size}")
        crashlytics.log("sync_pull", "meals=${resp.meals.size} diets=${resp.diets.size} " +
            "metrics=${resp.healthMetrics.size} lists=${resp.groceryLists.size}")
    }.onFailure { e ->
        crashlytics.recordNonFatal(e, context = "sync_pull", extras = mapOf("userId" to userId.toString()))
        Log.e(TAG, "Pull failed: ${e.message}")
    }
}
