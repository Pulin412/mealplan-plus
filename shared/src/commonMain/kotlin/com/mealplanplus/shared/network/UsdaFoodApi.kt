package com.mealplanplus.shared.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * USDA FoodData Central API client
 * Docs: https://fdc.nal.usda.gov/api-guide.html
 */
class UsdaFoodApi(private val client: HttpClient) {

    companion object {
        private const val BASE_URL = "https://api.nal.usda.gov"
        // DEMO_KEY has rate limits but works for testing
        // For production, get free key at https://api.nal.usda.gov
        const val API_KEY = "DEMO_KEY"
    }

    suspend fun searchFoods(
        query: String,
        pageSize: Int = 25,
        pageNumber: Int = 1,
        apiKey: String = API_KEY
    ): UsdaSearchResponse {
        return client.get("$BASE_URL/fdc/v1/foods/search") {
            parameter("api_key", apiKey)
            parameter("query", query)
            parameter("pageSize", pageSize)
            parameter("pageNumber", pageNumber)
            parameter("dataType", "Foundation,SR Legacy,Branded")
        }.body()
    }

    suspend fun getFoodDetails(
        fdcId: Int,
        apiKey: String = API_KEY
    ): UsdaFoodItem {
        return client.get("$BASE_URL/fdc/v1/food/$fdcId") {
            parameter("api_key", apiKey)
        }.body()
    }
}

@Serializable
data class UsdaSearchResponse(
    val totalHits: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val foods: List<UsdaFoodItem> = emptyList()
)

@Serializable
data class UsdaFoodItem(
    val fdcId: Int,
    val description: String,
    val dataType: String? = null,
    val brandOwner: String? = null,
    val brandName: String? = null,
    val ingredients: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val foodNutrients: List<UsdaNutrient> = emptyList()
) {
    // Nutrient IDs
    companion object {
        private const val ENERGY_KCAL = 1008
        private const val PROTEIN = 1003
        private const val CARBOHYDRATES = 1005
        private const val FAT = 1004
        private const val FIBER = 1079
        private const val SUGARS = 2000
    }

    val calories: Double get() = getNutrientValue(ENERGY_KCAL)
    val protein: Double get() = getNutrientValue(PROTEIN)
    val carbs: Double get() = getNutrientValue(CARBOHYDRATES)
    val fat: Double get() = getNutrientValue(FAT)
    val fiber: Double get() = getNutrientValue(FIBER)
    val sugar: Double get() = getNutrientValue(SUGARS)

    private fun getNutrientValue(nutrientId: Int): Double {
        return foodNutrients.find { it.nutrientId == nutrientId }?.value ?: 0.0
    }
}

@Serializable
data class UsdaNutrient(
    val nutrientId: Int? = null,
    val nutrientName: String? = null,
    val nutrientNumber: String? = null,
    val unitName: String? = null,
    val value: Double? = null
)
