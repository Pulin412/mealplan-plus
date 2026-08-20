package com.mealplanplus.api.domain.food

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.mealplanplus.api.generated.model.FoodDto
import com.mealplanplus.api.generated.model.FoodUnit
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Server-side proxy to Open Food Facts text search. Exists so browser clients avoid CORS — the
 * reliable OFF search host (`search.openfoodfacts.org`) sends no `Access-Control-Allow-Origin`
 * header, so a webapp fetch would be blocked. Android calls OFF directly and skips this.
 *
 * Maps each hit to a lightweight [FoodDto] (no id); the client creates the chosen one via POST /foods.
 * Free, no API key. Failures degrade to an empty list rather than propagating.
 */
@Component
class OpenFoodFactsClient {

    private val client = RestClient.builder()
        .baseUrl("https://search.openfoodfacts.org")
        .defaultHeader("User-Agent", "MealPlanPlus/2.0 (backend)")
        .build()

    fun search(query: String): List<FoodDto> {
        val response = runCatching {
            client.get()
                .uri { b ->
                    b.path("/search")
                        .queryParam("q", query)
                        .queryParam("page_size", 30)
                        .queryParam("fields", "code,product_name,brands,nutriments")
                        .build()
                }
                .retrieve()
                .body(OffSearchResponse::class.java)
        }.getOrNull() ?: return emptyList()

        return (response.hits ?: emptyList()).mapNotNull { it.toFoodDto() }
    }

    private fun OffSearchHit.toFoodDto(): FoodDto? {
        val cleanName = productName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val kcal = nutriments?.energyKcal100g ?: return null   // drop hits without nutrition
        return FoodDto(
            name = cleanName,
            caloriesPer100 = kcal,
            proteinPer100 = nutriments.proteins100g ?: 0.0,
            carbsPer100 = nutriments.carbohydrates100g ?: 0.0,
            fatPer100 = nutriments.fat100g ?: 0.0,
            // Extra nutrients — left null when OFF doesn't report them (kept distinct from a real 0).
            fiberPer100 = nutriments.fiber100g,
            sugarsPer100 = nutriments.sugars100g,
            saturatedFatPer100 = nutriments.saturatedFat100g,
            sodiumPer100 = nutriments.sodium100g,
            brand = brands?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() },
            barcode = code,
            unit = FoodUnit.GRAM,
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffSearchResponse(val hits: List<OffSearchHit>? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffSearchHit(
    val code: String? = null,
    @JsonProperty("product_name") val productName: String? = null,
    val brands: List<String>? = null,
    val nutriments: OffNutriments? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class OffNutriments(
    @JsonProperty("energy-kcal_100g") val energyKcal100g: Double? = null,
    @JsonProperty("proteins_100g") val proteins100g: Double? = null,
    @JsonProperty("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @JsonProperty("fat_100g") val fat100g: Double? = null,
    @JsonProperty("fiber_100g") val fiber100g: Double? = null,
    @JsonProperty("sugars_100g") val sugars100g: Double? = null,
    @JsonProperty("saturated-fat_100g") val saturatedFat100g: Double? = null,
    @JsonProperty("sodium_100g") val sodium100g: Double? = null,
)
