package com.mealplanplus.data.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvExporterTest {

    private val sample = ExportData(
        meals = listOf(
            MealRow(
                name = "Chicken Bowl",
                slots = listOf("LUNCH", "DINNER"),
                kcal = 520,
                proteinG = 42.0,
                carbsG = 55.5,
                fatG = 12.0,
                items = "Chicken 150g; Rice 100g",
            ),
        ),
        diets = listOf(
            DietRow(
                name = "Cut, phase 1",           // comma → must be quoted
                tags = listOf("cutting", "highprotein"),
                kcal = 1800,
                proteinG = 160.0,
                carbsG = 120.0,
                fatG = 50.0,
                entryCount = 4,
            ),
        ),
        workoutSets = listOf(
            WorkoutSetRow("2026-07-22", "Push Day", "Bench Press", 1, 8, 60.0),
            WorkoutSetRow("2026-07-22", "Push Day", "Bench Press", 2, 8, null), // bodyweight → blank
        ),
        health = listOf(
            HealthRow("WEIGHT", "2026-07-20T08:00:00Z", 74.5, null, "kg"),
            HealthRow("BLOOD_PRESSURE", "2026-07-20T08:05:00Z", 120.0, 80.0, "mmHg"),
        ),
    )

    @Test
    fun `emits all four section headers in order`() {
        val csv = CsvExporter.build(sample)
        val mealsAt = csv.indexOf("# MEALS")
        val dietsAt = csv.indexOf("# DIETS")
        val workoutsAt = csv.indexOf("# WORKOUTS (last 7 days)")
        val healthAt = csv.indexOf("# HEALTH (last 90 days)")
        assertTrue(mealsAt in 0 until dietsAt)
        assertTrue(dietsAt < workoutsAt)
        assertTrue(workoutsAt < healthAt)
    }

    @Test
    fun `meal row joins slots with semicolons and quotes fields with commas`() {
        val csv = CsvExporter.build(sample)
        assertTrue(csv.contains("Chicken Bowl,LUNCH;DINNER,520,42,55.5,12,Chicken 150g; Rice 100g"))
    }

    @Test
    fun `field containing a comma is quoted`() {
        val csv = CsvExporter.build(sample)
        assertTrue(csv.contains("\"Cut, phase 1\",cutting;highprotein,1800,160,120,50,4"))
    }

    @Test
    fun `nullable weight and secondary render as empty cells`() {
        val csv = CsvExporter.build(sample)
        assertTrue(csv.contains("2026-07-22,Push Day,Bench Press,2,8,\n"))       // weight blank
        assertTrue(csv.contains("WEIGHT,2026-07-20T08:00:00Z,74.5,,kg"))          // secondary blank
        assertTrue(csv.contains("BLOOD_PRESSURE,2026-07-20T08:05:00Z,120,80,mmHg"))
    }

    @Test
    fun `whole-number doubles drop the trailing zero`() {
        val csv = CsvExporter.build(sample)
        assertTrue(csv.contains(",42,"))     // 42.0 → 42
        assertTrue(csv.contains(",55.5,"))   // 55.5 kept
    }

    @Test
    fun `embedded double quote is escaped by doubling`() {
        val csv = CsvExporter.build(
            sample.copy(meals = listOf(sample.meals[0].copy(name = "6\" Sub"))),
        )
        assertTrue(csv.contains("\"6\"\" Sub\""))
    }

    @Test
    fun `empty snapshot still emits every section header`() {
        val csv = CsvExporter.build(ExportData(emptyList(), emptyList(), emptyList(), emptyList()))
        assertTrue(csv.contains("# MEALS"))
        assertTrue(csv.contains("# DIETS"))
        assertTrue(csv.contains("# WORKOUTS (last 7 days)"))
        assertTrue(csv.contains("# HEALTH (last 90 days)"))
    }
}
