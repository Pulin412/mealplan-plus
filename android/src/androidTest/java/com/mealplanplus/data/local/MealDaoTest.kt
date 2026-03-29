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
class MealDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: MealDao
    private lateinit var foodDao: FoodDao

    private val userId = 1L

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.mealDao()
        foodDao = db.foodDao()

        db.userDao().insertUser(User(id = userId, email = "t@t.com", passwordHash = "x", displayName = "T"))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertMeal(name: String, slot: String = "BREAKFAST"): Long =
        dao.insertMeal(Meal(userId = userId, name = name, slotType = slot))

    private suspend fun insertFood(name: String, cal: Double = 100.0): Long =
        foodDao.insertFood(FoodItem(name = name, caloriesPer100 = cal, proteinPer100 = 5.0, carbsPer100 = 10.0, fatPer100 = 2.0))

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    fun insertAndGetMealById() = runTest {
        val id = insertMeal("Oatmeal Bowl")
        val meal = dao.getMealById(id)
        assertNotNull(meal)
        assertEquals("Oatmeal Bowl", meal!!.name)
    }

    @Test
    fun getMealsByUser_returnsInNameOrder() = runTest {
        insertMeal("Zebra Steak")
        insertMeal("Apple Oats")
        insertMeal("Mango Smoothie")

        val meals = dao.getMealsByUser(userId).first()
        assertEquals(3, meals.size)
        assertEquals("Apple Oats", meals[0].name)
        assertEquals("Mango Smoothie", meals[1].name)
        assertEquals("Zebra Steak", meals[2].name)
    }

    @Test
    fun getMealsByUserAndSlot_filtersCorrectly() = runTest {
        insertMeal("Breakfast Bowl", "BREAKFAST")
        insertMeal("Lunch Salad", "LUNCH")
        insertMeal("Protein Shake", "BREAKFAST")

        val breakfastMeals = dao.getMealsByUserAndSlot(userId, "BREAKFAST").first()
        assertEquals(2, breakfastMeals.size)
        assertTrue(breakfastMeals.all { it.slotType == "BREAKFAST" })
    }

    @Test
    fun updateMeal_changesFields() = runTest {
        val id = insertMeal("Old Name")
        val meal = dao.getMealById(id)!!
        dao.updateMeal(meal.copy(name = "New Name", description = "Updated"))
        val updated = dao.getMealById(id)!!
        assertEquals("New Name", updated.name)
        assertEquals("Updated", updated.description)
    }

    @Test
    fun deleteMeal_removesFromDb() = runTest {
        val id = insertMeal("To Delete")
        dao.deleteMeal(dao.getMealById(id)!!)
        assertNull(dao.getMealById(id))
    }

    // ── Meal food items ───────────────────────────────────────────────────────

    @Test
    fun insertMealFoodItem_and_getMealFoodItems() = runTest {
        val mealId = insertMeal("Chicken Rice")
        val foodId = insertFood("Chicken Breast")

        dao.insertMealFoodItem(MealFoodItem(mealId = mealId, foodId = foodId, quantity = 150.0, unit = FoodUnit.GRAM))

        val items = dao.getMealFoodItems(mealId)
        assertEquals(1, items.size)
        assertEquals(foodId, items[0].foodId)
        assertEquals(150.0, items[0].quantity, 0.001)
    }

    @Test
    fun insertMultipleMealFoodItems() = runTest {
        val mealId = insertMeal("Mixed Bowl")
        val f1 = insertFood("Rice")
        val f2 = insertFood("Veggies")

        dao.insertMealFoodItems(listOf(
            MealFoodItem(mealId = mealId, foodId = f1, quantity = 100.0, unit = FoodUnit.GRAM),
            MealFoodItem(mealId = mealId, foodId = f2, quantity = 50.0, unit = FoodUnit.GRAM)
        ))

        assertEquals(2, dao.getMealFoodItems(mealId).size)
    }

    @Test
    fun removeFoodFromMeal_removesSingleFood() = runTest {
        val mealId = insertMeal("Two Food Meal")
        val f1 = insertFood("Food A")
        val f2 = insertFood("Food B")

        dao.insertMealFoodItem(MealFoodItem(mealId = mealId, foodId = f1, quantity = 100.0, unit = FoodUnit.GRAM))
        dao.insertMealFoodItem(MealFoodItem(mealId = mealId, foodId = f2, quantity = 100.0, unit = FoodUnit.GRAM))

        dao.removeFoodFromMeal(mealId, f1)
        val remaining = dao.getMealFoodItems(mealId)
        assertEquals(1, remaining.size)
        assertEquals(f2, remaining[0].foodId)
    }

    @Test
    fun clearMealFoodItems_removesAll() = runTest {
        val mealId = insertMeal("Full Meal")
        val f1 = insertFood("A")
        val f2 = insertFood("B")
        dao.insertMealFoodItem(MealFoodItem(mealId = mealId, foodId = f1, quantity = 100.0, unit = FoodUnit.GRAM))
        dao.insertMealFoodItem(MealFoodItem(mealId = mealId, foodId = f2, quantity = 100.0, unit = FoodUnit.GRAM))

        dao.clearMealFoodItems(mealId)
        assertTrue(dao.getMealFoodItems(mealId).isEmpty())
    }

    @Test
    fun deleteMealFoodItem_removesSpecificItem() = runTest {
        val mealId = insertMeal("Specific Delete")
        val foodId = insertFood("Specific Food")
        val item = MealFoodItem(mealId = mealId, foodId = foodId, quantity = 100.0, unit = FoodUnit.GRAM)
        dao.insertMealFoodItem(item)

        dao.deleteMealFoodItem(item)
        assertTrue(dao.getMealFoodItems(mealId).isEmpty())
    }

    // ── Cascade delete ────────────────────────────────────────────────────────

    @Test
    fun deleteMeal_cascadesMealFoodItems() = runTest {
        val mealId = insertMeal("Cascade Meal")
        val foodId = insertFood("Cascade Food")
        dao.insertMealFoodItem(MealFoodItem(mealId = mealId, foodId = foodId, quantity = 100.0, unit = FoodUnit.GRAM))

        dao.deleteMeal(dao.getMealById(mealId)!!)
        assertTrue(dao.getMealFoodItems(mealId).isEmpty())
    }

    // ── Slot-level behaviour ──────────────────────────────────────────────────

    @Test
    fun multipleSlotTypes_areStoredCorrectly() = runTest {
        val slots = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK")
        slots.forEach { insertMeal("Meal for $it", it) }

        slots.forEach { slot ->
            val meals = dao.getMealsByUserAndSlot(userId, slot).first()
            assertEquals(1, meals.size)
            assertEquals(slot, meals[0].slotType)
        }
    }
}
