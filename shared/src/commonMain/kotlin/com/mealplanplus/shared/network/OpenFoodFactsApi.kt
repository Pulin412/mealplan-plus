package com.mealplanplus.shared.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenFoodFacts API client
 */
class OpenFoodFactsApi(private val client: HttpClient) {

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org"
    }

    suspend fun getProductByBarcode(barcode: String): OpenFoodFactsResponse {
        return client.get("$BASE_URL/api/v0/product/$barcode.json").body()
    }

    suspend fun searchProducts(
        query: String,
        pageSize: Int = 20
    ): OpenFoodFactsSearchResponse {
        return client.get("$BASE_URL/cgi/search.pl") {
            parameter("search_terms", query)
            parameter("search_simple", 1)
            parameter("action", "process")
            parameter("json", 1)
            parameter("page_size", pageSize)
        }.body()
    }
}

@Serializable
data class OpenFoodFactsResponse(
    val status: Int = 0,
    @SerialName("status_verbose")
    val statusVerbose: String? = null,
    val product: OpenFoodFactsProduct? = null
)

@Serializable
data class OpenFoodFactsSearchResponse(
    val count: Int = 0,
    val page: Int = 1,
    @SerialName("page_size")
    val pageSize: Int = 20,
    val products: List<OpenFoodFactsProduct> = emptyList()
)

@Serializable
data class OpenFoodFactsProduct(
    val code: String? = null,
    @SerialName("product_name")
    val productName: String? = null,
    val brands: String? = null,
    @SerialName("serving_size")
    val servingSize: String? = null,
    val nutriments: Nutriments? = null
)

@Serializable
data class Nutriments(
    @SerialName("energy-kcal_100g")
    val energyKcal100g: Double? = null,
    @SerialName("proteins_100g")
    val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g")
    val carbohydrates100g: Double? = null,
    @SerialName("fat_100g")
    val fat100g: Double? = null,
    @SerialName("fiber_100g")
    val fiber100g: Double? = null,
    @SerialName("sugars_100g")
    val sugars100g: Double? = null
) {
    val energyKcal: Double get() = energyKcal100g ?: 0.0
    val proteins: Double get() = proteins100g ?: 0.0
    val carbohydrates: Double get() = carbohydrates100g ?: 0.0
    val fat: Double get() = fat100g ?: 0.0
}
