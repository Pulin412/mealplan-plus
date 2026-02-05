package com.mealplanplus.data.remote

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
    val energy_kcal_100g: Double?,
    val proteins_100g: Double?,
    val carbohydrates_100g: Double?,
    val fat_100g: Double?,
    val fiber_100g: Double?,
    val sugars_100g: Double?
) {
    // Fallback for different API field names
    val energyKcal: Double? get() = energy_kcal_100g
    val proteins: Double? get() = proteins_100g
    val carbohydrates: Double? get() = carbohydrates_100g
    val fat: Double? get() = fat_100g
}
