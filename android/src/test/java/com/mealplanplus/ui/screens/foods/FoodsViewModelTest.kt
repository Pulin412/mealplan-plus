package com.mealplanplus.ui.screens.foods

import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.Tag
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.TagRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class FoodsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var foodRepo: FoodRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var viewModel: FoodsViewModel

    private val apple = FoodItem(id = 1, name = "Apple", caloriesPer100 = 52.0, proteinPer100 = 0.3, carbsPer100 = 14.0, fatPer100 = 0.2)
    private val banana = FoodItem(id = 2, name = "Banana", caloriesPer100 = 89.0, proteinPer100 = 1.1, carbsPer100 = 23.0, fatPer100 = 0.3, isFavorite = true)
    private val chicken = FoodItem(id = 3, name = "Chicken Breast", caloriesPer100 = 165.0, proteinPer100 = 31.0, carbsPer100 = 0.0, fatPer100 = 3.6)
    private val tagLowCarb = Tag(id = 10, userId = 1, name = "Low Carb", color = "#4CAF50")

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        foodRepo = mockk(relaxed = true)
        tagRepo = mockk(relaxed = true)

        every { foodRepo.getAllFoods() } returns flowOf(listOf(apple, banana, chicken))
        every { foodRepo.getFavorites() } returns flowOf(listOf(banana))
        every { foodRepo.getRecentFoods(any()) } returns flowOf(listOf(chicken))
        every { foodRepo.searchFoods(any()) } returns flowOf(emptyList())
        every { foodRepo.getFoodIdsForTag(any()) } returns flowOf(emptyList())
        every { tagRepo.getTagsByUser() } returns flowOf(listOf(tagLowCarb))

        viewModel = FoodsViewModel(foodRepo, tagRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── tab switching ────────────────────────────────────────────────────────

    @Test
    fun `initial tab is ALL`() {
        assertEquals(FoodTab.ALL, viewModel.selectedTab.value)
    }

    @Test
    fun `selectTab FAVORITES switches to favorites source`() = runTest {
        viewModel.selectTab(FoodTab.FAVORITES)
        assertEquals(FoodTab.FAVORITES, viewModel.selectedTab.value)
    }

    @Test
    fun `selectTab RECENT switches to recent source`() = runTest {
        viewModel.selectTab(FoodTab.RECENT)
        assertEquals(FoodTab.RECENT, viewModel.selectedTab.value)
    }

    @Test
    fun `selectTab ALL after FAVORITES returns to all`() = runTest {
        viewModel.selectTab(FoodTab.FAVORITES)
        viewModel.selectTab(FoodTab.ALL)
        assertEquals(FoodTab.ALL, viewModel.selectedTab.value)
    }

    // ── search query ─────────────────────────────────────────────────────────

    @Test
    fun `initial search query is empty`() {
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `onSearchQueryChange updates searchQuery`() {
        viewModel.onSearchQueryChange("apple")
        assertEquals("apple", viewModel.searchQuery.value)
    }

    @Test
    fun `non-blank search query uses searchFoods`() = runTest {
        every { foodRepo.searchFoods("chi") } returns flowOf(listOf(chicken))
        viewModel.onSearchQueryChange("chi")
        assertEquals(listOf(chicken), viewModel.foods.first())
    }

    @Test
    fun `clearing search query reverts to getAllFoods`() = runTest {
        viewModel.onSearchQueryChange("chi")
        viewModel.onSearchQueryChange("")
        assertEquals(listOf(apple, banana, chicken), viewModel.foods.first())
    }

    // ── tag filter ───────────────────────────────────────────────────────────

    @Test
    fun `initial selectedTagIds is empty`() {
        assertTrue(viewModel.selectedTagIds.value.isEmpty())
    }

    @Test
    fun `toggleTag adds tag id to selection`() {
        viewModel.toggleTag(10L)
        assertTrue(10L in viewModel.selectedTagIds.value)
    }

    @Test
    fun `toggleTag twice removes tag id`() {
        viewModel.toggleTag(10L)
        viewModel.toggleTag(10L)
        assertFalse(10L in viewModel.selectedTagIds.value)
    }

    @Test
    fun `clearTagFilter empties selectedTagIds`() {
        viewModel.toggleTag(10L)
        viewModel.toggleTag(20L)
        viewModel.clearTagFilter()
        assertTrue(viewModel.selectedTagIds.value.isEmpty())
    }

    @Test
    fun `tag filter intersection excludes foods not in all selected tags`() = runTest {
        every { foodRepo.getFoodIdsForTag(10L) } returns flowOf(listOf(1L))
        every { foodRepo.getFoodIdsForTag(20L) } returns flowOf(listOf(2L))

        viewModel.toggleTag(10L)
        viewModel.toggleTag(20L)

        assertTrue(viewModel.foods.first().isEmpty())
    }

    @Test
    fun `tag filter with single tag returns matching foods`() = runTest {
        every { foodRepo.getFoodIdsForTag(10L) } returns flowOf(listOf(1L, 3L))
        viewModel.toggleTag(10L)

        val ids = viewModel.foods.first().map { it.id }
        assertTrue(ids.containsAll(listOf(1L, 3L)))
        assertFalse(2L in ids)
    }

    // ── toggleFavorite ───────────────────────────────────────────────────────

    @Test
    fun `toggleFavorite calls setFavorite with inverted value`() = runTest {
        viewModel.toggleFavorite(apple)
        coVerify { foodRepo.setFavorite(apple.id, true) }
    }

    @Test
    fun `toggleFavorite on favorite food sets isFavorite to false`() = runTest {
        viewModel.toggleFavorite(banana)
        coVerify { foodRepo.setFavorite(banana.id, false) }
    }

    // ── deleteFood ───────────────────────────────────────────────────────────

    @Test
    fun `deleteFood delegates to repository`() = runTest {
        viewModel.deleteFood(apple)
        coVerify { foodRepo.deleteFood(apple) }
    }

    // ── tags ─────────────────────────────────────────────────────────────────

    @Test
    fun `tags flow reflects tagRepository`() = runTest {
        assertEquals(listOf(tagLowCarb), viewModel.tags.first())
    }

    @Test
    fun `addTagToFood delegates to repository`() = runTest {
        viewModel.addTagToFood(foodId = 1L, tagId = 10L)
        coVerify { foodRepo.addTagToFood(1L, 10L) }
    }

    @Test
    fun `removeTagFromFood delegates to repository`() = runTest {
        viewModel.removeTagFromFood(foodId = 1L, tagId = 10L)
        coVerify { foodRepo.removeTagFromFood(1L, 10L) }
    }
}
