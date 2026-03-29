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
class DietDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: DietDao
    private lateinit var mealDao: MealDao
    private lateinit var foodDao: FoodDao

    private val userId = 1L

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.dietDao()
        mealDao = db.mealDao()
        foodDao = db.foodDao()

        // Insert seed user row (required by FK)
        db.userDao().run {
            runTest { insertUser(User(id = userId, email = "test@test.com", passwordHash = "x", displayName = "Test")) }
        }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertDiet(name: String, userId: Long = this.userId): Long =
        dao.insertDiet(Diet(userId = userId, name = name))

    private suspend fun insertMeal(name: String, slotType: String = "BREAKFAST"): Long =
        mealDao.insertMeal(Meal(userId = userId, name = name, slotType = slotType))

    private suspend fun insertFood(
        name: String, calories: Double = 200.0
    ): Long = foodDao.insertFood(
        FoodItem(name = name, caloriesPer100 = calories, proteinPer100 = 20.0, carbsPer100 = 10.0, fatPer100 = 5.0)
    )

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    fun insertAndGetDiet() = runTest {
        val id = insertDiet("Low Carb")
        val diet = dao.getDietById(id)
        assertNotNull(diet)
        assertEquals("Low Carb", diet!!.name)
        assertEquals(userId, diet.userId)
    }

    @Test
    fun getDietsByUser_returnsOnlyUserDiets() = runTest {
        insertDiet("Diet A", userId = 1L)
        insertDiet("Diet B", userId = 1L)

        // Insert user 2 and their diet
        db.userDao().insertUser(User(id = 2L, email = "b@b.com", passwordHash = "x", displayName = "B"))
        insertDiet("Other User Diet", userId = 2L)

        val diets = dao.getDietsByUser(1L).first()
        assertEquals(2, diets.size)
        assertTrue(diets.none { it.userId != 1L })
    }

    @Test
    fun updateDiet_changesName() = runTest {
        val id = insertDiet("Old Name")
        val diet = dao.getDietById(id)!!
        dao.updateDiet(diet.copy(name = "New Name"))
        assertEquals("New Name", dao.getDietById(id)!!.name)
    }

    @Test
    fun deleteDiet_removesFromDb() = runTest {
        val id = insertDiet("To Delete")
        dao.deleteDiet(dao.getDietById(id)!!)
        assertNull(dao.getDietById(id))
    }

    @Test
    fun getDietCountByUser_returnsCorrectCount() = runTest {
        insertDiet("D1")
        insertDiet("D2")
        insertDiet("D3")
        assertEquals(3, dao.getDietCountByUser(userId))
    }

    // ── Diet meals ────────────────────────────────────────────────────────────

    @Test
    fun insertDietMeal_and_getDietMeals() = runTest {
        val dietId = insertDiet("Plan A")
        val mealId = insertMeal("Breakfast Meal")
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "BREAKFAST", mealId = mealId))

        val meals = dao.getDietMeals(dietId)
        assertEquals(1, meals.size)
        assertEquals("BREAKFAST", meals[0].slotType)
        assertEquals(mealId, meals[0].mealId)
    }

    @Test
    fun clearDietMeals_removesAllSlots() = runTest {
        val dietId = insertDiet("Plan B")
        val mId = insertMeal("M")
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "BREAKFAST", mealId = mId))
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "LUNCH", mealId = mId))

        dao.clearDietMeals(dietId)
        assertTrue(dao.getDietMeals(dietId).isEmpty())
    }

    @Test
    fun removeMealFromDiet_removesSingleSlot() = runTest {
        val dietId = insertDiet("Plan C")
        val mId = insertMeal("M")
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "BREAKFAST", mealId = mId))
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "LUNCH", mealId = mId))

        dao.removeMealFromDiet(dietId, "BREAKFAST")
        val meals = dao.getDietMeals(dietId)
        assertEquals(1, meals.size)
        assertEquals("LUNCH", meals[0].slotType)
    }

    @Test
    fun getDietMeal_returnsCorrectSlot() = runTest {
        val dietId = insertDiet("Plan D")
        val mId = insertMeal("Lunch Meal", "LUNCH")
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "LUNCH", mealId = mId))

        val dm = dao.getDietMeal(dietId, "LUNCH")
        assertNotNull(dm)
        assertEquals(mId, dm!!.mealId)
    }

    @Test
    fun updateDietMealInstructions_persistsInstructions() = runTest {
        val dietId = insertDiet("Plan E")
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "DINNER", mealId = null))
        dao.updateDietMealInstructions(dietId, "DINNER", "Eat light after 7pm")
        val dm = dao.getDietMeal(dietId, "DINNER")
        assertEquals("Eat light after 7pm", dm!!.instructions)
    }

    // ── Full macro summary ────────────────────────────────────────────────────

    @Test
    fun getDietsWithFullSummary_computesCaloriesFromFoods() = runTest {
        val dietId = insertDiet("Calorie Diet")
        val mealId = insertMeal("Breakfast")
        val foodId = insertFood("Rice", calories = 130.0) // 130 cal/100g

        mealDao.insertMealFoodItem(
            MealFoodItem(mealId = mealId, foodId = foodId, quantity = 200.0, unit = FoodUnit.GRAM)
        )
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "BREAKFAST", mealId = mealId))

        val summaries = dao.getDietsWithFullSummaryByUser(userId).first()
        val s = summaries.find { it.id == dietId }!!
        // 130 * 200 / 100 = 260 kcal
        assertEquals(260.0, s.totalCalories, 1.0)
    }

    @Test
    fun getDietsWithFullSummary_dietWithNoMealsHasZeroCalories() = runTest {
        insertDiet("Empty Diet")
        val summaries = dao.getDietsWithFullSummaryByUser(userId).first()
        assertEquals(0.0, summaries[0].totalCalories, 0.001)
        assertEquals(0, summaries[0].mealCount)
    }

    // ── Batch enrichment ──────────────────────────────────────────────────────

    @Test
    fun getFoodNamesForDiets_returnsAllFoodNames() = runTest {
        val d1 = insertDiet("Diet 1")
        val d2 = insertDiet("Diet 2")
        val m1 = insertMeal("M1")
        val m2 = insertMeal("M2")
        val f1 = insertFood("Eggs")
        val f2 = insertFood("Chicken")

        mealDao.insertMealFoodItem(MealFoodItem(mealId = m1, foodId = f1, quantity = 100.0, unit = FoodUnit.GRAM))
        mealDao.insertMealFoodItem(MealFoodItem(mealId = m2, foodId = f2, quantity = 100.0, unit = FoodUnit.GRAM))
        dao.insertDietMeal(DietMeal(dietId = d1, slotType = "BREAKFAST", mealId = m1))
        dao.insertDietMeal(DietMeal(dietId = d2, slotType = "BREAKFAST", mealId = m2))

        val rows = dao.getFoodNamesForDiets(listOf(d1, d2))
        val namesByDiet = rows.groupBy({ it.dietId }, { it.foodName })
        assertTrue(namesByDiet[d1]!!.contains("Eggs"))
        assertTrue(namesByDiet[d2]!!.contains("Chicken"))
    }

    @Test
    fun getDietMealsForDiets_returnsOnlyRequestedDiets() = runTest {
        val d1 = insertDiet("D1")
        val d2 = insertDiet("D2")
        val d3 = insertDiet("D3")
        val mId = insertMeal("M")
        dao.insertDietMeal(DietMeal(dietId = d1, slotType = "BREAKFAST", mealId = mId))
        dao.insertDietMeal(DietMeal(dietId = d2, slotType = "LUNCH", mealId = mId))
        dao.insertDietMeal(DietMeal(dietId = d3, slotType = "DINNER", mealId = mId))

        val result = dao.getDietMealsForDiets(listOf(d1, d2))
        assertEquals(2, result.size)
        assertTrue(result.none { it.dietId == d3 })
    }

    // ── Cascade delete ────────────────────────────────────────────────────────

    @Test
    fun deleteDiet_cascadesDietMeals() = runTest {
        val dietId = insertDiet("Cascade Diet")
        val mId = insertMeal("M")
        dao.insertDietMeal(DietMeal(dietId = dietId, slotType = "BREAKFAST", mealId = mId))

        dao.deleteDiet(dao.getDietById(dietId)!!)
        assertTrue(dao.getDietMeals(dietId).isEmpty())
    }
}
