package com.mealplanplus.ui.screens.health

import com.mealplanplus.data.healthconnect.ActivityDaySummary
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.GlucoseSubType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.repository.HealthConnectRepository
import com.mealplanplus.data.repository.HealthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import com.mealplanplus.util.toEpochMs
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HealthViewModelTest {

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

    private lateinit var healthRepo: HealthRepository
    private lateinit var healthConnectRepo: HealthConnectRepository
    private lateinit var viewModel: HealthViewModel

    private fun metric(id: Long, value: Double, type: MetricType = MetricType.BLOOD_GLUCOSE,
                       subType: String? = null, date: Long = LocalDate.of(2026, 3, 1).toEpochMs()) =
        HealthMetric(id = id, userId = 1, date = date, metricType = type.name, value = value, subType = subType)

    @Before
    fun setup() {
        healthRepo = mockk(relaxed = true)
        healthConnectRepo = mockk(relaxed = true)

        every { healthRepo.getMetricsByTypeInRange(any(), any(), any()) } returns flowOf(emptyList())
        every { healthRepo.getMetricsByCustomType(any()) } returns flowOf(emptyList())
        every { healthRepo.getActiveCustomTypes() } returns flowOf(emptyList())
        every { healthConnectRepo.isAvailable } returns false
        coEvery { healthConnectRepo.hasPermissions() } returns false
        coEvery { healthConnectRepo.getActivityHistory(any(), any()) } returns emptyList<ActivityDaySummary>()

        viewModel = HealthViewModel(healthRepo, healthConnectRepo)
    }

    @After
    fun tearDown() {
        // Main dispatcher stays set (class-level @BeforeClass) — nothing to reset here.
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial selectedMetricType is BLOOD_GLUCOSE`() {
        assertEquals(MetricType.BLOOD_GLUCOSE, viewModel.uiState.value.selectedMetricType)
    }

    @Test
    fun `initial metrics list is empty`() {
        assertTrue(viewModel.uiState.value.metrics.isEmpty())
    }

    @Test
    fun `initial stats is null when no metrics`() {
        assertNull(viewModel.uiState.value.stats)
    }

    // ── selectMetricType ──────────────────────────────────────────────────────

    @Test
    fun `selectMetricType updates selectedMetricType and clears customTypeId`() = runTest {
        viewModel.selectMetricType(MetricType.WEIGHT)
        val state = viewModel.uiState.value
        assertEquals(MetricType.WEIGHT, state.selectedMetricType)
        assertNull(state.selectedCustomTypeId)
    }

    @Test
    fun `selectCustomType clears selectedMetricType`() = runTest {
        every { healthRepo.getMetricsByCustomType(42L) } returns flowOf(emptyList())
        viewModel.selectCustomType(42L)
        val state = viewModel.uiState.value
        assertEquals(42L, state.selectedCustomTypeId)
        assertNull(state.selectedMetricType)
    }

    // ── computeStats ──────────────────────────────────────────────────────────

    @Test
    fun `stats are null for empty metric list`() = runTest {
        every { healthRepo.getMetricsByTypeInRange(any(), any(), any()) } returns flowOf(emptyList())
        viewModel.selectMetricType(MetricType.WEIGHT)
        assertNull(viewModel.uiState.value.stats)
    }

    @Test
    fun `stats avg, min, max computed correctly`() = runTest {
        val metrics = listOf(
            metric(1, 80.0, MetricType.BLOOD_GLUCOSE),
            metric(2, 120.0, MetricType.BLOOD_GLUCOSE),
            metric(3, 160.0, MetricType.BLOOD_GLUCOSE)
        )
        every { healthRepo.getMetricsByTypeInRange(MetricType.BLOOD_GLUCOSE, any(), any()) } returns flowOf(metrics)
        viewModel.selectMetricType(MetricType.BLOOD_GLUCOSE)

        val stats = viewModel.uiState.value.stats
        assertNotNull(stats)
        assertEquals(120.0, stats!!.avg, 0.001)
        assertEquals(80.0, stats.min, 0.001)
        assertEquals(160.0, stats.max, 0.001)
    }

    @Test
    fun `single metric produces stats equal to that value`() = runTest {
        val metrics = listOf(metric(1, 95.0, MetricType.WEIGHT))
        every { healthRepo.getMetricsByTypeInRange(MetricType.WEIGHT, any(), any()) } returns flowOf(metrics)
        viewModel.selectMetricType(MetricType.WEIGHT)

        val stats = viewModel.uiState.value.stats!!
        assertEquals(95.0, stats.avg, 0.001)
        assertEquals(95.0, stats.min, 0.001)
        assertEquals(95.0, stats.max, 0.001)
    }

    // ── A1c estimation ────────────────────────────────────────────────────────

    @Test
    fun `estimatedA1c uses formula (avg + 46_7) divided by 28_7`() = runTest {
        // avg = 120 mg/dL → A1c = (120 + 46.7) / 28.7 = 5.809...
        val metrics = listOf(
            metric(1, 100.0, MetricType.BLOOD_GLUCOSE),
            metric(2, 140.0, MetricType.BLOOD_GLUCOSE)
        )
        every { healthRepo.getMetricsByTypeInRange(MetricType.BLOOD_GLUCOSE, any(), any()) } returns flowOf(metrics)
        viewModel.selectMetricType(MetricType.BLOOD_GLUCOSE)

        val expected = (120.0 + 46.7) / 28.7
        assertEquals(expected, viewModel.uiState.value.estimatedA1c!!, 0.001)
    }

    @Test
    fun `estimatedA1c is null for non-glucose metric type`() = runTest {
        val metrics = listOf(metric(1, 70.0, MetricType.WEIGHT))
        every { healthRepo.getMetricsByTypeInRange(MetricType.WEIGHT, any(), any()) } returns flowOf(metrics)
        viewModel.selectMetricType(MetricType.WEIGHT)
        assertNull(viewModel.uiState.value.estimatedA1c)
    }

    @Test
    fun `estimatedA1c is null when no glucose metrics`() = runTest {
        every { healthRepo.getMetricsByTypeInRange(MetricType.BLOOD_GLUCOSE, any(), any()) } returns flowOf(emptyList())
        viewModel.selectMetricType(MetricType.BLOOD_GLUCOSE)
        assertNull(viewModel.uiState.value.estimatedA1c)
    }

    // ── timeInRangePercent ────────────────────────────────────────────────────

    @Test
    fun `timeInRangePercent counts values between 80 and 180 inclusive`() = runTest {
        val metrics = listOf(
            metric(1, 70.0),   // low — out of range
            metric(2, 80.0),   // lower bound — in range
            metric(3, 140.0),  // in range
            metric(4, 180.0),  // upper bound — in range
            metric(5, 200.0)   // high — out of range
        )
        every { healthRepo.getMetricsByTypeInRange(MetricType.BLOOD_GLUCOSE, any(), any()) } returns flowOf(metrics)
        viewModel.selectMetricType(MetricType.BLOOD_GLUCOSE)
        // 3 out of 5 = 60%
        assertEquals(60, viewModel.uiState.value.timeInRangePercent)
    }

    @Test
    fun `timeInRangePercent is null for non-glucose type`() = runTest {
        every { healthRepo.getMetricsByTypeInRange(MetricType.WEIGHT, any(), any()) } returns flowOf(
            listOf(metric(1, 70.0, MetricType.WEIGHT))
        )
        viewModel.selectMetricType(MetricType.WEIGHT)
        assertNull(viewModel.uiState.value.timeInRangePercent)
    }

    // ── bgDistribution ────────────────────────────────────────────────────────

    @Test
    fun `bgDistribution buckets are computed correctly`() = runTest {
        // 10 readings: 2 low (<70), 4 in-range (70-140), 2 elevated (140-200), 2 high (>200)
        val metrics = listOf(
            metric(1, 60.0), metric(2, 65.0),          // low
            metric(3, 90.0), metric(4, 100.0),          // in-range (70-140)
            metric(5, 110.0), metric(6, 120.0),         // in-range (70-140)
            metric(7, 150.0), metric(8, 170.0),         // elevated (140-200)
            metric(9, 210.0), metric(10, 250.0)         // high (>200)
        )
        every { healthRepo.getMetricsByTypeInRange(MetricType.BLOOD_GLUCOSE, any(), any()) } returns flowOf(metrics)
        viewModel.selectMetricType(MetricType.BLOOD_GLUCOSE)

        val dist = viewModel.uiState.value.bgDistribution
        assertNotNull(dist)
        assertEquals(20, dist!!.lowPercent)       // 2/10
        assertEquals(40, dist.inRangePercent)     // 4/10 (70-140)
        assertEquals(20, dist.elevatedPercent)    // 2/10 (140-200)
        assertEquals(20, dist.highPercent)        // 2/10
    }

    @Test
    fun `bgDistribution is null for non-glucose type`() = runTest {
        every { healthRepo.getMetricsByTypeInRange(MetricType.WEIGHT, any(), any()) } returns flowOf(
            listOf(metric(1, 70.0, MetricType.WEIGHT))
        )
        viewModel.selectMetricType(MetricType.WEIGHT)
        assertNull(viewModel.uiState.value.bgDistribution)
    }

    // ── log sheet ─────────────────────────────────────────────────────────────

    @Test
    fun `showLogSheet opens sheet with reset values`() {
        viewModel.updateLogBgValue("120")
        viewModel.showLogSheet()
        val state = viewModel.uiState.value
        assertTrue(state.showLogSheet)
        assertEquals("", state.logBgValue)
        assertEquals(GlucoseSubType.FASTING.name, state.logBgSubType)
    }

    @Test
    fun `hideLogSheet closes sheet`() {
        viewModel.showLogSheet()
        viewModel.hideLogSheet()
        assertFalse(viewModel.uiState.value.showLogSheet)
    }

    @Test
    fun `updateLogBgValue updates state`() {
        viewModel.updateLogBgValue("115")
        assertEquals("115", viewModel.uiState.value.logBgValue)
    }

    @Test
    fun `updateLogBgSubType updates subtype`() {
        viewModel.updateLogBgSubType(GlucoseSubType.POST_MEAL.name)
        assertEquals(GlucoseSubType.POST_MEAL.name, viewModel.uiState.value.logBgSubType)
    }

    @Test
    fun `updateLogDate updates logDate`() {
        val date = LocalDate.of(2026, 3, 15)
        viewModel.updateLogDate(date)
        assertEquals(date, viewModel.uiState.value.logDate)
    }

    // ── custom metric type validation ─────────────────────────────────────────

    @Test
    fun `addCustomType with blank name sets error`() = runTest {
        viewModel.updateNewCustomTypeName("")
        viewModel.updateNewCustomTypeUnit("kg")
        viewModel.addCustomType()
        assertEquals("Name and unit required", viewModel.uiState.value.error)
    }

    @Test
    fun `addCustomType with blank unit sets error`() = runTest {
        viewModel.updateNewCustomTypeName("Waist")
        viewModel.updateNewCustomTypeUnit("")
        viewModel.addCustomType()
        assertEquals("Name and unit required", viewModel.uiState.value.error)
    }

    @Test
    fun `addCustomType with valid inputs calls repository`() = runTest {
        coEvery { healthRepo.addCustomType("Waist", "cm", 60.0, 100.0) } returns 99L
        every { healthRepo.getMetricsByCustomType(99L) } returns flowOf(emptyList())

        viewModel.updateNewCustomTypeName("Waist")
        viewModel.updateNewCustomTypeUnit("cm")
        viewModel.updateNewCustomTypeMin("60")
        viewModel.updateNewCustomTypeMax("100")
        viewModel.addCustomType()

        coVerify { healthRepo.addCustomType("Waist", "cm", 60.0, 100.0) }
    }

    // ── deleteCustomType (soft delete) ────────────────────────────────────────

    @Test
    fun `deleteCustomType marks type inactive`() = runTest {
        val customType = CustomMetricType(id = 5, userId = 1, name = "Steps", unit = "count")
        viewModel.deleteCustomType(customType)
        coVerify { healthRepo.updateCustomType(customType.copy(isActive = false)) }
    }

    // ── clearError ────────────────────────────────────────────────────────────

    @Test
    fun `clearError sets error to null`() = runTest {
        viewModel.updateNewCustomTypeName("")
        viewModel.updateNewCustomTypeUnit("")
        viewModel.addCustomType() // triggers error
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // ── selectMetricViewType ──────────────────────────────────────────────────

    @Test
    fun `selectMetricViewType updates metricViewType and resets offset`() = runTest {
        viewModel.shiftMetricPeriod(-2)
        viewModel.selectMetricViewType(PeriodViewType.MONTH)
        assertEquals(PeriodViewType.MONTH, viewModel.uiState.value.metricViewType)
        assertEquals(0, viewModel.uiState.value.metricPeriodOffset)
    }

    @Test
    fun `shiftMetricPeriod decrements offset and caps at zero when going forward`() = runTest {
        viewModel.shiftMetricPeriod(-1)
        assertEquals(-1, viewModel.uiState.value.metricPeriodOffset)
        viewModel.shiftMetricPeriod(1)
        assertEquals(0, viewModel.uiState.value.metricPeriodOffset)
        viewModel.shiftMetricPeriod(1)
        assertEquals(0, viewModel.uiState.value.metricPeriodOffset) // stays at 0, no future
    }
}
