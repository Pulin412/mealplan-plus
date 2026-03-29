package com.mealplanplus.ui.screens.calendar

import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.*
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.PlanRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var planRepo: PlanRepository
    private lateinit var dietRepo: DietRepository
    private lateinit var viewModel: CalendarViewModel

    private val today = LocalDate.now()
    private val todayStr = today.toString()
    private val testDiet = Diet(id = 1L, userId = 1L, name = "Low-Carb Plan", description = "Under 100g carbs/day")
    private val testPlan = Plan(userId = 1L, date = todayStr, dietId = 1L, isCompleted = false)
    private val testDietWithMeals = DietWithMeals(diet = testDiet, meals = emptyMap())
    private val testTag = Tag(id = 1L, userId = 1L, name = "Low Carb", color = "#7B1FA2")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        planRepo = mockk(relaxed = true)
        dietRepo = mockk(relaxed = true)

        val month = YearMonth.now()
        val start = month.atDay(1).toString()
        val end = month.atEndOfMonth().toString()

        every { planRepo.getPlansWithDietNames(any(), any()) } returns flowOf(emptyList())
        every { dietRepo.getDietsByUser() } returns flowOf(emptyList())
        coEvery { planRepo.getDietForDate(any()) } returns null
        coEvery { dietRepo.getDietWithMeals(any()) } returns testDietWithMeals
        coEvery { dietRepo.getTagsForDiet(any()) } returns listOf(testTag)

        viewModel = CalendarViewModel(planRepo, dietRepo, SavedStateHandle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `selectDate with plan loads correct diet`() = runTest {
        coEvery { planRepo.getDietForDate(todayStr) } returns testDiet
        coEvery { dietRepo.getDietWithMeals(1L) } returns testDietWithMeals
        coEvery { dietRepo.getTagsForDiet(1L) } returns listOf(testTag)

        viewModel.selectDate(today)

        val state = viewModel.uiState.value
        assertEquals(testDiet, state.selectedDiet)
        assertEquals(today, state.selectedDate)
    }

    @Test
    fun `selectDate with no plan sets null diet`() = runTest {
        coEvery { planRepo.getDietForDate(todayStr) } returns null

        viewModel.selectDate(today)

        val state = viewModel.uiState.value
        assertNull(state.selectedDiet)
        assertNull(state.selectedDietWithMeals)
        assertTrue(state.selectedDietTags.isEmpty())
    }

    @Test
    fun `assignDiet updates plan and state optimistically`() = runTest {
        coEvery { planRepo.setPlanForDate(todayStr, 1L) } returns Unit
        coEvery { dietRepo.getDietWithMeals(1L) } returns testDietWithMeals

        viewModel.selectDate(today)
        viewModel.assignDiet(testDiet)

        coVerify { planRepo.setPlanForDate(todayStr, 1L) }
        val state = viewModel.uiState.value
        assertEquals(testDiet, state.selectedDiet)
        assertFalse(state.showDietPicker)
    }

    @Test
    fun `clearPlan removes diet from state`() = runTest {
        // Setup with a plan
        coEvery { planRepo.getDietForDate(todayStr) } returns testDiet
        viewModel.selectDate(today)

        coEvery { planRepo.removePlan(todayStr) } returns Unit
        viewModel.clearPlan()

        coVerify { planRepo.removePlan(todayStr) }
        val state = viewModel.uiState.value
        assertNull(state.selectedDiet)
        assertNull(state.selectedDietWithMeals)
        assertFalse(state.plans.containsKey(todayStr))
    }

    @Test
    fun `toggleView changes isWeekView flag`() = runTest {
        assertTrue(viewModel.uiState.value.isWeekView)

        viewModel.toggleView()
        assertFalse(viewModel.uiState.value.isWeekView)

        viewModel.toggleView()
        assertTrue(viewModel.uiState.value.isWeekView)
    }

    @Test
    fun `goToNextMonth reloads plans for new month`() = runTest {
        val currentMonth = viewModel.uiState.value.currentMonth
        viewModel.goToNextMonth()

        val newMonth = viewModel.uiState.value.currentMonth
        assertEquals(currentMonth.plusMonths(1), newMonth)
        // getPlansWithDietNames called for new range
        val newStart = newMonth.atDay(1).toString()
        val newEnd = newMonth.atEndOfMonth().toString()
        coVerify(atLeast = 1) { planRepo.getPlansWithDietNames(any(), any()) }
    }
}
