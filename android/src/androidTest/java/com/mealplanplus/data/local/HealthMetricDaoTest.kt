package com.mealplanplus.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealplanplus.data.model.CustomMetricType
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import com.mealplanplus.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthMetricDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: HealthMetricDao
    private lateinit var userDao: UserDao

    private val userId = 1L
    private val testUser = User(id = userId, email = "test@example.com")

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.healthMetricDao()
        userDao = db.userDao()
        userDao.insertUser(testUser)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun metric(
        value: Double,
        type: MetricType = MetricType.BLOOD_GLUCOSE,
        date: String = "2026-03-01",
        subType: String? = null,
        secondaryValue: Double? = null,
        customTypeId: Long? = null,
        timestamp: Long = System.currentTimeMillis()
    ) = HealthMetric(
        userId = userId,
        date = date,
        metricType = if (customTypeId != null) null else type.name,
        customTypeId = customTypeId,
        value = value,
        subType = subType,
        secondaryValue = secondaryValue,
        timestamp = timestamp
    )

    // ── insertMetric / getMetricById ──────────────────────────────────────────

    @Test
    fun insertMetricAndRetrieveById() = runTest {
        val id = dao.insertMetric(metric(120.0))
        val result = dao.getMetricById(id)
        assertNotNull(result)
        assertEquals(120.0, result!!.value, 0.001)
    }

    @Test
    fun getMetricByIdReturnsNullForMissingId() = runTest {
        assertNull(dao.getMetricById(999L))
    }

    // ── getMetricsForDate ────────────────────────────────────────────────────

    @Test
    fun getMetricsForDateReturnsAllMetricsOnThatDate() = runTest {
        dao.insertMetric(metric(120.0, date = "2026-03-01"))
        dao.insertMetric(metric(80.0, type = MetricType.WEIGHT, date = "2026-03-01"))
        dao.insertMetric(metric(130.0, date = "2026-03-02"))

        val results = dao.getMetricsForDate(userId, "2026-03-01").first()
        assertEquals(2, results.size)
        assertTrue(results.all { it.date == "2026-03-01" })
    }

    @Test
    fun getMetricsForDateReturnsEmptyForDateWithNoData() = runTest {
        val results = dao.getMetricsForDate(userId, "2026-01-01").first()
        assertTrue(results.isEmpty())
    }

    @Test
    fun getMetricsForDateDoesNotReturnOtherUsersData() = runTest {
        val otherId = 2L
        userDao.insertUser(User(id = otherId, email = "other@example.com"))
        dao.insertMetric(metric(100.0, date = "2026-03-01"))
        dao.insertMetric(HealthMetric(userId = otherId, date = "2026-03-01",
            metricType = MetricType.BLOOD_GLUCOSE.name, value = 200.0))

        val results = dao.getMetricsForDate(userId, "2026-03-01").first()
        assertEquals(1, results.size)
        assertEquals(userId, results[0].userId)
    }

    // ── getMetricsByType ──────────────────────────────────────────────────────

    @Test
    fun getMetricsByTypeFiltersCorrectly() = runTest {
        dao.insertMetric(metric(120.0, type = MetricType.BLOOD_GLUCOSE))
        dao.insertMetric(metric(130.0, type = MetricType.BLOOD_GLUCOSE))
        dao.insertMetric(metric(75.0, type = MetricType.WEIGHT))

        val glucose = dao.getMetricsByType(userId, MetricType.BLOOD_GLUCOSE.name).first()
        assertEquals(2, glucose.size)
        assertTrue(glucose.all { it.metricType == MetricType.BLOOD_GLUCOSE.name })
    }

    // ── getMetricsByTypeInRange ────────────────────────────────────────────────

    @Test
    fun getMetricsByTypeInRangeReturnsBoundaryDates() = runTest {
        dao.insertMetric(metric(100.0, date = "2026-03-01"))
        dao.insertMetric(metric(110.0, date = "2026-03-15"))
        dao.insertMetric(metric(120.0, date = "2026-03-29"))
        dao.insertMetric(metric(130.0, date = "2026-04-01"))  // outside

        val results = dao.getMetricsByTypeInRange(
            userId, MetricType.BLOOD_GLUCOSE.name, "2026-03-01", "2026-03-29"
        ).first()
        assertEquals(3, results.size)
        assertFalse(results.any { it.date == "2026-04-01" })
    }

    @Test
    fun getMetricsByTypeInRangeOrderedAscByDate() = runTest {
        dao.insertMetric(metric(130.0, date = "2026-03-29", timestamp = 3000))
        dao.insertMetric(metric(100.0, date = "2026-03-01", timestamp = 1000))
        dao.insertMetric(metric(115.0, date = "2026-03-15", timestamp = 2000))

        val results = dao.getMetricsByTypeInRange(
            userId, MetricType.BLOOD_GLUCOSE.name, "2026-03-01", "2026-03-29"
        ).first()
        assertEquals(listOf("2026-03-01", "2026-03-15", "2026-03-29"), results.map { it.date })
    }

    @Test
    fun getMetricsByTypeInRangeReturnsEmptyOutsideRange() = runTest {
        dao.insertMetric(metric(100.0, date = "2026-01-01"))
        val results = dao.getMetricsByTypeInRange(
            userId, MetricType.BLOOD_GLUCOSE.name, "2026-03-01", "2026-03-29"
        ).first()
        assertTrue(results.isEmpty())
    }

    // ── getRecentMetrics ──────────────────────────────────────────────────────

    @Test
    fun getRecentMetricsRespectsLimit() = runTest {
        repeat(10) { i ->
            dao.insertMetric(metric(100.0 + i, date = "2026-03-${String.format("%02d", i + 1)}"))
        }
        val results = dao.getRecentMetrics(userId, 3).first()
        assertEquals(3, results.size)
    }

    @Test
    fun getRecentMetricsOrderedByDateDescending() = runTest {
        dao.insertMetric(metric(100.0, date = "2026-03-01", timestamp = 1000))
        dao.insertMetric(metric(110.0, date = "2026-03-15", timestamp = 2000))
        dao.insertMetric(metric(120.0, date = "2026-03-29", timestamp = 3000))

        val results = dao.getRecentMetrics(userId, 10).first()
        assertEquals("2026-03-29", results[0].date)
        assertEquals("2026-03-01", results[2].date)
    }

    // ── updateMetric ──────────────────────────────────────────────────────────

    @Test
    fun updateMetricPersistsChanges() = runTest {
        val id = dao.insertMetric(metric(120.0))
        val existing = dao.getMetricById(id)!!
        dao.updateMetric(existing.copy(value = 95.0, subType = "POST_MEAL"))

        val updated = dao.getMetricById(id)!!
        assertEquals(95.0, updated.value, 0.001)
        assertEquals("POST_MEAL", updated.subType)
    }

    // ── deleteMetric ──────────────────────────────────────────────────────────

    @Test
    fun deleteMetricRemovesEntry() = runTest {
        val id = dao.insertMetric(metric(120.0))
        val existing = dao.getMetricById(id)!!
        dao.deleteMetric(existing)
        assertNull(dao.getMetricById(id))
    }

    // ── blood pressure secondary value ────────────────────────────────────────

    @Test
    fun bloodPressureStoresSecondaryValue() = runTest {
        val id = dao.insertMetric(
            metric(120.0, type = MetricType.BLOOD_PRESSURE, secondaryValue = 80.0)
        )
        val result = dao.getMetricById(id)!!
        assertEquals(120.0, result.value, 0.001)
        assertEquals(80.0, result.secondaryValue!!, 0.001)
    }

    // ── custom metric types ───────────────────────────────────────────────────

    @Test
    fun insertCustomTypeAndRetrieve() = runTest {
        val id = dao.insertCustomType(
            CustomMetricType(userId = userId, name = "Steps", unit = "count")
        )
        val types = dao.getActiveCustomTypes(userId).first()
        assertEquals(1, types.size)
        assertEquals(id, types[0].id)
        assertEquals("Steps", types[0].name)
    }

    @Test
    fun getActiveCustomTypesExcludesInactiveTypes() = runTest {
        val activeId = dao.insertCustomType(
            CustomMetricType(userId = userId, name = "Steps", unit = "count", isActive = true)
        )
        val inactiveId = dao.insertCustomType(
            CustomMetricType(userId = userId, name = "Mood", unit = "score", isActive = false)
        )
        val active = dao.getActiveCustomTypes(userId).first()
        assertEquals(1, active.size)
        assertEquals(activeId, active[0].id)
    }

    @Test
    fun getAllCustomTypesIncludesInactiveTypes() = runTest {
        dao.insertCustomType(CustomMetricType(userId = userId, name = "Steps", unit = "count", isActive = true))
        dao.insertCustomType(CustomMetricType(userId = userId, name = "Mood", unit = "score", isActive = false))
        val all = dao.getAllCustomTypes(userId).first()
        assertEquals(2, all.size)
    }

    @Test
    fun updateCustomTypeSoftDelete() = runTest {
        val id = dao.insertCustomType(
            CustomMetricType(userId = userId, name = "Waist", unit = "cm", isActive = true)
        )
        val existing = dao.getActiveCustomTypes(userId).first().first { it.id == id }
        dao.updateCustomType(existing.copy(isActive = false))

        val active = dao.getActiveCustomTypes(userId).first()
        assertTrue(active.none { it.id == id })
        // Still appears in getAllCustomTypes
        val all = dao.getAllCustomTypes(userId).first()
        assertTrue(all.any { it.id == id })
    }

    @Test
    fun deleteCustomTypeRemovesIt() = runTest {
        val id = dao.insertCustomType(
            CustomMetricType(userId = userId, name = "Steps", unit = "count")
        )
        val type = dao.getActiveCustomTypes(userId).first().first { it.id == id }
        dao.deleteCustomType(type)
        assertTrue(dao.getAllCustomTypes(userId).first().isEmpty())
    }

    // ── custom metric logging ─────────────────────────────────────────────────

    @Test
    fun logCustomMetricAndRetrieveByCustomType() = runTest {
        val typeId = dao.insertCustomType(
            CustomMetricType(userId = userId, name = "Steps", unit = "count")
        )
        dao.insertMetric(metric(8000.0, customTypeId = typeId))
        dao.insertMetric(metric(9500.0, customTypeId = typeId))

        val results = dao.getMetricsByCustomType(userId, typeId).first()
        assertEquals(2, results.size)
        assertTrue(results.all { it.customTypeId == typeId })
    }

    @Test
    fun customMetricHasNullMetricType() = runTest {
        val typeId = dao.insertCustomType(
            CustomMetricType(userId = userId, name = "Steps", unit = "count")
        )
        val id = dao.insertMetric(metric(8000.0, customTypeId = typeId))
        val result = dao.getMetricById(id)!!
        assertNull(result.metricType)
        assertEquals(typeId, result.customTypeId)
    }

    // ── custom type range and min/max ─────────────────────────────────────────

    @Test
    fun customTypeStoresMinMaxValues() = runTest {
        val id = dao.insertCustomType(
            CustomMetricType(userId = userId, name = "SpO2", unit = "%", minValue = 95.0, maxValue = 100.0)
        )
        val type = dao.getCustomTypeById(id)!!
        assertEquals(95.0, type.minValue!!, 0.001)
        assertEquals(100.0, type.maxValue!!, 0.001)
    }

    @Test
    fun customTypeOrderedByName() = runTest {
        dao.insertCustomType(CustomMetricType(userId = userId, name = "Zinc", unit = "mg"))
        dao.insertCustomType(CustomMetricType(userId = userId, name = "Alpha", unit = "u"))
        dao.insertCustomType(CustomMetricType(userId = userId, name = "Magnesium", unit = "mg"))

        val names = dao.getActiveCustomTypes(userId).first().map { it.name }
        assertEquals(listOf("Alpha", "Magnesium", "Zinc"), names)
    }

    // ── unsync helpers ────────────────────────────────────────────────────────

    @Test
    fun getUnsyncedMetricsReturnsOnlyUnsyncedOnes() = runTest {
        val now = System.currentTimeMillis()
        dao.insertMetric(metric(100.0).copy(syncedAt = null))   // unsynced
        dao.insertMetric(metric(110.0).copy(syncedAt = now, updatedAt = now - 1000)) // synced
        dao.insertMetric(metric(120.0).copy(syncedAt = now - 2000, updatedAt = now)) // updated after sync

        val unsynced = dao.getUnsyncedMetrics(userId)
        assertEquals(2, unsynced.size) // syncedAt=null and updatedAt > syncedAt
    }
}
