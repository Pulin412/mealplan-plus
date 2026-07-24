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

    /**
     * Text search via the Search-a-licious host (absolute URL — different host than the product API,
     * and the only reliable OFF search endpoint). Native HTTP has no CORS constraint, so Android calls
     * it directly; the webapp goes through a backend proxy instead.
     */
    @GET("https://search.openfoodfacts.org/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page_size") pageSize: Int = 30,
        @Query("fields") fields: String = "code,product_name,brands,nutriments",
    ): Response<OffSearchResponse>
}

data class OffSearchResponse(
    val hits: List<OffSearchHit>? = null,
)

/** A search hit. Note `brands` is an **array** here (vs. a comma-string in the product API). */
data class OffSearchHit(
    val code: String? = null,
    @SerializedName("product_name") val productName: String? = null,
    val brands: List<String>? = null,
    val nutriments: OffNutriments? = null,
)

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
