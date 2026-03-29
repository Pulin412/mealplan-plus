package com.mealplanplus.data.repository

import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.remote.UsdaFoodApi
import com.mealplanplus.data.remote.UsdaFoodItem
import com.mealplanplus.util.CrashlyticsReporter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsdaFoodRepository @Inject constructor(
    private val api: UsdaFoodApi,
    private val crashlytics: CrashlyticsReporter
) {
    suspend fun searchFoods(query: String): Result<List<UsdaFoodResult>> {
        return try {
            val response = api.searchFoods(query = query)
            val foods = response.foods ?: emptyList()
            val results = foods.map { item ->
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
            crashlytics.recordNonFatal(e, context = "usda_search", extras = mapOf("query" to query))
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
            crashlytics.recordNonFatal(e, context = "usda_details", extras = mapOf("fdcId" to fdcId.toString()))
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
     * Convert to local FoodItem for saving (USDA returns per 100g by default)
     */
    fun toFoodItem(): FoodItem {
        // USDA returns macros per servingSize, normalize to per 100g
        val factor = if (servingSize > 0) 100.0 / servingSize else 1.0
        return FoodItem(
            name = name,
            brand = brand,
            caloriesPer100 = calories * factor,
            proteinPer100 = protein * factor,
            carbsPer100 = carbs * factor,
            fatPer100 = fat * factor,
            gramsPerPiece = if (servingUnit != "g" && servingUnit != "ml") servingSize else null
        )
    }
}
