package com.mealplanplus.shared.util

import com.mealplanplus.shared.model.FoodItem
import com.mealplanplus.shared.model.FoodUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class NutritionCalculatorTest {

    private fun createTestFood(
        caloriesPer100: Double = 100.0,
        proteinPer100: Double = 10.0,
        carbsPer100: Double = 20.0,
        fatPer100: Double = 5.0
    ) = FoodItem(
        id = 1,
        name = "Test Food",
        caloriesPer100 = caloriesPer100,
        proteinPer100 = proteinPer100,
        carbsPer100 = carbsPer100,
        fatPer100 = fatPer100
    )

    @Test
    fun testCalculateWithGrams() {
        val food = createTestFood(
            caloriesPer100 = 200.0,
            proteinPer100 = 20.0,
            carbsPer100 = 10.0,
            fatPer100 = 5.0
        )

        val result = NutritionCalculator.calculate(food, 50.0, FoodUnit.GRAM)

        assertEquals(100.0, result.calories, 0.01)
        assertEquals(10.0, result.protein, 0.01)
        assertEquals(5.0, result.carbs, 0.01)
        assertEquals(2.5, result.fat, 0.01)
    }

    @Test
    fun testCalculateWith100Grams() {
        val food = createTestFood(
            caloriesPer100 = 165.0,
            proteinPer100 = 31.0,
            carbsPer100 = 0.0,
            fatPer100 = 3.6
        )

        val result = NutritionCalculator.calculate(food, 100.0, FoodUnit.GRAM)

        assertEquals(165.0, result.calories, 0.01)
        assertEquals(31.0, result.protein, 0.01)
        assertEquals(0.0, result.carbs, 0.01)
        assertEquals(3.6, result.fat, 0.01)
    }

    @Test
    fun testMacroSummaryAddition() {
        val summary1 = MacroSummary(calories = 100.0, protein = 10.0, carbs = 20.0, fat = 5.0)
        val summary2 = MacroSummary(calories = 50.0, protein = 5.0, carbs = 10.0, fat = 2.5)

        val result = summary1 + summary2

        assertEquals(150.0, result.calories, 0.01)
        assertEquals(15.0, result.protein, 0.01)
        assertEquals(30.0, result.carbs, 0.01)
        assertEquals(7.5, result.fat, 0.01)
    }

    @Test
    fun testMacroSummaryMultiplication() {
        val summary = MacroSummary(calories = 100.0, protein = 10.0, carbs = 20.0, fat = 5.0)

        val result = summary * 2.0

        assertEquals(200.0, result.calories, 0.01)
        assertEquals(20.0, result.protein, 0.01)
        assertEquals(40.0, result.carbs, 0.01)
        assertEquals(10.0, result.fat, 0.01)
    }

    @Test
    fun testCalculateTotalWithMultipleFoods() {
        val food1 = createTestFood(
            caloriesPer100 = 100.0,
            proteinPer100 = 10.0,
            carbsPer100 = 20.0,
            fatPer100 = 5.0
        )
        val food2 = createTestFood(
            caloriesPer100 = 200.0,
            proteinPer100 = 20.0,
            carbsPer100 = 10.0,
            fatPer100 = 10.0
        )

        val items = listOf(
            FoodWithQuantity(food1, 100.0, FoodUnit.GRAM),
            FoodWithQuantity(food2, 50.0, FoodUnit.GRAM)
        )

        val result = NutritionCalculator.calculateTotal(items)

        // food1: 100g = 100 cal, 10 protein, 20 carbs, 5 fat
        // food2: 50g = 100 cal, 10 protein, 5 carbs, 5 fat
        assertEquals(200.0, result.calories, 0.01)
        assertEquals(20.0, result.protein, 0.01)
        assertEquals(25.0, result.carbs, 0.01)
        assertEquals(10.0, result.fat, 0.01)
    }

    @Test
    fun testMacroPercentages() {
        // 100g protein = 400 cal
        // 100g carbs = 400 cal
        // 50g fat = 450 cal
        // Total = 1250 cal
        val summary = MacroSummary(
            calories = 1250.0,
            protein = 100.0,
            carbs = 100.0,
            fat = 50.0
        )

        val (proteinPct, carbsPct, fatPct) = NutritionCalculator.macroPercentages(summary)

        assertEquals(32.0, proteinPct, 1.0)  // 400/1250
        assertEquals(32.0, carbsPct, 1.0)    // 400/1250
        assertEquals(36.0, fatPct, 1.0)      // 450/1250
    }

    @Test
    fun testMacroPercentagesZero() {
        val summary = MacroSummary.ZERO

        val (proteinPct, carbsPct, fatPct) = NutritionCalculator.macroPercentages(summary)

        assertEquals(0.0, proteinPct, 0.01)
        assertEquals(0.0, carbsPct, 0.01)
        assertEquals(0.0, fatPct, 0.01)
    }

    @Test
    fun testMacroSummaryIntValues() {
        val summary = MacroSummary(
            calories = 123.7,
            protein = 15.9,
            carbs = 30.1,
            fat = 8.5
        )

        assertEquals(123, summary.caloriesInt)
        assertEquals(15, summary.proteinInt)
        assertEquals(30, summary.carbsInt)
        assertEquals(8, summary.fatInt)
    }

    @Test
    fun testZeroMacroSummary() {
        val zero = MacroSummary.ZERO

        assertEquals(0.0, zero.calories, 0.01)
        assertEquals(0.0, zero.protein, 0.01)
        assertEquals(0.0, zero.carbs, 0.01)
        assertEquals(0.0, zero.fat, 0.01)
    }

    @Test
    fun testCalculateTotalEmptyList() {
        val result = NutritionCalculator.calculateTotal(emptyList())

        assertEquals(0.0, result.calories, 0.01)
        assertEquals(0.0, result.protein, 0.01)
        assertEquals(0.0, result.carbs, 0.01)
        assertEquals(0.0, result.fat, 0.01)
    }
}
