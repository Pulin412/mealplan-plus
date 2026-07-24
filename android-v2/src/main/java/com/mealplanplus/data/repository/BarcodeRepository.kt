package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.model.FoodDto
import com.mealplanplus.data.generated.model.FoodUnit
import com.mealplanplus.data.remote.OpenFoodFactsApi
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves a scanned barcode into a [FoodDto] via Open Food Facts. Returns null when the product is
 * unknown or has no usable name. The result is a plain [FoodDto] so the scan flow can reuse the same
 * add path as online search ([FoodRepository.addOnline]). Macros default to 0 when OFF lacks them —
 * the user can edit after adding.
 */
@Singleton
class BarcodeRepository @Inject constructor(
    private val api: OpenFoodFactsApi,
) {
    suspend fun lookup(barcode: String): FoodDto? {
        val response = runCatching { api.getProduct(barcode) }.getOrNull() ?: return null
        val product = response.body()?.takeIf { it.status == 1 }?.product ?: return null
        val name = product.productName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val n = product.nutriments
        return FoodDto(
            name = name,
            caloriesPer100 = n?.energyKcal100g ?: 0.0,
            proteinPer100 = n?.proteins100g ?: 0.0,
            carbsPer100 = n?.carbohydrates100g ?: 0.0,
            fatPer100 = n?.fat100g ?: 0.0,
            brand = product.brands?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() },
            barcode = barcode,
            unit = FoodUnit.GRAM,
        )
    }
}
