package com.mealplanplus.ui.screens.log

import android.appwidget.AppWidgetManager
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.toEpochMs
import com.mealplanplus.util.WidgetAppearanceState
import com.mealplanplus.util.WidgetPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailyLogViewModelTest {

    companion object {
        @OptIn(ExperimentalCoroutinesApi::class)
        @JvmStatic
        @BeforeClass
        fun setUpClass() {
            Dispatchers.setMain(UnconfinedTestDispatcher())
        }

        @JvmStatic
        @AfterClass
        fun tearDownClass() {
            // Intentionally left empty: Main stays set so lingering background
            // coroutines (widget update, DataStore IO) can dispatch without
            // throwing in subsequent test classes.
        }
    }

    private lateinit var logRepository: DailyLogRepository
    private lateinit var mealRepository: MealRepository
    private lateinit var planRepository: PlanRepository
    private lateinit var dietRepository: DietRepository
    private lateinit var foodRepository: FoodRepository
    private lateinit var viewModel: DailyLogViewModel

    private val today = LocalDate.now()
    private val todayMs = today.toEpochMs()

    // Food with 635 kcal/100g → 635 kcal when quantity = 100g
    private val actualFood = FoodItem(
        id = 1L, name = "Chicken Breast",
        caloriesPer100 = 635.0, proteinPer100 = 31.0, carbsPer100 = 0.0, fatPer100 = 3.6
    )
    private val loggedFood = LoggedFood(
        id = 1L, userId = 1L, logDate = todayMs,
        foodId = 1L, quantity = 100.0, unit = FoodUnit.GRAM, slotType = "BREAKFAST"
    )
    private val loggedFoodWithDetails = LoggedFoodWithDetails(loggedFood, actualFood)
    private val logWithFoods = DailyLogWithFoods(
        log = DailyLog(userId = 1L, date = todayMs),
        foods = listOf(loggedFoodWithDetails)
    )

    // Planned meal with 1446 kcal (food with 1446 kcal/100g × 100g)
    private val plannedFood = FoodItem(
        id = 2L, name = "Full Day Diet",
        caloriesPer100 = 1446.0, proteinPer100 = 80.0, carbsPer100 = 150.0, fatPer100 = 40.0
    )
    private val mealFoodItem = MealFoodItem(mealId = 1L, foodId = 2L, quantity = 100.0, unit = FoodUnit.GRAM)
    private val mealFoodItemWithDetails = MealFoodItemWithDetails(mealFoodItem, plannedFood)
    private val meal = Meal(id = 1L, name = "Breakfast")
    private val mealWithFoods = MealWithFoods(meal, listOf(mealFoodItemWithDetails)) // totalCalories = 1446
    private val testDiet = Diet(id = 1L, name = "Test Diet")
    private val testPlan = Plan(userId = 1L, date = todayMs, dietId = 1L, isCompleted = false)
    private val dietWithMeals = DietWithMeals(testDiet, mapOf("BREAKFAST" to mealWithFoods))

    @Before
    fun setup() {
        logRepository = mockk(relaxed = true)
        mealRepository = mockk(relaxed = true)
        planRepository = mockk(relaxed = true)
        dietRepository = mockk(relaxed = true)
        foodRepository = mockk(relaxed = true)

        every { logRepository.getLogWithFoods(any()) } returns flowOf(logWithFoods)
        every { foodRepository.getAllFoods() } returns flowOf(emptyList())
        coEvery { planRepository.getPlanForDate(any()) } returns testPlan
        coEvery { dietRepository.getDietWithMeals(1L) } returns dietWithMeals
        val tempDir = java.io.File(System.getProperty("java.io.tmpdir"), "mealplan_test_${System.nanoTime()}")
        tempDir.mkdirs()
        val context = mockk<android.content.Context>(relaxed = true)
        every { context.filesDir } returns tempDir
        every { context.applicationContext } returns context

        // AuthPreferences is called in init for reactive custom slot loading
        mockkObject(AuthPreferences)
        every { AuthPreferences.getUserId(any()) } returns flowOf(1L)

        // WidgetPreferences.getAppearance reads DataStore (needs real filesDir); mock it out
        mockkObject(WidgetPreferences)
        coEvery { WidgetPreferences.getAppearance(any()) } returns WidgetAppearanceState()

        // Glance widget updateAll calls AppWidgetManager.getInstance internally
        mockkStatic(AppWidgetManager::class)
        every { AppWidgetManager.getInstance(any()) } returns mockk(relaxed = true)

        viewModel = DailyLogViewModel(logRepository, mealRepository, planRepository, dietRepository, foodRepository, context)
    }

    @After
    fun tearDown() {
        unmockkObject(AuthPreferences)
        unmockkObject(WidgetPreferences)
        // Note: AppWidgetManager static mock is intentionally NOT unmocked here.
        // The widget update coroutine runs on DefaultDispatcher and may outlive the test;
        // removing the mock early causes "not mocked" exceptions in subsequent tests.
    }

    @Test
    fun `loadLog_populatesLoggedFoods`() = runTest {
        val state = viewModel.uiState.value
        assertNotNull(state.logWithFoods)
        assertEquals(1, state.logWithFoods!!.foods.size)
        assertEquals("Chicken Breast", state.logWithFoods!!.foods.first().food.name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `deleteFood_removesFromSlot`() = runTest {
        viewModel.deleteLoggedFood(1L)
        coVerify { logRepository.deleteLoggedFood(1L) }
    }

    @Test
    fun `planVsActual_computesCorrectDiff`() = runTest {
        val state = viewModel.uiState.value
        // actual = 635, planned = 1446, diff = -811
        assertEquals(635, state.comparison.actualCalories)
        assertEquals(1446, state.comparison.plannedCalories)
        assertEquals(-811, state.comparison.calorieDiff)
    }

    @Test
    fun `dateNavigation_goToPreviousDay`() = runTest {
        viewModel.goToPreviousDay()
        assertEquals(today.minusDays(1), viewModel.uiState.value.date)
    }
}
