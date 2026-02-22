package com.mealplanplus.shared.repository

import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.model.FoodItem
import com.mealplanplus.shared.model.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FoodRepository(private val database: MealPlanDatabase) {

    private val queries = database.foodItemQueries

    fun getAllFoods(): Flow<List<FoodItem>> {
        return queries.selectAll().asFlowList().map { list ->
            list.map { it.toFoodItem() }
        }
    }

    suspend fun getFoodById(id: Long): FoodItem? {
        return queries.selectById(id).executeAsOneOrNull()?.toFoodItem()
    }

    suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        return queries.selectByBarcode(barcode).executeAsOneOrNull()?.toFoodItem()
    }

    fun searchByName(query: String, limit: Long = 50): Flow<List<FoodItem>> {
        return queries.searchByName(query, limit).asFlowList().map { list ->
            list.map { it.toFoodItem() }
        }
    }

    fun getFavorites(): Flow<List<FoodItem>> {
        return queries.selectFavorites().asFlowList().map { list ->
            list.map { it.toFoodItem() }
        }
    }

    fun getRecent(limit: Long = 20): Flow<List<FoodItem>> {
        return queries.selectRecent(limit).asFlowList().map { list ->
            list.map { row ->
                FoodItem(
                    id = row.id,
                    name = row.name,
                    brand = row.brand,
                    barcode = row.barcode,
                    caloriesPer100 = row.caloriesPer100,
                    proteinPer100 = row.proteinPer100,
                    carbsPer100 = row.carbsPer100,
                    fatPer100 = row.fatPer100,
                    gramsPerPiece = row.gramsPerPiece,
                    gramsPerCup = row.gramsPerCup,
                    gramsPerTbsp = row.gramsPerTbsp,
                    gramsPerTsp = row.gramsPerTsp,
                    glycemicIndex = row.glycemicIndex?.toInt(),
                    isFavorite = row.isFavorite == 1L,
                    lastUsed = row.lastUsed,
                    createdAt = row.createdAt,
                    isSystemFood = row.isSystemFood == 1L
                )
            }
        }
    }

    suspend fun insertFood(food: FoodItem): Long {
        queries.insert(
            name = food.name,
            brand = food.brand,
            barcode = food.barcode,
            caloriesPer100 = food.caloriesPer100,
            proteinPer100 = food.proteinPer100,
            carbsPer100 = food.carbsPer100,
            fatPer100 = food.fatPer100,
            gramsPerPiece = food.gramsPerPiece,
            gramsPerCup = food.gramsPerCup,
            gramsPerTbsp = food.gramsPerTbsp,
            gramsPerTsp = food.gramsPerTsp,
            glycemicIndex = food.glycemicIndex?.toLong(),
            isFavorite = if (food.isFavorite) 1L else 0L,
            lastUsed = food.lastUsed,
            createdAt = food.createdAt,
            isSystemFood = if (food.isSystemFood) 1L else 0L
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateFood(food: FoodItem) {
        queries.update(
            name = food.name,
            brand = food.brand,
            barcode = food.barcode,
            caloriesPer100 = food.caloriesPer100,
            proteinPer100 = food.proteinPer100,
            carbsPer100 = food.carbsPer100,
            fatPer100 = food.fatPer100,
            gramsPerPiece = food.gramsPerPiece,
            gramsPerCup = food.gramsPerCup,
            gramsPerTbsp = food.gramsPerTbsp,
            gramsPerTsp = food.gramsPerTsp,
            glycemicIndex = food.glycemicIndex?.toLong(),
            isFavorite = if (food.isFavorite) 1L else 0L,
            lastUsed = food.lastUsed,
            id = food.id
        )
    }

    suspend fun setFavorite(id: Long, isFavorite: Boolean) {
        queries.updateFavorite(if (isFavorite) 1L else 0L, id)
    }

    suspend fun updateLastUsed(id: Long) {
        queries.updateLastUsed(currentTimeMillis(), id)
    }

    suspend fun deleteFood(id: Long) {
        queries.deleteById(id)
    }

    // MARK: - Snapshot functions for iOS
    suspend fun getAllFoodsSnapshot(): List<FoodItem> {
        return queries.selectAll().executeAsList().map { it.toFoodItem() }
    }

    suspend fun searchByNameSnapshot(query: String, limit: Long = 50): List<FoodItem> {
        return queries.searchByName(query, limit).executeAsList().map { it.toFoodItem() }
    }

    private fun com.mealplanplus.shared.db.Food_items.toFoodItem(): FoodItem {
        return FoodItem(
            id = id,
            name = name,
            brand = brand,
            barcode = barcode,
            caloriesPer100 = caloriesPer100,
            proteinPer100 = proteinPer100,
            carbsPer100 = carbsPer100,
            fatPer100 = fatPer100,
            gramsPerPiece = gramsPerPiece,
            gramsPerCup = gramsPerCup,
            gramsPerTbsp = gramsPerTbsp,
            gramsPerTsp = gramsPerTsp,
            glycemicIndex = glycemicIndex?.toInt(),
            isFavorite = isFavorite == 1L,
            lastUsed = lastUsed,
            createdAt = createdAt,
            isSystemFood = isSystemFood == 1L
        )
    }
}
