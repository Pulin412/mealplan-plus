package com.mealplanplus.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * USDA FoodData Central API
 * Docs: https://fdc.nal.usda.gov/api-guide.html
 * No API key required for basic access
 */
interface UsdaFoodApi {

    companion object {
        // DEMO_KEY has rate limits but works for testing
        // For production, get free key at https://api.nal.usda.gov
        const val API_KEY = "DEMO_KEY"
    }

    @GET("fdc/v1/foods/search")
    suspend fun searchFoods(
        @Query("api_key") apiKey: String = API_KEY,
        @Query("query") query: String,
        @Query("pageSize") pageSize: Int = 25,
        @Query("pageNumber") pageNumber: Int = 1,
        @Query("dataType") dataType: String = "Foundation,SR Legacy,Branded"
    ): UsdaSearchResponse

    @GET("fdc/v1/food/{fdcId}")
    suspend fun getFoodDetails(
        @Path("fdcId") fdcId: Int,
        @Query("api_key") apiKey: String = API_KEY
    ): UsdaFoodItem
}

data class UsdaSearchResponse(
    val totalHits: Int,
    val currentPage: Int,
    val totalPages: Int,
    val foods: List<UsdaFoodItem>
)

data class UsdaFoodItem(
    val fdcId: Int,
    val description: String,
    val dataType: String?,
    val brandOwner: String?,
    val brandName: String?,
    val ingredients: String?,
    val servingSize: Double?,
    val servingSizeUnit: String?,
    val foodNutrients: List<UsdaNutrient>
) {
    // Extract common nutrients
    val calories: Double get() = getNutrientValue(1008) // Energy (kcal)
    val protein: Double get() = getNutrientValue(1003) // Protein
    val carbs: Double get() = getNutrientValue(1005) // Carbohydrates
    val fat: Double get() = getNutrientValue(1004) // Total fat
    val fiber: Double get() = getNutrientValue(1079) // Fiber
    val sugar: Double get() = getNutrientValue(2000) // Sugars

    private fun getNutrientValue(nutrientId: Int): Double {
        return foodNutrients.find { it.nutrientId == nutrientId }?.value ?: 0.0
    }
}

data class UsdaNutrient(
    val nutrientId: Int,
    val nutrientName: String,
    val nutrientNumber: String?,
    val unitName: String,
    val value: Double
)
