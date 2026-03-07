package com.mealplanplus.ui.screens.home

import android.content.Context
import app.cash.turbine.test
import com.mealplanplus.data.model.DailyMacroSummary
import com.mealplanplus.data.model.DietWithMeals
import com.mealplanplus.data.model.MealWithFoods
import com.mealplanplus.data.model.Plan
import com.mealplanplus.data.model.PlanWithDietName
import com.mealplanplus.data.repository.AuthRepository
import com.mealplanplus.data.repository.DailyLogRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.HealthRepository
import com.mealplanplus.data.repository.PlanRepository
import com.mealplanplus.util.AuthPreferences
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject

import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dailyLogRepo: DailyLogRepository
    private lateinit var healthRepo: HealthRepository
    private lateinit var planRepo: PlanRepository
    private lateinit var dietRepo: DietRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dailyLogRepo = mockk(relaxed = true)
        healthRepo = mockk(relaxed = true)
        planRepo = mockk(relaxed = true)
        dietRepo = mockk(relaxed = true)
        authRepo = mockk(relaxed = true)
        context = mockk(relaxed = true)

        // Stub shared flows to return empty by default
        every { dailyLogRepo.getLogWithFoods(any()) } returns flowOf(null)
        every { healthRepo.getMetricsForDate(any()) } returns flowOf(emptyList())
        every { healthRepo.getMetricsByTypeInRange(any(), any(), any()) } returns flowOf(emptyList())
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(emptyList())
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(emptyList())

        // Stub AuthPreferences (object) to return null userId so loadUserName exits early
        mockkObject(AuthPreferences)
        every { AuthPreferences.getUserId(any()) } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkObject(AuthPreferences)
    }

    // ─── slotEmoji companion ───────────────────────────────────────────────────

    @Test
    fun slotEmoji_breakfast_returnsEgg() {
        assertEquals("🍳", HomeViewModel.slotEmoji("BREAKFAST"))
        assertEquals("🍳", HomeViewModel.slotEmoji("breakfast"))
    }

    @Test
    fun slotEmoji_lunch_returnsSun() {
        assertEquals("☀️", HomeViewModel.slotEmoji("LUNCH"))
    }

    @Test
    fun slotEmoji_dinner_returnsMoon() {
        assertEquals("🌙", HomeViewModel.slotEmoji("DINNER"))
    }

    @Test
    fun slotEmoji_snack_returnsApple() {
        assertEquals("🍎", HomeViewModel.slotEmoji("SNACK"))
        assertEquals("🍎", HomeViewModel.slotEmoji("EVENING_SNACK"))
    }

    @Test
    fun slotEmoji_preWorkout_returnsFlex() {
        assertEquals("💪", HomeViewModel.slotEmoji("PRE_WORKOUT"))
    }

    @Test
    fun slotEmoji_earlyMorning_returnsSunrise() {
        assertEquals("🌅", HomeViewModel.slotEmoji("EARLY_MORNING"))
    }

    @Test
    fun slotEmoji_unknown_returnsDefault() {
        assertEquals("🍽️", HomeViewModel.slotEmoji("UNKNOWN_SLOT"))
    }

    // ─── computeStreak ────────────────────────────────────────────────────────

    @Test
    fun streak_noData_isZero() = runTest {
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(emptyList())

        val vm = buildViewModel()
        assertEquals(0, vm.uiState.value.dayStreak)
    }

    @Test
    fun streak_todayOnly_isOne() = runTest {
        val today = LocalDate.now().toString()
        val data = listOf(DailyMacroSummary(date = today, calories = 1800.0, protein = 80.0, carbs = 200.0, fat = 60.0))
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(data)

        val vm = buildViewModel()
        assertEquals(1, vm.uiState.value.dayStreak)
    }

    @Test
    fun streak_threeContinuousDays_isThree() = runTest {
        val today = LocalDate.now()
        val data = (0..2).map { daysAgo ->
            DailyMacroSummary(
                date = today.minusDays(daysAgo.toLong()).toString(),
                calories = 1800.0, protein = 80.0, carbs = 200.0, fat = 60.0
            )
        }
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(data)

        val vm = buildViewModel()
        assertEquals(3, vm.uiState.value.dayStreak)
    }

    @Test
    fun streak_gapYesterday_isOneNotTwo() = runTest {
        val today = LocalDate.now()
        // Today logged, yesterday not, 2 days ago logged → streak should be 1 (breaks at yesterday)
        val data = listOf(
            DailyMacroSummary(date = today.toString(), calories = 1800.0, protein = 0.0, carbs = 0.0, fat = 0.0),
            DailyMacroSummary(date = today.minusDays(2).toString(), calories = 1500.0, protein = 0.0, carbs = 0.0, fat = 0.0)
        )
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(data)

        val vm = buildViewModel()
        assertEquals(1, vm.uiState.value.dayStreak)
    }

    @Test
    fun streak_zerocaloriesBreaksStreak() = runTest {
        val today = LocalDate.now()
        val data = listOf(
            DailyMacroSummary(date = today.toString(), calories = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0)
        )
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(data)

        val vm = buildViewModel()
        assertEquals(0, vm.uiState.value.dayStreak)
    }

    // ─── weekDays state derivation ─────────────────────────────────────────────

    @Test
    fun weekDays_todayWithCalories_isCompleted() = runTest {
        val today = LocalDate.now()
        val calories = listOf(
            DailyMacroSummary(date = today.toString(), calories = 1200.0, protein = 0.0, carbs = 0.0, fat = 0.0)
        )
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(calories)

        val vm = buildViewModel()
        val todayInfo = vm.uiState.value.weekDays.find { it.date == today }
        assertEquals(WeekDayState.COMPLETED, todayInfo?.state)
    }

    @Test
    fun weekDays_pastDayNoPlanNoCalories_isMissed() = runTest {
        val yesterday = LocalDate.now().minusDays(1)
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(emptyList())
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(emptyList())

        val vm = buildViewModel()
        val info = vm.uiState.value.weekDays.find { it.date == yesterday }
        assertEquals(WeekDayState.MISSED, info?.state)
    }

    @Test
    fun weekDays_todayNoPlanNoCalories_isNoData() = runTest {
        val today = LocalDate.now()
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(emptyList())
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(emptyList())

        val vm = buildViewModel()
        val info = vm.uiState.value.weekDays.find { it.date == today }
        // Today with no plan and no calories is NO_DATA (not MISSED, since it's today)
        assertEquals(WeekDayState.NO_DATA, info?.state)
    }

    @Test
    fun weekDays_pastDayWithPlanButNoCalories_isMissed() = runTest {
        val twoDaysAgo = LocalDate.now().minusDays(2)
        val plans = listOf(
            PlanWithDietName(
                userId = 1L,
                date = twoDaysAgo.toString(),
                dietId = 5L,
                isCompleted = false,
                notes = null,
                dietName = "Remission 12"
            )
        )
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(plans)
        every { dailyLogRepo.getCompletedDaysCalories(any(), any()) } returns flowOf(emptyList())

        val vm = buildViewModel()
        val info = vm.uiState.value.weekDays.find { it.date == twoDaysAgo }
        assertEquals(WeekDayState.MISSED, info?.state)
    }

    @Test
    fun weekDays_completedPlan_isCompleted() = runTest {
        val yesterday = LocalDate.now().minusDays(1)
        val plans = listOf(
            PlanWithDietName(
                userId = 1L,
                date = yesterday.toString(),
                dietId = 5L,
                isCompleted = true,
                notes = null,
                dietName = "Diet"
            )
        )
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(plans)

        val vm = buildViewModel()
        val info = vm.uiState.value.weekDays.find { it.date == yesterday }
        assertEquals(WeekDayState.COMPLETED, info?.state)
    }

    @Test
    fun weekDays_todayIsAlwaysIncluded() = runTest {
        val vm = buildViewModel()
        val today = LocalDate.now()
        val hasTodayEntry = vm.uiState.value.weekDays.any { it.date == today }
        assertEquals(true, hasTodayEntry)
    }

    @Test
    fun weekDays_alwaysHasSevenEntries() = runTest {
        val vm = buildViewModel()
        assertEquals(7, vm.uiState.value.weekDays.size)
    }

    // ─── dietLabel in week ────────────────────────────────────────────────────

    @Test
    fun weekDays_dietLabelExtractedCorrectly() = runTest {
        val today = LocalDate.now()
        val plans = listOf(
            PlanWithDietName(
                userId = 1L,
                date = today.toString(),
                dietId = 17L,
                isCompleted = false,
                notes = null,
                dietName = "Mediterranean Diet 17"
            )
        )
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(plans)

        val vm = buildViewModel()
        val info = vm.uiState.value.weekDays.find { it.date == today }
        // extractShortDietName should produce a short label (not null)
        assertEquals(false, info?.dietLabel.isNullOrBlank())
    }

    @Test
    fun weekDays_noPlan_dietLabelIsNull() = runTest {
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(emptyList())

        val vm = buildViewModel()
        val today = LocalDate.now()
        val info = vm.uiState.value.weekDays.find { it.date == today }
        assertEquals(null, info?.dietLabel)
    }

    // ─── todayPlanSlots ───────────────────────────────────────────────────────

    @Test
    fun todayPlanSlots_noDietAssigned_emptyList() = runTest {
        coEvery { planRepo.getPlanForDate(any()) } returns null

        val vm = buildViewModel()
        assertEquals(emptyList<TodayPlanSlot>(), vm.uiState.value.todayPlanSlots)
    }

    @Test
    fun todayPlanSlots_dietAssignedWithSlots_slotsReturned() = runTest {
        val fakePlanWithDietName = PlanWithDietName(
            userId = 1L,
            date = LocalDate.now().toString(),
            dietId = 42L,
            isCompleted = false,
            notes = null,
            dietName = "Test Diet"
        )
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(listOf(fakePlanWithDietName))

        val fakeDietWithMeals = mockk<DietWithMeals>(relaxed = true)
        val breakfastMeal = mockk<MealWithFoods>(relaxed = true)
        every { breakfastMeal.meal.name } returns "Oats Bowl"
        every { fakeDietWithMeals.meals } returns mapOf("BREAKFAST" to breakfastMeal, "LUNCH" to null)
        coEvery { dietRepo.getDietWithMeals(42L) } returns fakeDietWithMeals

        val vm = buildViewModel()
        val slots = vm.uiState.value.todayPlanSlots
        assertEquals(2, slots.size)

        val breakfast = slots.find { it.slotType == "BREAKFAST" }
        assertEquals("Breakfast", breakfast?.slotDisplayName)
        assertEquals("Oats Bowl", breakfast?.plannedMealName)
        assertEquals("🍳", breakfast?.emoji)
    }

    @Test
    fun todayPlanSlots_loggedSlot_isLoggedTrue() = runTest {
        val fakePlanWithDietName = PlanWithDietName(
            userId = 1L,
            date = LocalDate.now().toString(),
            dietId = 99L,
            isCompleted = false,
            notes = null,
            dietName = "Test Diet"
        )
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(listOf(fakePlanWithDietName))

        val fakeDietWithMeals = mockk<DietWithMeals>(relaxed = true)
        every { fakeDietWithMeals.meals } returns mapOf("BREAKFAST" to null)
        coEvery { dietRepo.getDietWithMeals(99L) } returns fakeDietWithMeals

        // Simulate breakfast being logged
        val loggedFoodWithDetails = mockk<com.mealplanplus.data.model.LoggedFoodWithDetails>(relaxed = true)
        every { loggedFoodWithDetails.loggedFood.slotType } returns "BREAKFAST"
        val logWithFoods = mockk<com.mealplanplus.data.model.DailyLogWithFoods>(relaxed = true)
        every { logWithFoods.foods } returns listOf(loggedFoodWithDetails)
        every { logWithFoods.foodsForSlot(any()) } returns emptyList()
        every { logWithFoods.foodsForSlot("BREAKFAST") } returns listOf(loggedFoodWithDetails)
        every { logWithFoods.totalCalories } returns 400.0
        every { logWithFoods.totalProtein } returns 20.0
        every { logWithFoods.totalCarbs } returns 50.0
        every { logWithFoods.totalFat } returns 10.0
        every { dailyLogRepo.getLogWithFoods(any()) } returns flowOf(logWithFoods)

        val vm = buildViewModel()
        val breakfast = vm.uiState.value.todayPlanSlots.find { it.slotType == "BREAKFAST" }
        assertEquals(true, breakfast?.isLogged)
    }

    @Test
    fun todayPlanSlots_unloggedSlot_isLoggedFalse() = runTest {
        val fakePlanWithDietName = PlanWithDietName(
            userId = 1L,
            date = LocalDate.now().toString(),
            dietId = 88L,
            isCompleted = false,
            notes = null,
            dietName = "Test Diet"
        )
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(listOf(fakePlanWithDietName))

        val fakeDietWithMeals = mockk<DietWithMeals>(relaxed = true)
        every { fakeDietWithMeals.meals } returns mapOf("DINNER" to null)
        coEvery { dietRepo.getDietWithMeals(88L) } returns fakeDietWithMeals

        val vm = buildViewModel()
        val dinner = vm.uiState.value.todayPlanSlots.find { it.slotType == "DINNER" }
        assertEquals(false, dinner?.isLogged)
    }

    @Test
    fun todayPlanSlots_sortedBySlotOrder() = runTest {
        val fakePlanWithDietName = PlanWithDietName(userId = 1L, date = LocalDate.now().toString(), dietId = 77L, isCompleted = false, notes = null, dietName = "Test Diet")
        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(listOf(fakePlanWithDietName))

        val fakeDietWithMeals = mockk<DietWithMeals>(relaxed = true)
        // DINNER (order 9) before BREAKFAST (order 1) — should be re-sorted
        every { fakeDietWithMeals.meals } returns linkedMapOf("DINNER" to null, "BREAKFAST" to null)
        coEvery { dietRepo.getDietWithMeals(77L) } returns fakeDietWithMeals

        val vm = buildViewModel()
        val slots = vm.uiState.value.todayPlanSlots
        assertEquals("BREAKFAST", slots.firstOrNull()?.slotType)
        assertEquals("DINNER", slots.lastOrNull()?.slotType)
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun buildViewModel() = HomeViewModel(
        dailyLogRepository = dailyLogRepo,
        healthRepository = healthRepo,
        planRepository = planRepo,
        dietRepository = dietRepo,
        authRepository = authRepo,
        context = context
    )
}
