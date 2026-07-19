package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.model.Food
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local-only data source for foods. All reads and writes go to Room; the UI never waits
 * on the network. Writes are optimistic and marked `dirty` for the [SyncManager] to push.
 *
 * The one exception is [searchOnline] — an explicit online-only read against the external
 * food database (not local data), mirroring the "online-only" case in the offline-first
 * guidance.
 */
@Singleton
class FoodRepository @Inject constructor(
    private val dao: FoodDao,
    private val api: FoodsApi,
) {
    fun getFoods(): Flow<List<Food>> = dao.getAllFoods()

    fun searchFoods(q: String): Flow<List<Food>> = dao.searchFoods(q)

    /** Create a food locally with a client-generated UUID; syncs on the next push. */
    suspend fun createManual(
        name: String,
        caloriesPer100: Double,
        proteinPer100: Double,
        carbsPer100: Double,
        fatPer100: Double,
        servingLabel: String? = null,
    ) {
        dao.upsert(
            Food(
                name = name,
                caloriesPer100 = caloriesPer100,
                proteinPer100 = proteinPer100,
                carbsPer100 = carbsPer100,
                fatPer100 = fatPer100,
                servingLabel = servingLabel,
                dirty = true,
            )
        )
    }

    suspend fun toggleFavorite(food: Food) {
        dao.upsert(
            food.copy(
                isFavorite = !food.isFavorite,
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            )
        )
    }

    /** Soft-delete: hidden from the UI now, pushed as a tombstone, removed once synced. */
    suspend fun delete(food: Food) {
        val now = System.currentTimeMillis()
        dao.upsert(food.copy(deletedAt = now, updatedAt = now, dirty = true))
    }

    // ── Online-only: external food DB search (not local data) ────────────────────
    suspend fun searchOnline(q: String): List<FoodDto> =
        api.searchFoods(q = q, page = 0, size = 30).body()?.content ?: emptyList()

    /** Save an online search result as the user's own food (fresh identity), then sync. */
    suspend fun addOnline(dto: FoodDto) {
        dao.upsert(dto.toEntity(dirty = true))
    }
}
