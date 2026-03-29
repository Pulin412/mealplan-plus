package com.mealplanplus.ui.screens.grocery

import androidx.lifecycle.SavedStateHandle
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.model.GroceryItem
import com.mealplanplus.data.model.GroceryItemWithFood
import com.mealplanplus.data.model.GroceryList
import com.mealplanplus.data.model.GroceryListWithItems
import com.mealplanplus.data.repository.GroceryRepository
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
class GroceryDetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: GroceryRepository
    private lateinit var viewModel: GroceryDetailViewModel

    private val listId = 42L
    private val groceryList = GroceryList(id = listId, userId = 1L, name = "Weekend Shop")

    private fun makeItem(id: Long, checked: Boolean = false, name: String? = null) =
        GroceryItemWithFood(
            item = GroceryItem(id = id, listId = listId, customName = name, quantity = 1.0, unit = FoodUnit.GRAM, isChecked = checked),
            food = null
        )

    private val item1 = makeItem(1L, checked = false, name = "Eggs")
    private val item2 = makeItem(2L, checked = true, name = "Milk")
    private val item3 = makeItem(3L, checked = false, name = "Bread")

    private val listWithItems = GroceryListWithItems(list = groceryList, items = listOf(item1, item2, item3))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)

        every { repo.getListWithItems(listId) } returns flowOf(listWithItems)

        val context = mockk<android.content.Context>(relaxed = true)
        val savedState = SavedStateHandle(mapOf("listId" to listId))
        viewModel = GroceryDetailViewModel(savedState, repo, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── initial load ──────────────────────────────────────────────────────────

    @Test
    fun `initial state loads list with items`() = runTest {
        val state = viewModel.uiState.value
        assertNotNull(state.list)
        assertEquals(3, state.list!!.items.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun `initial tab is 0 (All)`() = runTest {
        assertEquals(0, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `showAddItemDialog is false initially`() = runTest {
        assertFalse(viewModel.uiState.value.showAddItemDialog)
    }

    // ── tab switching ─────────────────────────────────────────────────────────

    @Test
    fun `setSelectedTab updates selectedTab`() = runTest {
        viewModel.setSelectedTab(1)
        assertEquals(1, viewModel.uiState.value.selectedTab)
    }

    @Test
    fun `setSelectedTab back to 0 works`() = runTest {
        viewModel.setSelectedTab(1)
        viewModel.setSelectedTab(0)
        assertEquals(0, viewModel.uiState.value.selectedTab)
    }

    // ── toggleItemChecked ─────────────────────────────────────────────────────

    @Test
    fun `toggleItemChecked calls repo with inverted value`() = runTest {
        viewModel.toggleItemChecked(1L, currentChecked = false)
        coVerify { repo.toggleItemChecked(1L, true) }
    }

    @Test
    fun `toggleItemChecked on checked item passes false`() = runTest {
        viewModel.toggleItemChecked(2L, currentChecked = true)
        coVerify { repo.toggleItemChecked(2L, false) }
    }

    // ── uncheckAllItems ───────────────────────────────────────────────────────

    @Test
    fun `uncheckAllItems delegates to repo with listId`() = runTest {
        viewModel.uncheckAllItems()
        coVerify { repo.uncheckAllItems(listId) }
    }

    // ── deleteItem ────────────────────────────────────────────────────────────

    @Test
    fun `deleteItem delegates to repo`() = runTest {
        viewModel.deleteItem(1L)
        coVerify { repo.deleteItem(1L) }
    }

    // ── add item dialog ───────────────────────────────────────────────────────

    @Test
    fun `showAddItemDialog sets flag to true`() = runTest {
        viewModel.showAddItemDialog()
        assertTrue(viewModel.uiState.value.showAddItemDialog)
    }

    @Test
    fun `hideAddItemDialog sets flag to false`() = runTest {
        viewModel.showAddItemDialog()
        viewModel.hideAddItemDialog()
        assertFalse(viewModel.uiState.value.showAddItemDialog)
    }

    @Test
    fun `addCustomItem calls repo and closes dialog`() = runTest {
        viewModel.showAddItemDialog()
        viewModel.addCustomItem("Butter", 200.0, FoodUnit.GRAM, "Dairy")
        coVerify { repo.addCustomItem(listId, "Butter", 200.0, FoodUnit.GRAM, "Dairy") }
        assertFalse(viewModel.uiState.value.showAddItemDialog)
    }

    // ── edit item dialog ──────────────────────────────────────────────────────

    @Test
    fun `showEditItemDialog sets item`() = runTest {
        viewModel.showEditItemDialog(item1)
        assertEquals(item1, viewModel.uiState.value.showEditItemDialog)
    }

    @Test
    fun `hideEditItemDialog clears item`() = runTest {
        viewModel.showEditItemDialog(item1)
        viewModel.hideEditItemDialog()
        assertNull(viewModel.uiState.value.showEditItemDialog)
    }

    @Test
    fun `updateItemQuantity calls repo and closes dialog`() = runTest {
        viewModel.showEditItemDialog(item1)
        viewModel.updateItemQuantity(1L, 250.0)
        coVerify { repo.updateItemQuantity(1L, 250.0) }
        assertNull(viewModel.uiState.value.showEditItemDialog)
    }

    // ── regenerate ────────────────────────────────────────────────────────────

    @Test
    fun `regenerateList delegates to repo`() = runTest {
        viewModel.regenerateList()
        coVerify { repo.regenerateList(listId) }
    }

    @Test
    fun `regenerateList clears isRegenerating after completion`() = runTest {
        viewModel.regenerateList()
        assertFalse(viewModel.uiState.value.isRegenerating)
    }

    // ── clearError ────────────────────────────────────────────────────────────

    @Test
    fun `clearError resets error to null`() = runTest {
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }
}
