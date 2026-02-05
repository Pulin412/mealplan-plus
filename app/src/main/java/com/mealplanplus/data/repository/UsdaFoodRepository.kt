package com.mealplanplus.data.repository

import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.remote.UsdaFoodApi
import com.mealplanplus.data.remote.UsdaFoodItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsdaFoodRepository @Inject constructor(
    private val api: UsdaFoodApi
) {
    suspend fun searchFoods(query: String): Result<List<UsdaFoodResult>> {
        return try {
            val response = api.searchFoods(query)
            val results = response.foods.map { item ->
                UsdaFoodResult(
                    fdcId = item.fdcId,
                    name = item.description,
                    brand = item.brandOwner ?: item.brandName,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    servingSize = item.servingSize ?: 100.0,
                    servingUnit = item.servingSizeUnit ?: "g"
                )
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFoodDetails(fdcId: Int): Result<UsdaFoodResult> {
        return try {
            val item = api.getFoodDetails(fdcId)
            Result.success(
                UsdaFoodResult(
                    fdcId = item.fdcId,
                    name = item.description,
                    brand = item.brandOwner ?: item.brandName,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    servingSize = item.servingSize ?: 100.0,
                    servingUnit = item.servingSizeUnit ?: "g"
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Simplified USDA food result for UI
 */
data class UsdaFoodResult(
    val fdcId: Int,
    val name: String,
    val brand: String?,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val servingSize: Double,
    val servingUnit: String
) {
    /**
     * Convert to local FoodItem for saving
     */
    fun toFoodItem(): FoodItem {
        return FoodItem(
            name = name,
            brand = brand,
            servingSize = servingSize,
            servingUnit = servingUnit,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fat = fat
        )
    }
}
