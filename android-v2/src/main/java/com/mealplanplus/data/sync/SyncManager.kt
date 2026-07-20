package com.mealplanplus.data.sync

import com.mealplanplus.data.generated.api.SyncApi
import com.mealplanplus.data.generated.model.SyncPushRequest
import com.mealplanplus.data.generated.model.TombstoneDto
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.local.dao.MealDao
import com.mealplanplus.data.repository.toDto
import com.mealplanplus.data.repository.toEntity
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The sole network boundary for domain data. Repositories only touch Room; this pushes
 * local (dirty) changes up and pulls server changes down via the backend's delta-sync
 * endpoints. Covers Foods + Meals; more entity types plug into the same push/pull.
 */
@Singleton
class SyncManager @Inject constructor(
    private val syncApi: SyncApi,
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val cursor: SyncCursorStore,
) {
    /** Push dirty rows, then pull changes since the cursor. Best-effort; safe to call often. */
    suspend fun sync(): Result<Unit> = runCatching {
        push()
        pull()
    }

    private suspend fun push() {
        val dirtyFoods = foodDao.getDirty()
        val dirtyMeals = mealDao.getDirty()
        if (dirtyFoods.isEmpty() && dirtyMeals.isEmpty()) return

        val (delFoods, liveFoods) = dirtyFoods.partition { it.deletedAt != null }
        val (delMeals, liveMeals) = dirtyMeals.partition { it.deletedAt != null }

        val tombstones =
            delFoods.map { TombstoneDto("food", UUID.fromString(it.id), Instant.ofEpochMilli(it.deletedAt!!)) } +
            delMeals.map { TombstoneDto("meal", UUID.fromString(it.id), Instant.ofEpochMilli(it.deletedAt!!)) }

        val resp = syncApi.syncPush(
            SyncPushRequest(
                foods = liveFoods.map { it.toDto() },
                meals = liveMeals.map { it.toDto() },
                tombstones = tombstones,
            )
        )
        if (!resp.isSuccessful) return

        foodDao.clearDirty(liveFoods.map { it.id }); foodDao.hardDelete(delFoods.map { it.id })
        mealDao.clearDirty(liveMeals.map { it.id }); mealDao.hardDelete(delMeals.map { it.id })
    }

    private suspend fun pull() {
        val resp = syncApi.syncPull(cursor.get()).body() ?: return

        // Upsert incoming — but never clobber un-pushed local edits (still dirty).
        val dirtyFoodIds = foodDao.dirtyIds().toSet()
        val foods = resp.foods.filter { it.serverId?.toString() !in dirtyFoodIds }.map { it.toEntity() }
        if (foods.isNotEmpty()) foodDao.upsertAll(foods)

        val dirtyMealIds = mealDao.dirtyIds().toSet()
        val meals = resp.meals.filter { it.serverId?.toString() !in dirtyMealIds }.map { it.toEntity() }
        if (meals.isNotEmpty()) mealDao.upsertAll(meals)

        // Apply server-side deletes.
        resp.tombstones.filter { it.entityType == "food" }.map { it.serverId.toString() }
            .takeIf { it.isNotEmpty() }?.let { foodDao.hardDelete(it) }
        resp.tombstones.filter { it.entityType == "meal" }.map { it.serverId.toString() }
            .takeIf { it.isNotEmpty() }?.let { mealDao.hardDelete(it) }

        cursor.set(resp.serverTime)
    }
}
