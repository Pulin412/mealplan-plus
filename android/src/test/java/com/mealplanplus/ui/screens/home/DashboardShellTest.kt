package com.mealplanplus.ui.screens.home

import com.mealplanplus.data.generated.model.CalorieRingDto
import com.mealplanplus.data.generated.model.DashboardDto
import com.mealplanplus.data.generated.model.MacroPanelDto
import com.mealplanplus.data.generated.model.SlotStatusDto
import com.mealplanplus.data.generated.model.StreakDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The day-rollover shell: a cached dashboard captured yesterday must paint today's *empty* layout
 * instantly — targets/streak kept, but every day-specific "logged" value reset so the first open of
 * a new day never flashes yesterday's checkmarks or a full ring before the refresh lands.
 */
class DashboardShellTest {

    private fun yesterday() = DashboardDto(
        date = LocalDate.of(2026, 8, 3),
        calorieRing = CalorieRingDto(target = 2000, consumed = 1800.0, remaining = 200.0, isOver = false),
        macros = MacroPanelDto(
            consumedProtein = 100.0, consumedCarbs = 150.0, consumedFat = 60.0,
            targetProtein = 120, targetCarbs = 200, targetFat = 70,
        ),
        slots = listOf(
            SlotStatusDto(slot = "BREAKFAST", isLogged = true, kcal = 500.0, protein = 30.0,
                carbs = 50.0, fat = 20.0, items = emptyList(), mealName = "Oats"),
            SlotStatusDto(slot = "DINNER", isLogged = true, kcal = 700.0, protein = 40.0,
                carbs = 60.0, fat = 25.0, items = emptyList(), mealName = "Curry"),
        ),
        additionalFoods = emptyList(),
        streak = StreakDto(current = 5, best = 9, dots = listOf(true, true, true)),
        dietCount = 2,
        dietName = "Cutting",
    )

    private val today = LocalDate.of(2026, 8, 4)

    @Test
    fun shell_stampsToday_andResetsConsumedTotals() {
        val shell = yesterday().shellFor(today)
        assertEquals(today, shell.date)
        assertEquals(0.0, shell.calorieRing.consumed, 0.0)
        assertEquals(2000.0, shell.calorieRing.remaining, 0.0)   // remaining resets to full target
        assertFalse(shell.calorieRing.isOver)
        assertEquals(0.0, shell.macros.consumedProtein, 0.0)
        assertEquals(0.0, shell.macros.consumedCarbs, 0.0)
        assertEquals(0.0, shell.macros.consumedFat, 0.0)
    }

    @Test
    fun shell_clearsLoggedState_butKeepsPlanAndTargets() {
        val shell = yesterday().shellFor(today)
        assertTrue(shell.slots.none { it.isLogged })          // no stale checkmarks
        assertEquals(listOf("BREAKFAST", "DINNER"), shell.slots.map { it.slot })  // layout kept
        assertEquals(2000, shell.calorieRing.target)          // targets kept
        assertEquals(120, shell.macros.targetProtein)
        assertEquals("Cutting", shell.dietName)               // diet context kept
    }

    @Test
    fun shell_isNoOp_whenAlreadyToday() {
        val fresh = yesterday().copy(date = today)
        assertEquals(fresh, fresh.shellFor(today))
    }
}
