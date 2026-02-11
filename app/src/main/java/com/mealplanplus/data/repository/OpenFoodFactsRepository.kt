package com.mealplanplus.data.repository

import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.remote.OpenFoodFactsApi
import com.mealplanplus.data.remote.OpenFoodFactsProduct
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenFoodFactsRepository @Inject constructor(
    private val api: OpenFoodFactsApi
) {
    suspend fun getProductByBarcode(barcode: String): Result<FoodItem?> {
        return try {
            val response = api.getProductByBarcode(barcode)
            if (response.status == 1 && response.product != null) {
                Result.success(mapToFoodItem(response.product, barcode))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchProducts(query: String): Result<List<FoodItem>> {
        return try {
            val response = api.searchProducts(query)
            val foods = response.products.mapNotNull { product ->
                mapToFoodItem(product, product.code)
            }
            Result.success(foods)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToFoodItem(product: OpenFoodFactsProduct, barcode: String?): FoodItem? {
        val name = product.product_name
        if (name.isNullOrBlank()) return null

        val nutriments = product.nutriments ?: return null

        return FoodItem(
            name = name,
            brand = product.brands,
            barcode = barcode,
            caloriesPer100 = nutriments.energy_kcal_100g ?: 0.0,
            proteinPer100 = nutriments.proteins_100g ?: 0.0,
            carbsPer100 = nutriments.carbohydrates_100g ?: 0.0,
            fatPer100 = nutriments.fat_100g ?: 0.0
        )
    }
}
