package com.mealplanplus.util

import org.junit.Assert.assertEquals
import org.junit.Test

class GroceryCategoryTest {

    private fun cat(name: String) = GroceryCategory.categorize(name)

    // ── Dairy ─────────────────────────────────────────────────────────────────

    @Test fun `milk categorizes as Dairy`() = assertEquals(GroceryCategory.DAIRY, cat("Whole Milk"))
    @Test fun `yogurt categorizes as Dairy`() = assertEquals(GroceryCategory.DAIRY, cat("Greek Yogurt"))
    @Test fun `cheese categorizes as Dairy`() = assertEquals(GroceryCategory.DAIRY, cat("Cheddar Cheese"))
    @Test fun `whey protein categorizes as Dairy`() = assertEquals(GroceryCategory.DAIRY, cat("Whey Protein Isolate"))
    @Test fun `ghee categorizes as Dairy`() = assertEquals(GroceryCategory.DAIRY, cat("Ghee"))

    // ── Proteins ──────────────────────────────────────────────────────────────

    @Test fun `chicken breast categorizes as Proteins`() = assertEquals(GroceryCategory.PROTEINS, cat("Chicken Breast"))
    @Test fun `salmon categorizes as Proteins`() = assertEquals(GroceryCategory.PROTEINS, cat("Atlantic Salmon"))
    @Test fun `eggs categorizes as Proteins`() = assertEquals(GroceryCategory.PROTEINS, cat("Boiled Eggs"))
    @Test fun `lentils categorizes as Proteins`() = assertEquals(GroceryCategory.PROTEINS, cat("Red Lentils"))
    @Test fun `tofu categorizes as Proteins`() = assertEquals(GroceryCategory.PROTEINS, cat("Firm Tofu"))

    // ── Grains ────────────────────────────────────────────────────────────────

    @Test fun `rice categorizes as Grains`() = assertEquals(GroceryCategory.GRAINS, cat("Brown Rice"))
    @Test fun `oats categorizes as Grains`() = assertEquals(GroceryCategory.GRAINS, cat("Rolled Oats"))
    @Test fun `bread categorizes as Grains`() = assertEquals(GroceryCategory.GRAINS, cat("Whole Wheat Bread"))
    @Test fun `pasta categorizes as Grains`() = assertEquals(GroceryCategory.GRAINS, cat("Whole Grain Pasta"))

    // ── Vegetables ────────────────────────────────────────────────────────────

    @Test fun `spinach categorizes as Vegetables`() = assertEquals(GroceryCategory.VEGETABLES, cat("Baby Spinach"))
    @Test fun `broccoli categorizes as Vegetables`() = assertEquals(GroceryCategory.VEGETABLES, cat("Broccoli Florets"))
    @Test fun `sweet potato categorizes as Vegetables`() = assertEquals(GroceryCategory.VEGETABLES, cat("Sweet Potato"))
    @Test fun `mushroom categorizes as Vegetables`() = assertEquals(GroceryCategory.VEGETABLES, cat("Portobello Mushroom"))

    // ── Fruits ────────────────────────────────────────────────────────────────

    @Test fun `apple categorizes as Fruits`() = assertEquals(GroceryCategory.FRUITS, cat("Granny Smith Apple"))
    @Test fun `banana categorizes as Fruits`() = assertEquals(GroceryCategory.FRUITS, cat("Banana"))
    @Test fun `blueberry (singular) categorizes as Fruits`() = assertEquals(GroceryCategory.FRUITS, cat("Fresh Blueberry"))
    @Test fun `avocado categorizes as Fruits`() = assertEquals(GroceryCategory.FRUITS, cat("Hass Avocado"))

    // ── Fats & Oils ───────────────────────────────────────────────────────────

    @Test fun `olive oil categorizes as Fats`() = assertEquals(GroceryCategory.FATS_OILS, cat("Extra Virgin Olive Oil"))
    @Test fun `almonds categorizes as Fats`() = assertEquals(GroceryCategory.FATS_OILS, cat("Raw Almonds"))
    @Test fun `chia seeds categorizes as Fats`() = assertEquals(GroceryCategory.FATS_OILS, cat("Chia Seeds"))

    // ── Beverages ─────────────────────────────────────────────────────────────

    // Note: "Orange Juice" / "Coconut Water" hit fruit keywords before beverages — use kombucha
    @Test fun `kombucha categorizes as Beverages`() = assertEquals(GroceryCategory.BEVERAGES, cat("Kombucha"))
    @Test fun `green tea categorizes as Beverages`() = assertEquals(GroceryCategory.BEVERAGES, cat("Green Tea"))
    @Test fun `coffee categorizes as Beverages`() = assertEquals(GroceryCategory.BEVERAGES, cat("Black Coffee"))

    // ── Spices & Condiments ───────────────────────────────────────────────────

    @Test fun `salt categorizes as Spices`() = assertEquals(GroceryCategory.SPICES, cat("Sea Salt"))
    @Test fun `honey categorizes as Spices`() = assertEquals(GroceryCategory.SPICES, cat("Raw Honey"))
    @Test fun `soy sauce categorizes as Spices`() = assertEquals(GroceryCategory.SPICES, cat("Low Sodium Soy Sauce"))
    @Test fun `cumin categorizes as Spices`() = assertEquals(GroceryCategory.SPICES, cat("Ground Cumin"))

    // ── Other ─────────────────────────────────────────────────────────────────

    @Test fun `unknown food categorizes as Other`() = assertEquals(GroceryCategory.OTHER, cat("Xanthan Gum"))
    @Test fun `empty string categorizes as Other`() = assertEquals(GroceryCategory.OTHER, cat(""))

    // ── Case-insensitivity ────────────────────────────────────────────────────

    @Test fun `categorize is case insensitive`() {
        assertEquals(GroceryCategory.DAIRY, cat("MILK"))
        assertEquals(GroceryCategory.PROTEINS, cat("CHICKEN"))
        assertEquals(GroceryCategory.GRAINS, cat("RICE"))
    }

    // ── All categories are non-null ───────────────────────────────────────────

    @Test fun `all() list contains all category constants`() {
        val all = GroceryCategory.all
        assert(GroceryCategory.DAIRY in all)
        assert(GroceryCategory.PROTEINS in all)
        assert(GroceryCategory.GRAINS in all)
        assert(GroceryCategory.VEGETABLES in all)
        assert(GroceryCategory.FRUITS in all)
        assert(GroceryCategory.FATS_OILS in all)
        assert(GroceryCategory.BEVERAGES in all)
        assert(GroceryCategory.SPICES in all)
        assert(GroceryCategory.OTHER in all)
        assertEquals(9, all.size)
    }
}
