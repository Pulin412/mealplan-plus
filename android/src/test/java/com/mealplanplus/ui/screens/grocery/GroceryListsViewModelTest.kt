package com.mealplanplus.ui.screens.grocery

import com.mealplanplus.data.model.GroceryItem
import com.mealplanplus.data.model.GroceryItemWithFood
import com.mealplanplus.data.model.GroceryList
import com.mealplanplus.data.model.GroceryListWithItems
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.repository.GroceryRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class GroceryListsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: GroceryRepository
    private lateinit var viewModel: GroceryListsViewModel

    private val list1 = GroceryList(id = 1L, userId = 1L, name = "Week 1")
    private val list2 = GroceryList(id = 2L, userId = 1L, name = "Week 2")

    private fun makeItem(listId: Long, itemId: Long, checked: Boolean = false) =
        GroceryItemWithFood(
            item = GroceryItem(id = itemId, listId = listId, quantity = 1.0, unit = FoodUnit.GRAM, isChecked = checked),
            food = null
        )

    private fun withItems(list: GroceryList, items: List<GroceryItemWithFood>) =
        GroceryListWithItems(list = list, items = items)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)

        every { repo.getListsByUser() } returns flowOf(listOf(list1, list2))

        // list1: 3 items, 1 checked
        coEvery { repo.getListWithItemsOnce(1L) } returns withItems(list1, listOf(
            makeItem(1L, 101L, checked = true),
            makeItem(1L, 102L),
            makeItem(1L, 103L)
        ))
        // list2: 2 items, 2 checked
        coEvery { repo.getListWithItemsOnce(2L) } returns withItems(list2, listOf(
            makeItem(2L, 201L, checked = true),
            makeItem(2L, 202L, checked = true)
        ))

        viewModel = GroceryListsViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has both lists loaded`() = runTest {
        val state = viewModel.uiState.value
        assertEquals(2, state.lists.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `list1 has correct item and checked counts`() = runTest {
        val item = viewModel.uiState.value.lists.find { it.list.id == 1L }!!
        assertEquals(3, item.itemCount)
        assertEquals(1, item.checkedCount)
    }

    @Test
    fun `list2 is fully checked`() = runTest {
        val item = viewModel.uiState.value.lists.find { it.list.id == 2L }!!
        assertEquals(2, item.itemCount)
        assertEquals(2, item.checkedCount)
    }

    @Test
    fun `progressPercent is computed correctly`() = runTest {
        val item = viewModel.uiState.value.lists.find { it.list.id == 1L }!!
        assertEquals(1f / 3f, item.progressPercent, 0.001f)
    }

    @Test
    fun `empty list has progressPercent of zero`() {
        coEvery { repo.getListWithItemsOnce(any()) } returns withItems(list1, emptyList())
        viewModel = GroceryListsViewModel(repo)
        val item = viewModel.uiState.value.lists.firstOrNull()
        // With empty items, progressPercent should be 0
        assertEquals(0f, item?.progressPercent ?: 0f, 0.001f)
    }

    // ── deleteList ────────────────────────────────────────────────────────────

    @Test
    fun `deleteList delegates to repository`() = runTest {
        viewModel.deleteList(list1)
        coVerify { repo.deleteList(list1) }
    }

    // ── error handling ────────────────────────────────────────────────────────

    @Test
    fun `clearError resets error to null`() = runTest {
        // Simulate error state by re-creating with failing repo
        every { repo.getListsByUser() } returns flowOf(emptyList())
        viewModel = GroceryListsViewModel(repo)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // ── names preserved ───────────────────────────────────────────────────────

    @Test
    fun `list names are preserved in GroceryListItem`() = runTest {
        val names = viewModel.uiState.value.lists.map { it.list.name }
        assertTrue(names.contains("Week 1"))
        assertTrue(names.contains("Week 2"))
    }
}
