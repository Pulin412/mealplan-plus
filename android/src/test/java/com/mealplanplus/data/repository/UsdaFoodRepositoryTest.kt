package com.mealplanplus.data.repository

import com.mealplanplus.data.remote.UsdaFoodApi
import com.mealplanplus.data.remote.UsdaFoodItem
import com.mealplanplus.data.remote.UsdaSearchResponse
import com.mealplanplus.util.CrashlyticsReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UsdaFoodRepositoryTest {

    private lateinit var api: UsdaFoodApi
    private lateinit var crashlytics: CrashlyticsReporter
    private lateinit var repository: UsdaFoodRepository

    private fun fakeItem(fdcId: Int = 1, description: String = "Chicken") = UsdaFoodItem(
        fdcId = fdcId,
        description = description,
        brandOwner = "USDA",
        servingSize = 100.0,
        servingSizeUnit = "g"
    )

    @Before
    fun setUp() {
        api = mockk()
        crashlytics = mockk(relaxed = true)
        repository = UsdaFoodRepository(api, crashlytics)
    }

    // ── searchFoods — success ─────────────────────────────────────────────────

    @Test
    fun searchFoods_success_returnsResults() = runTest {
        coEvery { api.searchFoods(query = "chicken") } returns UsdaSearchResponse(
            totalHits = 1,
            foods = listOf(fakeItem(fdcId = 42, description = "Chicken Breast"))
        )

        val result = repository.searchFoods("chicken")

        assertTrue(result.isSuccess)
        val items = result.getOrThrow()
        assertEquals(1, items.size)
        assertEquals(42, items[0].fdcId)
        assertEquals("Chicken Breast", items[0].name)
    }

    @Test
    fun searchFoods_emptyFoodsList_returnsEmptyList() = runTest {
        coEvery { api.searchFoods(query = "xyz123") } returns UsdaSearchResponse(foods = emptyList())

        val result = repository.searchFoods("xyz123")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun searchFoods_nullFoodsList_returnsEmptyList() = runTest {
        coEvery { api.searchFoods(query = "xyz") } returns UsdaSearchResponse(foods = null)

        val result = repository.searchFoods("xyz")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    // ── searchFoods — failure ─────────────────────────────────────────────────

    @Test
    fun searchFoods_apiThrows_returnsFailure() = runTest {
        val exception = RuntimeException("network error")
        coEvery { api.searchFoods(query = "chicken") } throws exception

        val result = repository.searchFoods("chicken")

        assertTrue(result.isFailure)
    }

    @Test
    fun searchFoods_apiThrows_reportsNonFatalWithQueryContext() = runTest {
        val exception = RuntimeException("timeout")
        coEvery { api.searchFoods(query = "beef") } throws exception

        repository.searchFoods("beef")

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(
                exception,
                context = "usda_search",
                extras = mapOf("query" to "beef")
            )
        }
    }

    @Test
    fun searchFoods_success_doesNotReportNonFatal() = runTest {
        coEvery { api.searchFoods(query = "apple") } returns UsdaSearchResponse(foods = emptyList())

        repository.searchFoods("apple")

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any(), any()) }
    }

    // ── getFoodDetails — success ───────────────────────────────────────────────

    @Test
    fun getFoodDetails_success_returnsResult() = runTest {
        coEvery { api.getFoodDetails(fdcId = 99) } returns fakeItem(fdcId = 99, description = "Salmon")

        val result = repository.getFoodDetails(99)

        assertTrue(result.isSuccess)
        assertEquals(99, result.getOrThrow().fdcId)
        assertEquals("Salmon", result.getOrThrow().name)
        assertEquals("USDA", result.getOrThrow().brand)
    }

    // ── getFoodDetails — failure ──────────────────────────────────────────────

    @Test
    fun getFoodDetails_apiThrows_returnsFailure() = runTest {
        coEvery { api.getFoodDetails(fdcId = 1) } throws RuntimeException("404")

        val result = repository.getFoodDetails(1)

        assertTrue(result.isFailure)
    }

    @Test
    fun getFoodDetails_apiThrows_reportsNonFatalWithFdcIdContext() = runTest {
        val exception = RuntimeException("server error")
        coEvery { api.getFoodDetails(fdcId = 77) } throws exception

        repository.getFoodDetails(77)

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(
                exception,
                context = "usda_details",
                extras = mapOf("fdcId" to "77")
            )
        }
    }

    @Test
    fun getFoodDetails_success_doesNotReportNonFatal() = runTest {
        coEvery { api.getFoodDetails(fdcId = 5) } returns fakeItem(fdcId = 5)

        repository.getFoodDetails(5)

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any(), any()) }
    }
}
