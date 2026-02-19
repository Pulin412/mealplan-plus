package com.mealplanplus.shared.util

import com.mealplanplus.shared.model.*

/**
 * Summary of macronutrients
 */
data class MacroSummary(
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
) {
    operator fun plus(other: MacroSummary) = MacroSummary(
        calories = calories + other.calories,
        protein = protein + other.protein,
        carbs = carbs + other.carbs,
        fat = fat + other.fat
    )

    operator fun times(multiplier: Double) = MacroSummary(
        calories = calories * multiplier,
        protein = protein * multiplier,
        carbs = carbs * multiplier,
        fat = fat * multiplier
    )

    val caloriesInt: Int get() = calories.toInt()
    val proteinInt: Int get() = protein.toInt()
    val carbsInt: Int get() = carbs.toInt()
    val fatInt: Int get() = fat.toInt()

    companion object {
        val ZERO = MacroSummary()
    }
}

/**
 * Food item with quantity for calculation
 */
data class FoodWithQuantity(
    val food: FoodItem,
    val quantity: Double,
    val unit: FoodUnit
)

/**
 * Nutrition calculation utilities
 */
object NutritionCalculator {

    /**
     * Calculate macros for a food item with given quantity
     */
    fun calculate(food: FoodItem, quantity: Double, unit: FoodUnit): MacroSummary {
        return MacroSummary(
            calories = food.calculateCalories(quantity, unit),
            protein = food.calculateProtein(quantity, unit),
            carbs = food.calculateCarbs(quantity, unit),
            fat = food.calculateFat(quantity, unit)
        )
    }

    /**
     * Calculate total macros for a list of foods with quantities
     */
    fun calculateTotal(items: List<FoodWithQuantity>): MacroSummary {
        return items.fold(MacroSummary.ZERO) { acc, item ->
            acc + calculate(item.food, item.quantity, item.unit)
        }
    }

    /**
     * Calculate macros for meal food items
     */
    fun calculateMealMacros(items: List<MealFoodItemWithDetails>): MacroSummary {
        return items.fold(MacroSummary.ZERO) { acc, item ->
            acc + MacroSummary(
                calories = item.calculatedCalories,
                protein = item.calculatedProtein,
                carbs = item.calculatedCarbs,
                fat = item.calculatedFat
            )
        }
    }

    /**
     * Calculate macros for a meal with foods
     */
    fun calculateMealWithFoodsMacros(mealWithFoods: MealWithFoods): MacroSummary {
        return calculateMealMacros(mealWithFoods.items)
    }

    /**
     * Calculate macros for logged foods
     */
    fun calculateLoggedFoodsMacros(foods: List<LoggedFoodWithDetails>): MacroSummary {
        return foods.fold(MacroSummary.ZERO) { acc, item ->
            acc + MacroSummary(
                calories = item.calculatedCalories,
                protein = item.calculatedProtein,
                carbs = item.calculatedCarbs,
                fat = item.calculatedFat
            )
        }
    }

    /**
     * Calculate macros for logged meals (with quantity multiplier)
     */
    fun calculateLoggedMealsMacros(meals: List<LoggedMealWithDetails>): MacroSummary {
        return meals.fold(MacroSummary.ZERO) { acc, meal ->
            acc + MacroSummary(
                calories = meal.totalCalories,
                protein = meal.totalProtein,
                carbs = meal.totalCarbs,
                fat = meal.totalFat
            )
        }
    }

    /**
     * Calculate macros for a diet (all meals)
     */
    fun calculateDietMacros(dietWithMeals: DietWithMeals): MacroSummary {
        return dietWithMeals.meals.values
            .filterNotNull()
            .fold(MacroSummary.ZERO) { acc, meal ->
                acc + calculateMealWithFoodsMacros(meal)
            }
    }

    /**
     * Calculate daily log macros
     */
    fun calculateDailyLogMacros(log: DailyLogWithFoods): MacroSummary {
        return calculateLoggedFoodsMacros(log.foods)
    }

    /**
     * Convert quantity from one unit to grams
     */
    fun toGrams(food: FoodItem, quantity: Double, unit: FoodUnit): Double {
        return food.toGrams(quantity, unit)
    }

    /**
     * Calculate calorie percentage from macro (for pie charts)
     */
    fun macroPercentages(summary: MacroSummary): Triple<Double, Double, Double> {
        val totalMacroCalories = (summary.protein * 4) + (summary.carbs * 4) + (summary.fat * 9)
        if (totalMacroCalories <= 0) return Triple(0.0, 0.0, 0.0)

        val proteinPercent = (summary.protein * 4 / totalMacroCalories) * 100
        val carbsPercent = (summary.carbs * 4 / totalMacroCalories) * 100
        val fatPercent = (summary.fat * 9 / totalMacroCalories) * 100

        return Triple(proteinPercent, carbsPercent, fatPercent)
    }
}
