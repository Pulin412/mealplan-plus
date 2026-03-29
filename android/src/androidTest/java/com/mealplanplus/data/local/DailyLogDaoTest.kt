package com.mealplanplus.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealplanplus.data.model.DailyLog
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.LoggedFood
import com.mealplanplus.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailyLogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DailyLogDao
    private lateinit var foodDao: FoodDao
    private lateinit var userDao: UserDao

    private val userId = 1L
    private val today = "2026-03-29"
    private val yesterday = "2026-03-28"

    private val testUser = User(id = userId, email = "test@example.com")

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.dailyLogDao()
        foodDao = db.foodDao()
        userDao = db.userDao()
        userDao.insertUser(testUser)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun insertFood(
        name: String,
        calories: Double = 100.0,
        protein: Double = 5.0,
        carbs: Double = 20.0,
        fat: Double = 2.0
    ): Long = foodDao.insertFood(
        FoodItem(
            name = name,
            caloriesPer100 = calories,
            proteinPer100 = protein,
            carbsPer100 = carbs,
            fatPer100 = fat
        )
    )

    private fun log(date: String = today) = DailyLog(userId = userId, date = date)

    private fun loggedFood(
        foodId: Long,
        quantity: Double = 100.0,
        unit: String = "GRAM",
        slotType: String = "Breakfast",
        date: String = today
    ) = LoggedFood(
        userId = userId,
        logDate = date,
        foodId = foodId,
        quantity = quantity,
        unit = com.mealplanplus.data.model.FoodUnit.valueOf(unit),
        slotType = slotType
    )

    // ── insertLog / getLogByDate ──────────────────────────────────────────────

    @Test
    fun insertLogAndGetByDate() = runTest {
        dao.insertLog(log(today))
        val result = dao.getLogByDate(userId, today)
        assertNotNull(result)
        assertEquals(today, result!!.date)
    }

    @Test
    fun getLogByDateReturnsNullWhenNoLog() = runTest {
        assertNull(dao.getLogByDate(userId, today))
    }

    @Test
    fun getLogByDateFlowEmitsNullThenLog() = runTest {
        dao.insertLog(log(today))
        val result = dao.getLogByDateFlow(userId, today).first()
        assertNotNull(result)
    }

    // ── getLogsByUser ─────────────────────────────────────────────────────────

    @Test
    fun getLogsByUserReturnsAllLogsForUser() = runTest {
        dao.insertLog(log(today))
        dao.insertLog(log(yesterday))
        val logs = dao.getLogsByUser(userId).first()
        assertEquals(2, logs.size)
    }

    @Test
    fun getLogsByUserDoesNotReturnLogsForOtherUser() = runTest {
        val otherUserId = 2L
        userDao.insertUser(User(id = otherUserId, email = "other@example.com"))
        dao.insertLog(log(today))
        dao.insertLog(DailyLog(userId = otherUserId, date = today))
        val logs = dao.getLogsByUser(userId).first()
        assertEquals(1, logs.size)
    }

    // ── insertLoggedFood / getLoggedFoods ─────────────────────────────────────

    @Test
    fun insertLoggedFoodAndRetrieve() = runTest {
        val foodId = insertFood("Oatmeal")
        dao.insertLog(log(today))
        dao.insertLoggedFood(loggedFood(foodId))

        val foods = dao.getLoggedFoods(userId, today).first()
        assertEquals(1, foods.size)
        assertEquals(foodId, foods[0].foodId)
    }

    @Test
    fun getLoggedFoodsReturnsEmptyWhenNoneLogged() = runTest {
        dao.insertLog(log(today))
        assertTrue(dao.getLoggedFoods(userId, today).first().isEmpty())
    }

    @Test
    fun getLoggedFoodsForSlotReturnsCorrectSlot() = runTest {
        val foodId1 = insertFood("Eggs")
        val foodId2 = insertFood("Chicken")
        dao.insertLog(log(today))
        dao.insertLoggedFood(loggedFood(foodId1, slotType = "Breakfast"))
        dao.insertLoggedFood(loggedFood(foodId2, slotType = "Lunch"))

        val breakfastFoods = dao.getLoggedFoodsForSlot(userId, today, "Breakfast")
        assertEquals(1, breakfastFoods.size)
        assertEquals(foodId1, breakfastFoods[0].foodId)
    }

    // ── deleteLoggedFoodById ──────────────────────────────────────────────────

    @Test
    fun deleteLoggedFoodByIdRemovesEntry() = runTest {
        val foodId = insertFood("Banana")
        dao.insertLog(log(today))
        val id = dao.insertLoggedFood(loggedFood(foodId))
        dao.deleteLoggedFoodById(id)

        assertTrue(dao.getLoggedFoods(userId, today).first().isEmpty())
    }

    // ── clearLoggedFoodsForSlot ───────────────────────────────────────────────

    @Test
    fun clearLoggedFoodsForSlotRemovesOnlyThatSlot() = runTest {
        val breakfastFoodId = insertFood("Eggs")
        val lunchFoodId = insertFood("Rice")
        dao.insertLog(log(today))
        dao.insertLoggedFood(loggedFood(breakfastFoodId, slotType = "Breakfast"))
        dao.insertLoggedFood(loggedFood(lunchFoodId, slotType = "Lunch"))

        dao.clearLoggedFoodsForSlot(userId, today, "Breakfast")

        val remaining = dao.getLoggedFoods(userId, today).first()
        assertEquals(1, remaining.size)
        assertEquals("Lunch", remaining[0].slotType)
    }

    @Test
    fun clearLoggedFoodsRemovesAllFoodsForDate() = runTest {
        val foodId = insertFood("Apple")
        dao.insertLog(log(today))
        dao.insertLoggedFood(loggedFood(foodId, slotType = "Breakfast"))
        dao.insertLoggedFood(loggedFood(foodId, slotType = "Lunch"))

        dao.clearLoggedFoods(userId, today)
        assertTrue(dao.getLoggedFoods(userId, today).first().isEmpty())
    }

    // ── updateLoggedFood ──────────────────────────────────────────────────────

    @Test
    fun updateLoggedFoodPersistsChanges() = runTest {
        val foodId = insertFood("Rice")
        dao.insertLog(log(today))
        val id = dao.insertLoggedFood(loggedFood(foodId, quantity = 100.0))
        val existing = dao.getLoggedFoodsForSlot(userId, today, "Breakfast").first()
        dao.updateLoggedFood(existing.copy(quantity = 200.0))

        val updated = dao.getLoggedFoodsForSlot(userId, today, "Breakfast").first()
        assertEquals(200.0, updated.quantity, 0.001)
    }

    // ── getLogsBetweenDates ───────────────────────────────────────────────────

    @Test
    fun getLogsBetweenDatesReturnsBoundaryDays() = runTest {
        dao.insertLog(log("2026-03-25"))
        dao.insertLog(log("2026-03-27"))
        dao.insertLog(log("2026-03-29"))
        dao.insertLog(log("2026-03-31"))

        val result = dao.getLogsBetweenDates(userId, "2026-03-27", "2026-03-29").first()
        val dates = result.map { it.date }
        assertTrue("2026-03-27" in dates)
        assertTrue("2026-03-29" in dates)
        assertFalse("2026-03-25" in dates)
        assertFalse("2026-03-31" in dates)
    }

    // ── getDailyMacroTotals ───────────────────────────────────────────────────

    @Test
    fun getDailyMacroTotalsAggregatesGramsCorrectly() = runTest {
        // Food: 200 cal, 10g protein, 30g carbs, 5g fat per 100g
        val foodId = insertFood("TestFood", calories = 200.0, protein = 10.0, carbs = 30.0, fat = 5.0)
        dao.insertLog(log(today))
        // Log 200g → 400 cal, 20g protein, 60g carbs, 10g fat
        dao.insertLoggedFood(loggedFood(foodId, quantity = 200.0, unit = "GRAM"))

        val totals = dao.getDailyMacroTotals(userId, today, today).first()
        assertEquals(1, totals.size)
        val summary = totals[0]
        assertEquals(today, summary.date)
        assertEquals(400.0, summary.calories, 1.0)
        assertEquals(20.0, summary.protein, 0.5)
        assertEquals(60.0, summary.carbs, 0.5)
        assertEquals(10.0, summary.fat, 0.5)
    }

    @Test
    fun getDailyMacroTotalsReturnsZeroCaloriesForEmptyLog() = runTest {
        dao.insertLog(log(today))
        val totals = dao.getDailyMacroTotals(userId, today, today).first()
        // No logged foods → no rows (grouped by date requires at least one logged_food row)
        assertTrue(totals.isEmpty())
    }

    @Test
    fun getDailyMacroTotalsSumsAcrossMultipleFoodsAndSlots() = runTest {
        val food1 = insertFood("Food1", calories = 100.0, protein = 10.0, carbs = 10.0, fat = 10.0)
        val food2 = insertFood("Food2", calories = 200.0, protein = 20.0, carbs = 20.0, fat = 20.0)
        dao.insertLog(log(today))
        dao.insertLoggedFood(loggedFood(food1, quantity = 100.0, slotType = "Breakfast"))
        dao.insertLoggedFood(loggedFood(food2, quantity = 100.0, slotType = "Lunch"))

        val totals = dao.getDailyMacroTotals(userId, today, today).first()
        assertEquals(1, totals.size)
        assertEquals(300.0, totals[0].calories, 1.0)
    }

    @Test
    fun getDailyMacroTotalsGroupsByDate() = runTest {
        val foodId = insertFood("Egg", calories = 155.0, protein = 13.0, carbs = 1.1, fat = 11.0)
        dao.insertLog(log(today))
        dao.insertLog(log(yesterday))
        dao.insertLoggedFood(loggedFood(foodId, quantity = 100.0, date = today))
        dao.insertLoggedFood(loggedFood(foodId, quantity = 50.0, date = yesterday))

        val totals = dao.getDailyMacroTotals(userId, yesterday, today).first()
        assertEquals(2, totals.size)
        // today: 100g → 155 cal; yesterday: 50g → 77.5 cal
        val todayRow = totals.first { it.date == today }
        val yestRow = totals.first { it.date == yesterday }
        assertEquals(155.0, todayRow.calories, 2.0)
        assertEquals(77.5, yestRow.calories, 2.0)
    }
}
