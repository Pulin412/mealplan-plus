package com.mealplanplus.data.repository

import com.mealplanplus.data.remote.Nutriments
import com.mealplanplus.data.remote.OpenFoodFactsApi
import com.mealplanplus.data.remote.OpenFoodFactsProduct
import com.mealplanplus.data.remote.OpenFoodFactsResponse
import com.mealplanplus.data.remote.OpenFoodFactsSearchResponse
import com.mealplanplus.util.CrashlyticsReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenFoodFactsRepositoryTest {

    private lateinit var api: OpenFoodFactsApi
    private lateinit var crashlytics: CrashlyticsReporter
    private lateinit var repository: OpenFoodFactsRepository

    private fun fakeNutriments(kcal: Double = 200.0) = Nutriments(
        energy_kcal_100g = kcal,
        proteins_100g = 10.0,
        carbohydrates_100g = 30.0,
        fat_100g = 5.0,
        fiber_100g = null,
        sugars_100g = null
    )

    private fun fakeProduct(name: String = "Oat Milk", barcode: String = "123") =
        OpenFoodFactsProduct(
            code = barcode,
            product_name = name,
            brands = "Test Brand",
            serving_size = null,
            nutriments = fakeNutriments()
        )

    @Before
    fun setUp() {
        api = mockk()
        crashlytics = mockk(relaxed = true)
        repository = OpenFoodFactsRepository(api, crashlytics)
    }

    // ── getProductByBarcode — success ─────────────────────────────────────────

    @Test
    fun getProductByBarcode_status1WithProduct_returnsFoodItem() = runTest {
        coEvery { api.getProductByBarcode("123") } returns OpenFoodFactsResponse(
            status = 1,
            status_verbose = "product found",
            product = fakeProduct(name = "Oat Milk", barcode = "123")
        )

        val result = repository.getProductByBarcode("123")

        assertTrue(result.isSuccess)
        val food = result.getOrThrow()
        assertNotNull(food)
        assertEquals("Oat Milk", food!!.name)
        assertEquals("Test Brand", food.brand)
    }

    @Test
    fun getProductByBarcode_status0_returnsSuccessWithNull() = runTest {
        coEvery { api.getProductByBarcode("000") } returns OpenFoodFactsResponse(
            status = 0,
            status_verbose = "product not found",
            product = null
        )

        val result = repository.getProductByBarcode("000")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun getProductByBarcode_nullProduct_returnsSuccessWithNull() = runTest {
        coEvery { api.getProductByBarcode("999") } returns OpenFoodFactsResponse(
            status = 1,
            status_verbose = "product found",
            product = null
        )

        val result = repository.getProductByBarcode("999")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    // ── getProductByBarcode — failure ─────────────────────────────────────────

    @Test
    fun getProductByBarcode_apiThrows_returnsFailure() = runTest {
        coEvery { api.getProductByBarcode("bad") } throws RuntimeException("network error")

        val result = repository.getProductByBarcode("bad")

        assertTrue(result.isFailure)
    }

    @Test
    fun getProductByBarcode_apiThrows_reportsNonFatalWithBarcodeContext() = runTest {
        val exception = RuntimeException("timeout")
        coEvery { api.getProductByBarcode("456") } throws exception

        repository.getProductByBarcode("456")

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(
                exception,
                context = "off_barcode",
                extras = mapOf("barcode" to "456")
            )
        }
    }

    @Test
    fun getProductByBarcode_success_doesNotReportNonFatal() = runTest {
        coEvery { api.getProductByBarcode("123") } returns OpenFoodFactsResponse(
            status = 1, status_verbose = null, product = fakeProduct()
        )

        repository.getProductByBarcode("123")

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any(), any()) }
    }

    // ── searchProducts — success ──────────────────────────────────────────────

    @Test
    fun searchProducts_success_returnsMappedFoodItems() = runTest {
        coEvery { api.searchProducts(query = "oat") } returns OpenFoodFactsSearchResponse(
            count = 2,
            page = 1,
            page_size = 20,
            products = listOf(
                fakeProduct(name = "Oat Milk", barcode = "111"),
                fakeProduct(name = "Oat Bar", barcode = "222")
            )
        )

        val result = repository.searchProducts("oat")

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertEquals("Oat Milk", result.getOrThrow()[0].name)
        assertEquals("Oat Bar", result.getOrThrow()[1].name)
    }

    @Test
    fun searchProducts_productWithBlankName_isFiltered() = runTest {
        coEvery { api.searchProducts(query = "test") } returns OpenFoodFactsSearchResponse(
            count = 2,
            page = 1,
            page_size = 20,
            products = listOf(
                fakeProduct(name = "Good Product", barcode = "111"),
                OpenFoodFactsProduct(code = "222", product_name = "  ", brands = null, serving_size = null, nutriments = fakeNutriments())
            )
        )

        val result = repository.searchProducts("test")

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("Good Product", result.getOrThrow()[0].name)
    }

    @Test
    fun searchProducts_productWithNullNutriments_isFiltered() = runTest {
        coEvery { api.searchProducts(query = "test") } returns OpenFoodFactsSearchResponse(
            count = 1,
            page = 1,
            page_size = 20,
            products = listOf(
                OpenFoodFactsProduct(code = "333", product_name = "Mystery Food", brands = null, serving_size = null, nutriments = null)
            )
        )

        val result = repository.searchProducts("test")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    // ── searchProducts — failure ──────────────────────────────────────────────

    @Test
    fun searchProducts_apiThrows_returnsFailure() = runTest {
        coEvery { api.searchProducts(query = "milk") } throws RuntimeException("server down")

        val result = repository.searchProducts("milk")

        assertTrue(result.isFailure)
    }

    @Test
    fun searchProducts_apiThrows_reportsNonFatalWithQueryContext() = runTest {
        val exception = RuntimeException("403 Forbidden")
        coEvery { api.searchProducts(query = "milk") } throws exception

        repository.searchProducts("milk")

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(
                exception,
                context = "off_search",
                extras = mapOf("query" to "milk")
            )
        }
    }

    @Test
    fun searchProducts_success_doesNotReportNonFatal() = runTest {
        coEvery { api.searchProducts(query = "oat") } returns OpenFoodFactsSearchResponse(
            count = 0, page = 1, page_size = 20, products = emptyList()
        )

        repository.searchProducts("oat")

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any(), any()) }
    }
}
