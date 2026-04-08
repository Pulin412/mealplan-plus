package com.mealplanplus.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("api/v0/product/{barcode}.json")
    suspend fun getProductByBarcode(@Path("barcode") barcode: String): OpenFoodFactsResponse

    @GET("cgi/search.pl")
    suspend fun searchProducts(
        @Query("search_terms") query: String,
        @Query("search_simple") simple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): OpenFoodFactsSearchResponse
}

data class OpenFoodFactsResponse(
    val status: Int,
    val status_verbose: String?,
    val product: OpenFoodFactsProduct?
)

data class OpenFoodFactsSearchResponse(
    val count: Int,
    val page: Int,
    val page_size: Int,
    val products: List<OpenFoodFactsProduct>
)

data class OpenFoodFactsProduct(
    val code: String?,
    val product_name: String?,
    val brands: String?,
    val serving_size: String?,
    val nutriments: Nutriments?
)

data class Nutriments(
    // OpenFoodFacts uses a hyphen in the JSON key ("energy-kcal_100g") which
    // is not a valid Kotlin/Java identifier, so @SerializedName is required.
    @SerializedName("energy-kcal_100g")
    val energy_kcal_100g: Double?,
    // kJ fallback — some products only report energy in kJ, not kcal
    @SerializedName("energy_100g")
    val energy_kj_100g: Double? = null,
    val proteins_100g: Double?,
    val carbohydrates_100g: Double?,
    val fat_100g: Double?,
    val fiber_100g: Double?,
    val sugars_100g: Double?
) {
    /** Calories per 100 g/ml — prefers kcal field, falls back to kJ ÷ 4.184. */
    val caloriesPer100g: Double
        get() = energy_kcal_100g
            ?: energy_kj_100g?.div(4.184)
            ?: 0.0
}
