package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.model.Food
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val dao: FoodDao,
    private val api: FoodsApi,
) {

    fun getFoods(): Flow<List<Food>> = dao.getAllFoods()

    fun searchFoods(q: String): Flow<List<Food>> = dao.searchFoods(q)

    suspend fun refresh() {
        val dtos = api.listFoods(favorites = false).body() ?: return
        val entities = dtos.map { it.toEntity() }
        dao.upsertAll(entities)
    }

    /** Online search — returns generated DTOs for the caller to display or save. */
    suspend fun searchOnline(q: String): List<FoodDto> =
        api.searchFoods(q = q, page = 0, size = 30).body()?.content ?: emptyList()

    suspend fun createFood(dto: FoodDto): Food {
        val created = api.createFood(dto).body() ?: dto
        val entity = created.toEntity()
        val localId = dao.upsert(entity)
        return entity.copy(id = localId)
    }

    suspend fun deleteFood(food: Food) {
        val remoteId = food.serverId?.toLongOrNull()
        if (remoteId != null) {
            runCatching { api.deleteFood(remoteId) }
        }
        dao.delete(food)
    }

    suspend fun toggleFavorite(food: Food) {
        val newFav = !food.isFavorite
        dao.updateFavorite(food.id, newFav)
        val remoteId = food.serverId?.toLongOrNull()
        if (remoteId != null) {
            runCatching {
                val updated = api.toggleFoodFavorite(remoteId).body()
                dao.updateFavorite(food.id, updated?.isFavorite ?: newFav)
            }
        }
    }
}

private fun FoodDto.toEntity() = Food(
    serverId       = serverId?.toString() ?: id?.toString(),
    name           = name,
    brand          = brand,
    servingLabel   = null,
    caloriesPer100 = caloriesPer100,
    proteinPer100  = proteinPer100,
    carbsPer100    = carbsPer100,
    fatPer100      = fatPer100,
    gramsPerPiece  = gramsPerPiece,
    gramsPerCup    = gramsPerCup,
    gramsPerTbsp   = gramsPerTbsp,
    gramsPerTsp    = gramsPerTsp,
    glycemicIndex  = glycemicIndex,
    isFavorite     = isFavorite ?: false,
    isSystemFood   = isSystemFood ?: false,
    verified       = verified ?: false,
)
