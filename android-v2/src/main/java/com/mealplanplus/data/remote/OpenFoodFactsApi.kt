package com.mealplanplus.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Open Food Facts — free, no-key, ~3M-product barcode database. Called directly from the app (its
 * own Retrofit, no auth header) to resolve a scanned barcode into nutrition. Public API v2:
 * `https://world.openfoodfacts.org/api/v2/product/{barcode}.json`.
 */
interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "code,product_name,brands,nutriments",
    ): Response<OffProductResponse>
}

data class OffProductResponse(
    val status: Int? = null,        // 1 = found, 0 = not found
    val product: OffProduct? = null,
)

data class OffProduct(
    @SerializedName("product_name") val productName: String? = null,
    val brands: String? = null,
    val nutriments: OffNutriments? = null,
)

/** Per-100g values (grams / kcal). Any field may be absent when a product lacks that datum. */
data class OffNutriments(
    @SerializedName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerializedName("proteins_100g") val proteins100g: Double? = null,
    @SerializedName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerializedName("fat_100g") val fat100g: Double? = null,
)
