package com.mealplanplus.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealplanplus.data.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlanDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PlanDao
    private lateinit var dietDao: DietDao

    private val userId = 1L

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.planDao()
        dietDao = db.dietDao()

        db.userDao().insertUser(User(id = userId, email = "t@t.com", passwordHash = "x", displayName = "T"))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertDiet(name: String): Long =
        dietDao.insertDiet(Diet(userId = userId, name = name))

    private suspend fun upsert(date: String, dietId: Long?, completed: Boolean = false) =
        dao.upsertPlan(Plan(userId = userId, date = date, dietId = dietId, isCompleted = completed))

    // ── Basic upsert / get ────────────────────────────────────────────────────

    @Test
    fun upsertAndGetPlanForDate() = runTest {
        val dietId = insertDiet("Low Carb")
        upsert("2025-06-01", dietId)

        val plan = dao.getPlanForDate(userId, "2025-06-01")
        assertNotNull(plan)
        assertEquals(dietId, plan!!.dietId)
        assertFalse(plan.isCompleted)
    }

    @Test
    fun upsertPlan_updatesExistingRow() = runTest {
        val d1 = insertDiet("Diet A")
        val d2 = insertDiet("Diet B")
        upsert("2025-06-01", d1)
        upsert("2025-06-01", d2) // replace

        val plan = dao.getPlanForDate(userId, "2025-06-01")
        assertEquals(d2, plan!!.dietId)
    }

    @Test
    fun getPlanForDate_returnsNullWhenMissing() = runTest {
        assertNull(dao.getPlanForDate(userId, "2099-01-01"))
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    fun deletePlan_removesRow() = runTest {
        val dietId = insertDiet("To Remove")
        upsert("2025-06-10", dietId)
        dao.deletePlan(userId, "2025-06-10")
        assertNull(dao.getPlanForDate(userId, "2025-06-10"))
    }

    // ── Range queries ─────────────────────────────────────────────────────────

    @Test
    fun getPlansInRange_returnsOnlyDatesInRange() = runTest {
        val dietId = insertDiet("Diet")
        upsert("2025-06-01", dietId)
        upsert("2025-06-15", dietId)
        upsert("2025-07-01", dietId) // outside range

        val plans = dao.getPlansInRange(userId, "2025-06-01", "2025-06-30").first()
        assertEquals(2, plans.size)
        assertTrue(plans.none { it.date == "2025-07-01" })
    }

    @Test
    fun getPlansInRange_emptyWhenNoPlanInRange() = runTest {
        val plans = dao.getPlansInRange(userId, "2025-01-01", "2025-01-31").first()
        assertTrue(plans.isEmpty())
    }

    // ── Plans with diet names (JOIN) ──────────────────────────────────────────

    @Test
    fun getPlansWithDietNames_includesDietName() = runTest {
        val dietId = insertDiet("High Protein")
        upsert("2025-06-05", dietId)

        val plans = dao.getPlansWithDietNames(userId, "2025-06-01", "2025-06-30").first()
        assertEquals(1, plans.size)
        assertEquals("High Protein", plans[0].dietName)
        assertEquals("2025-06-05", plans[0].date)
    }

    @Test
    fun getPlansWithDietNames_nullDietIdReturnsNullName() = runTest {
        upsert("2025-06-20", dietId = null)

        val plans = dao.getPlansWithDietNames(userId, "2025-06-01", "2025-06-30").first()
        assertEquals(1, plans.size)
        assertNull(plans[0].dietName)
    }

    // ── Diet for date ─────────────────────────────────────────────────────────

    @Test
    fun getDietForDate_returnsDietWhenPlanExists() = runTest {
        val dietId = insertDiet("Keto")
        upsert("2025-06-12", dietId)

        val diet = dao.getDietForDate(userId, "2025-06-12")
        assertNotNull(diet)
        assertEquals("Keto", diet!!.name)
    }

    @Test
    fun getDietForDate_returnsNullWhenNoPlan() = runTest {
        assertNull(dao.getDietForDate(userId, "2025-06-13"))
    }

    @Test
    fun getDietForDate_returnsNullWhenPlanHasNullDiet() = runTest {
        upsert("2025-06-14", dietId = null)
        assertNull(dao.getDietForDate(userId, "2025-06-14"))
    }

    // ── isCompleted flag ──────────────────────────────────────────────────────

    @Test
    fun completedPlan_isStoredAndRetrieved() = runTest {
        val dietId = insertDiet("Completed Diet")
        upsert("2025-06-25", dietId, completed = true)

        val plan = dao.getPlanForDate(userId, "2025-06-25")
        assertTrue(plan!!.isCompleted)
    }

    // ── getPlansByUser ────────────────────────────────────────────────────────

    @Test
    fun getPlansByUser_returnsAllPlansDescending() = runTest {
        val dietId = insertDiet("D")
        upsert("2025-06-01", dietId)
        upsert("2025-06-10", dietId)
        upsert("2025-06-20", dietId)

        val plans = dao.getPlansByUser(userId).first()
        assertEquals(3, plans.size)
        // Ordered DESC by date
        assertEquals("2025-06-20", plans[0].date)
        assertEquals("2025-06-01", plans[2].date)
    }
}
