package com.mealplanplus.data.model

import org.junit.Assert.*
import org.junit.Test

class FoodItemTest {

    private fun food(
        calories: Double = 200.0,
        protein: Double = 10.0,
        carbs: Double = 30.0,
        fat: Double = 5.0,
        gramsPerPiece: Double? = null,
        gramsPerCup: Double? = null,
        gramsPerTbsp: Double? = null,
        gramsPerTsp: Double? = null
    ) = FoodItem(
        name = "Test Food",
        caloriesPer100 = calories,
        proteinPer100 = protein,
        carbsPer100 = carbs,
        fatPer100 = fat,
        gramsPerPiece = gramsPerPiece,
        gramsPerCup = gramsPerCup,
        gramsPerTbsp = gramsPerTbsp,
        gramsPerTsp = gramsPerTsp
    )

    // ── toGrams ──────────────────────────────────────────────────────────────

    @Test
    fun `toGrams GRAM returns quantity as-is`() {
        assertEquals(150.0, food().toGrams(150.0, FoodUnit.GRAM), 0.001)
    }

    @Test
    fun `toGrams ML returns quantity as-is`() {
        assertEquals(250.0, food().toGrams(250.0, FoodUnit.ML), 0.001)
    }

    @Test
    fun `toGrams SERVING returns quantity times 100`() {
        assertEquals(200.0, food().toGrams(2.0, FoodUnit.SERVING), 0.001)
    }

    @Test
    fun `toGrams PIECE uses gramsPerPiece when set`() {
        val f = food(gramsPerPiece = 50.0)
        assertEquals(150.0, f.toGrams(3.0, FoodUnit.PIECE), 0.001)
    }

    @Test
    fun `toGrams PIECE falls back to 100g when gramsPerPiece is null`() {
        assertEquals(200.0, food().toGrams(2.0, FoodUnit.PIECE), 0.001)
    }

    @Test
    fun `toGrams SLICE uses gramsPerPiece when set`() {
        val f = food(gramsPerPiece = 30.0)
        assertEquals(90.0, f.toGrams(3.0, FoodUnit.SLICE), 0.001)
    }

    @Test
    fun `toGrams SLICE falls back to 30g when gramsPerPiece is null`() {
        assertEquals(60.0, food().toGrams(2.0, FoodUnit.SLICE), 0.001)
    }

    @Test
    fun `toGrams SCOOP falls back to 30g when gramsPerPiece is null`() {
        assertEquals(30.0, food().toGrams(1.0, FoodUnit.SCOOP), 0.001)
    }

    @Test
    fun `toGrams CUP uses gramsPerCup when set`() {
        val f = food(gramsPerCup = 185.0)
        assertEquals(370.0, f.toGrams(2.0, FoodUnit.CUP), 0.001)
    }

    @Test
    fun `toGrams CUP falls back to 240g when gramsPerCup is null`() {
        assertEquals(240.0, food().toGrams(1.0, FoodUnit.CUP), 0.001)
    }

    @Test
    fun `toGrams TBSP uses gramsPerTbsp when set`() {
        val f = food(gramsPerTbsp = 14.0)
        assertEquals(42.0, f.toGrams(3.0, FoodUnit.TBSP), 0.001)
    }

    @Test
    fun `toGrams TBSP falls back to 15g when gramsPerTbsp is null`() {
        assertEquals(15.0, food().toGrams(1.0, FoodUnit.TBSP), 0.001)
    }

    @Test
    fun `toGrams TSP uses gramsPerTsp when set`() {
        val f = food(gramsPerTsp = 4.0)
        assertEquals(8.0, f.toGrams(2.0, FoodUnit.TSP), 0.001)
    }

    @Test
    fun `toGrams TSP falls back to 5g when gramsPerTsp is null`() {
        assertEquals(5.0, food().toGrams(1.0, FoodUnit.TSP), 0.001)
    }

    // ── calculateCalories ────────────────────────────────────────────────────

    @Test
    fun `calculateCalories for 100g GRAM returns caloriesPer100`() {
        val f = food(calories = 350.0)
        assertEquals(350.0, f.calculateCalories(100.0, FoodUnit.GRAM), 0.01)
    }

    @Test
    fun `calculateCalories for 200g GRAM is double caloriesPer100`() {
        val f = food(calories = 200.0)
        assertEquals(400.0, f.calculateCalories(200.0, FoodUnit.GRAM), 0.01)
    }

    @Test
    fun `calculateCalories for 1 PIECE with known weight`() {
        // 1 egg = 50g, calories=155 per 100g → 1 egg = 77.5 kcal
        val egg = food(calories = 155.0, gramsPerPiece = 50.0)
        assertEquals(77.5, egg.calculateCalories(1.0, FoodUnit.PIECE), 0.01)
    }

    @Test
    fun `calculateCalories for 1 SERVING returns caloriesPer100`() {
        val f = food(calories = 400.0)
        // 1 serving = 100g → 400 * 100 / 100 = 400
        assertEquals(400.0, f.calculateCalories(1.0, FoodUnit.SERVING), 0.01)
    }

    @Test
    fun `calculateCalories returns zero when quantity is zero`() {
        assertEquals(0.0, food().calculateCalories(0.0, FoodUnit.GRAM), 0.001)
    }

    // ── calculateProtein / Carbs / Fat ───────────────────────────────────────

    @Test
    fun `calculateProtein for 200g GRAM`() {
        val f = food(protein = 25.0)
        assertEquals(50.0, f.calculateProtein(200.0, FoodUnit.GRAM), 0.01)
    }

    @Test
    fun `calculateCarbs for 1 CUP with custom cup weight`() {
        // rice: 185g/cup, 28g carbs per 100g → 1 cup = 185 * 0.28 = 51.8g carbs
        val rice = food(carbs = 28.0, gramsPerCup = 185.0)
        assertEquals(51.8, rice.calculateCarbs(1.0, FoodUnit.CUP), 0.01)
    }

    @Test
    fun `calculateFat for 1 TBSP olive oil`() {
        // olive oil: 14g/tbsp, 100g fat per 100g → 1 tbsp = 14g fat
        val oil = food(fat = 100.0, gramsPerTbsp = 14.0)
        assertEquals(14.0, oil.calculateFat(1.0, FoodUnit.TBSP), 0.01)
    }

    // ── backward-compatibility properties ────────────────────────────────────

    @Test
    fun `backward compat calories equals caloriesPer100`() {
        val f = food(calories = 300.0)
        assertEquals(f.caloriesPer100, f.calories, 0.001)
    }

    @Test
    fun `servingSize is always 100`() {
        assertEquals(100.0, food().servingSize, 0.001)
    }

    @Test
    fun `servingUnit is always g`() {
        assertEquals("g", food().servingUnit)
    }
}
