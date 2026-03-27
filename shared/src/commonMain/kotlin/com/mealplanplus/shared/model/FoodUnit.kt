package com.mealplanplus.shared.model

/**
 * Units for measuring food quantities
 */
enum class FoodUnit(val label: String, val shortLabel: String) {
    GRAM("Grams", "g"),
    ML("Milliliters", "ml"),
    SERVING("Servings", "srv"),
    PIECE("Pieces", "pcs"),
    CUP("Cup", "cup"),
    TBSP("Tablespoon", "tbsp"),
    TSP("Teaspoon", "tsp"),
    SLICE("Slice", "slice"),
    SCOOP("Scoop", "scoop");

    companion object {
        fun fromString(value: String): FoodUnit =
            entries.find { it.name == value || it.shortLabel == value } ?: GRAM
    }
}
