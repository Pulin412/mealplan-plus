package com.mealplanplus.ui.screens.diets

import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.DietFullSummary
import com.mealplanplus.data.model.Tag
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.TagRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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

@OptIn(ExperimentalCoroutinesApi::class)
class DietsViewModelTest {

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
            // coroutines can dispatch without throwing in subsequent test classes.
        }
    }

    private lateinit var dietRepo: DietRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var cache: DietDisplayCache
    private lateinit var viewModel: DietsViewModel

    private val tagLowCarb = Tag(id = 1, userId = 1, name = "Low Carb", color = "#4CAF50")
    private val tagHighProtein = Tag(id = 2, userId = 1, name = "High Protein", color = "#2196F3")

    // DietFullSummary instances — used by DietsViewModel.loadDietsWithMeals()
    private fun summary(
        id: Long, name: String, calories: Int = 0, createdAt: Long = id * 1000
    ) = DietFullSummary(
        id = id, name = name, description = null,
        createdAt = createdAt, mealCount = 0,
        totalCalories = calories, totalProtein = 0, totalCarbs = 0, totalFat = 0
    )

    private val s1 = summary(1, "Diet-M1", 1200, 1000)
    private val s2 = summary(2, "Diet-M10", 2000, 2000)
    private val s3 = summary(3, "Diet-R2", 1800, 3000)

    // Tags per diet (returned by getTagsForDiets batch call)
    private val tagsMap = mapOf(
        1L to listOf(tagLowCarb),
        2L to listOf(tagHighProtein),
        3L to listOf(tagLowCarb, tagHighProtein)
    )
    // Food names per diet
    private val foodNamesMap = mapOf(
        1L to listOf("Eggs", "Spinach"),
        2L to listOf("Chicken", "Whey"),
        3L to listOf("Rice", "Broccoli")
    )
    // Assigned slots per diet
    private val slotsMap = mapOf(
        1L to setOf("Breakfast", "Lunch"),
        2L to setOf("Breakfast", "Dinner"),
        3L to setOf("Lunch", "Dinner")
    )

    @Before
    fun setup() {
        dietRepo = mockk(relaxed = true)
        tagRepo = mockk(relaxed = true)
        cache = DietDisplayCache()

        every { tagRepo.getTagsByUser() } returns flowOf(listOf(tagLowCarb, tagHighProtein))
        // Return real summaries so loadDietsWithMeals() populates _dietsWithMeals correctly
        every { dietRepo.getDietsWithFullSummary() } returns flowOf(listOf(s1, s2, s3))
        coEvery { dietRepo.getTagsForDiets(any()) } returns tagsMap
        coEvery { dietRepo.getDietFoodNamesAndSlots(any()) } returns (foodNamesMap to slotsMap)

        viewModel = DietsViewModel(dietRepo, tagRepo, cache)
    }

    @After
    fun tearDown() {
        // Main dispatcher stays set (class-level @BeforeClass) — nothing to reset here.
    }

    // Helper: get the current state with a subscriber (needed for WhileSubscribed flows)
    private suspend fun state() = viewModel.uiState.first()

    // ── natural sort ──────────────────────────────────────────────────────────

    @Test
    fun `NAME_ASC natural sort orders Diet-M1 before Diet-M10`() = runTest {
        viewModel.updateSortOption(DietSortOption.NAME_ASC)
        val names = state().diets.map { it.diet.name }
        val m1Idx = names.indexOf("Diet-M1")
        val m10Idx = names.indexOf("Diet-M10")
        assertTrue("Diet-M1 should sort before Diet-M10", m1Idx < m10Idx)
    }

    @Test
    fun `NAME_DESC reverses natural sort order`() = runTest {
        viewModel.updateSortOption(DietSortOption.NAME_DESC)
        val names = state().diets.map { it.diet.name }
        val m1Idx = names.indexOf("Diet-M1")
        val m10Idx = names.indexOf("Diet-M10")
        assertTrue("Diet-M10 should sort before Diet-M1 in DESC", m10Idx < m1Idx)
    }

    @Test
    fun `CALORIES_ASC sorts lowest calories first`() = runTest {
        viewModel.updateSortOption(DietSortOption.CALORIES_ASC)
        val calories = state().diets.map { it.totalCalories }
        assertEquals(listOf(1200, 1800, 2000), calories)
    }

    @Test
    fun `CALORIES_DESC sorts highest calories first`() = runTest {
        viewModel.updateSortOption(DietSortOption.CALORIES_DESC)
        val calories = state().diets.map { it.totalCalories }
        assertEquals(listOf(2000, 1800, 1200), calories)
    }

    @Test
    fun `NEWEST sorts by createdAt descending`() = runTest {
        viewModel.updateSortOption(DietSortOption.NEWEST)
        val ids = state().diets.map { it.diet.id }
        // createdAt: id=1→1000, id=2→2000, id=3→3000 → newest first = [3,2,1]
        assertEquals(listOf(3L, 2L, 1L), ids)
    }

    @Test
    fun `OLDEST sorts by createdAt ascending`() = runTest {
        viewModel.updateSortOption(DietSortOption.OLDEST)
        val ids = state().diets.map { it.diet.id }
        assertEquals(listOf(1L, 2L, 3L), ids)
    }

    // ── search filter ─────────────────────────────────────────────────────────

    @Test
    fun `search by name filters correctly`() = runTest {
        viewModel.updateSearchQuery("M1")
        val names = state().diets.map { it.diet.name }
        assertTrue(names.contains("Diet-M1"))
        assertFalse(names.contains("Diet-R2"))
    }

    @Test
    fun `empty search returns all diets`() = runTest {
        viewModel.updateSearchQuery("zzz")
        viewModel.updateSearchQuery("")
        assertEquals(3, state().diets.size)
    }

    @Test
    fun `search is case insensitive`() = runTest {
        viewModel.updateSearchQuery("diet-m")
        assertTrue(state().diets.isNotEmpty())
    }

    // ── food filter ───────────────────────────────────────────────────────────

    @Test
    fun `food filter matches diets containing that food name`() = runTest {
        viewModel.updateFoodFilter("Chicken")
        val names = state().diets.map { it.diet.name }
        assertTrue(names.contains("Diet-M10"))
        assertFalse(names.contains("Diet-M1"))
        assertFalse(names.contains("Diet-R2"))
    }

    @Test
    fun `food filter is case insensitive`() = runTest {
        viewModel.updateFoodFilter("eggs")
        assertTrue(state().diets.any { it.diet.name == "Diet-M1" })
    }

    // ── slot filter ───────────────────────────────────────────────────────────

    @Test
    fun `slot filter returns only diets with that slot assigned`() = runTest {
        viewModel.toggleSlotFilter("Dinner")
        // Diet-M10 and Diet-R2 have Dinner; Diet-M1 does not
        val names = state().diets.map { it.diet.name }
        assertTrue(names.contains("Diet-M10"))
        assertTrue(names.contains("Diet-R2"))
        assertFalse(names.contains("Diet-M1"))
    }

    @Test
    fun `multiple slot filters require all slots to be assigned`() = runTest {
        viewModel.toggleSlotFilter("Breakfast")
        viewModel.toggleSlotFilter("Dinner")
        // Only Diet-M10 has BOTH Breakfast AND Dinner
        val items = state().diets
        assertEquals(1, items.size)
        assertEquals("Diet-M10", items[0].diet.name)
    }

    @Test
    fun `toggleSlotFilter twice removes the slot filter`() = runTest {
        viewModel.toggleSlotFilter("Dinner")
        viewModel.toggleSlotFilter("Dinner")
        assertEquals(3, state().diets.size)
    }

    // ── tag filter ────────────────────────────────────────────────────────────

    @Test
    fun `tag filter ANY mode returns diets with at least one selected tag`() = runTest {
        viewModel.toggleTagFilter(tagLowCarb.id)
        val names = state().diets.map { it.diet.name }
        // Diet-M1 and Diet-R2 have tagLowCarb
        assertTrue(names.contains("Diet-M1"))
        assertTrue(names.contains("Diet-R2"))
    }

    @Test
    fun `tag filter ALL mode returns only diets with all selected tags`() = runTest {
        // Ensure ALL mode
        if (viewModel.uiState.first().tagFilterMode != TagFilterMode.ALL) {
            viewModel.toggleTagFilterMode()
        }
        viewModel.toggleTagFilter(tagLowCarb.id)
        viewModel.toggleTagFilter(tagHighProtein.id)

        // Only Diet-R2 has BOTH tags
        val items = state().diets
        assertEquals(1, items.size)
        assertEquals("Diet-R2", items[0].diet.name)
    }

    @Test
    fun `clearTagFilters removes all selected tags`() = runTest {
        viewModel.toggleTagFilter(tagLowCarb.id)
        viewModel.toggleTagFilter(tagHighProtein.id)
        viewModel.clearTagFilters()
        val s = state()
        assertTrue(s.selectedTagIds.isEmpty())
        assertEquals(3, s.diets.size)
    }

    // ── clearAllFilters ───────────────────────────────────────────────────────

    @Test
    fun `clearAllFilters resets all filters`() = runTest {
        viewModel.updateSearchQuery("Diet-M1")
        viewModel.updateFoodFilter("Eggs")
        viewModel.toggleSlotFilter("Lunch")
        viewModel.toggleTagFilter(tagLowCarb.id)

        viewModel.clearAllFilters()

        val s = state()
        assertEquals("", s.searchQuery)
        assertEquals("", s.foodFilter)
        assertTrue(s.slotFilter.isEmpty())
        assertTrue(s.selectedTagIds.isEmpty())
        assertEquals(3, s.diets.size)
    }

    // ── tag dialog ────────────────────────────────────────────────────────────

    @Test
    fun `showTagsManagement sets showTagsDialog to true`() = runTest {
        viewModel.showTagsManagement()
        assertTrue(state().showTagsDialog)
    }

    @Test
    fun `hideTagsManagement sets showTagsDialog to false`() = runTest {
        viewModel.showTagsManagement()
        viewModel.hideTagsManagement()
        assertFalse(state().showTagsDialog)
    }

    // ── tagCountMap ───────────────────────────────────────────────────────────

    @Test
    fun `tagCountMap reflects how many diets have each tag`() = runTest {
        val s = state()
        // tagLowCarb (id=1): Diet-M1 + Diet-R2 = 2
        // tagHighProtein (id=2): Diet-M10 + Diet-R2 = 2
        assertEquals(2, s.tagCountMap[tagLowCarb.id])
        assertEquals(2, s.tagCountMap[tagHighProtein.id])
    }

    // ── totalDietCount ────────────────────────────────────────────────────────

    @Test
    fun `totalDietCount reflects all diets before filtering`() = runTest {
        // "Diet-R2" matches only one diet (unique name); Diet-M1 would also match Diet-M10
        viewModel.updateSearchQuery("Diet-R2")
        val s = state()
        // filtered list has 1, but total count is 3
        assertEquals(1, s.diets.size)
        assertEquals(3, s.totalDietCount)
    }
}
