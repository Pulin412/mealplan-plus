package com.mealplanplus.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class FoodItemTest {

    private fun createChickenBreast() = FoodItem(
        id = 1,
        name = "Chicken Breast",
        caloriesPer100 = 165.0,
        proteinPer100 = 31.0,
        carbsPer100 = 0.0,
        fatPer100 = 3.6
    )

    @Test
    fun testCalculateCaloriesGrams() {
        val food = createChickenBreast()

        // 100g = 165 cal
        assertEquals(165.0, food.calculateCalories(100.0, FoodUnit.GRAM), 0.01)

        // 50g = 82.5 cal
        assertEquals(82.5, food.calculateCalories(50.0, FoodUnit.GRAM), 0.01)

        // 200g = 330 cal
        assertEquals(330.0, food.calculateCalories(200.0, FoodUnit.GRAM), 0.01)
    }

    @Test
    fun testCalculateProteinGrams() {
        val food = createChickenBreast()

        // 100g = 31g protein
        assertEquals(31.0, food.calculateProtein(100.0, FoodUnit.GRAM), 0.01)

        // 150g = 46.5g protein
        assertEquals(46.5, food.calculateProtein(150.0, FoodUnit.GRAM), 0.01)
    }

    @Test
    fun testCalculateCarbsAndFat() {
        val food = createChickenBreast()

        assertEquals(0.0, food.calculateCarbs(100.0, FoodUnit.GRAM), 0.01)
        assertEquals(3.6, food.calculateFat(100.0, FoodUnit.GRAM), 0.01)

        // Double the quantity
        assertEquals(7.2, food.calculateFat(200.0, FoodUnit.GRAM), 0.01)
    }

    @Test
    fun testToGramsFromGram() {
        val food = createChickenBreast()

        assertEquals(100.0, food.toGrams(100.0, FoodUnit.GRAM), 0.01)
        assertEquals(50.0, food.toGrams(50.0, FoodUnit.GRAM), 0.01)
    }

    @Test
    fun testToGramsFromMl() {
        val food = createChickenBreast()

        // ML assumes 1:1 density
        assertEquals(100.0, food.toGrams(100.0, FoodUnit.ML), 0.01)
    }

    @Test
    fun testToGramsFromCup() {
        val food = createChickenBreast()

        // 1 cup = 240g (default)
        assertEquals(240.0, food.toGrams(1.0, FoodUnit.CUP), 0.01)
    }

    @Test
    fun testToGramsFromTablespoon() {
        val food = createChickenBreast()

        // 1 tbsp = 15g (default)
        assertEquals(15.0, food.toGrams(1.0, FoodUnit.TBSP), 0.01)
    }

    @Test
    fun testToGramsFromTeaspoon() {
        val food = createChickenBreast()

        // 1 tsp = 5g (default)
        assertEquals(5.0, food.toGrams(1.0, FoodUnit.TSP), 0.01)
    }

    @Test
    fun testToGramsFromPiece() {
        val food = createChickenBreast()

        // PIECE uses gramsPerPiece (default 100g)
        assertEquals(100.0, food.toGrams(1.0, FoodUnit.PIECE), 0.01)
        assertEquals(200.0, food.toGrams(2.0, FoodUnit.PIECE), 0.01)
    }

    @Test
    fun testToGramsWithCustomPieceWeight() {
        val egg = FoodItem(
            id = 2,
            name = "Egg",
            caloriesPer100 = 155.0,
            proteinPer100 = 13.0,
            carbsPer100 = 1.1,
            fatPer100 = 11.0,
            gramsPerPiece = 50.0  // One egg = 50g
        )

        assertEquals(50.0, egg.toGrams(1.0, FoodUnit.PIECE), 0.01)
        assertEquals(100.0, egg.toGrams(2.0, FoodUnit.PIECE), 0.01)
    }

    @Test
    fun testFoodUnitValues() {
        assertEquals("g", FoodUnit.GRAM.shortLabel)
        assertEquals("ml", FoodUnit.ML.shortLabel)
        assertEquals("cup", FoodUnit.CUP.shortLabel)
        assertEquals("tbsp", FoodUnit.TBSP.shortLabel)
        assertEquals("tsp", FoodUnit.TSP.shortLabel)
        assertEquals("pc", FoodUnit.PIECE.shortLabel)
    }

    @Test
    fun testFoodUnitFromString() {
        assertEquals(FoodUnit.GRAM, FoodUnit.fromString("GRAM"))
        assertEquals(FoodUnit.GRAM, FoodUnit.fromString("g"))
        assertEquals(FoodUnit.ML, FoodUnit.fromString("ml"))
        assertEquals(FoodUnit.CUP, FoodUnit.fromString("cup"))
        assertEquals(FoodUnit.GRAM, FoodUnit.fromString("unknown"))  // default
    }
}
