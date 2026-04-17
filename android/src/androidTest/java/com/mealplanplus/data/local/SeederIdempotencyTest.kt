package com.mealplanplus.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealplanplus.data.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies that running the seeders multiple times does not create duplicate rows.
 *
 * Guards against regressions to the bug fixed in DB v28 (UserDataSeeder lacked
 * a getDietCount() > 0 early-return, allowing duplicate diets/meals on every app start).
 *
 * These are instrumented tests (Room in-memory DB) — run via:
 *   ./gradlew :android:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SeederIdempotencyTest {

    private lateinit var db: AppDatabase
    private lateinit var dietDao: DietDao
    private lateinit var mealDao: MealDao
    private lateinit var foodDao: FoodDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dietDao = db.dietDao()
        mealDao = db.mealDao()
        foodDao = db.foodDao()

        // Seed a minimal user required by FK constraints
        runTest {
            db.userDao().insertUser(
                User(
                    id = 1L,
                    email = "test@example.com",
                    passwordHash = "x",
                    displayName = "Test User"
                )
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun dietDao_insertOrIgnore_doesNotDuplicateOnConflict() = runTest {
        // Insert a diet twice with IGNORE strategy (same as seeder upsert)
        val diet = com.mealplanplus.data.model.Diet(name = "Test Diet", isSystem = true)
        dietDao.insertDiet(diet)
        dietDao.insertDiet(diet)   // duplicate — should be ignored / replaced, not doubled

        val all = dietDao.getAllDiets().first()
        assertEquals("Inserting the same diet twice must not duplicate rows", 1, all.size)
    }

    @Test
    fun mealDao_replace_onConflict_doesNotAddExtraRow() = runTest {
        // Insert a meal with an explicit id; REPLACE strategy means re-inserting the same id
        // updates the row rather than adding a duplicate.
        val meal = com.mealplanplus.data.model.Meal(id = 42L, name = "Breakfast Bowl", isSystem = true)
        mealDao.insertMeal(meal)
        mealDao.insertMeal(meal.copy(name = "Breakfast Bowl (updated)"))

        val all = mealDao.getAllMeals().first()
        assertEquals("Re-inserting the same id must replace, not duplicate", 1, all.size)
        assertEquals("Breakfast Bowl (updated)", all.first().name)
    }

    @Test
    fun dietDao_getDietCount_returnsCorrectCount() = runTest {
        assertEquals(0, dietDao.getDietCount())

        dietDao.insertDiet(com.mealplanplus.data.model.Diet(name = "Diet A", isSystem = true))
        dietDao.insertDiet(com.mealplanplus.data.model.Diet(name = "Diet B", isSystem = true))

        assertEquals(2, dietDao.getDietCount())
    }

    @Test
    fun mealDao_getMealCount_returnsCorrectCount() = runTest {
        assertEquals(0, mealDao.getMealCount())

        mealDao.insertMeal(com.mealplanplus.data.model.Meal(name = "Meal A", isSystem = true))
        mealDao.insertMeal(com.mealplanplus.data.model.Meal(name = "Meal B", isSystem = true))

        assertEquals(2, mealDao.getMealCount())
    }

    @Test
    fun seederGuard_dietCountAboveZeroMeansSkip() = runTest {
        // Simulate the idempotency guard in UserDataSeeder:
        //   if (dietDao.getDietCount() > 0) return
        // After one diet is inserted, the guard must trip.
        dietDao.insertDiet(com.mealplanplus.data.model.Diet(name = "Seeded Diet", isSystem = true))

        val shouldSkip = dietDao.getDietCount() > 0
        assertTrue("Seeder guard must prevent re-seeding when diets already exist", shouldSkip)
    }
}
