package com.mealplanplus.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mealplanplus.data.model.FoodItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: FoodDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.foodDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun food(
        name: String,
        calories: Double = 100.0,
        barcode: String? = null,
        isFavorite: Boolean = false,
        isSystem: Boolean = false
    ) = FoodItem(
        name = name,
        caloriesPer100 = calories,
        proteinPer100 = 5.0,
        carbsPer100 = 15.0,
        fatPer100 = 2.0,
        barcode = barcode,
        isFavorite = isFavorite,
        isSystemFood = isSystem
    )

    // ── insert + getAllFoods ───────────────────────────────────────────────────

    @Test
    fun insertAndGetAllFoods() = runTest {
        dao.insertFood(food("Apple"))
        dao.insertFood(food("Banana"))
        val all = dao.getAllFoods().first()
        assertEquals(2, all.size)
    }

    @Test
    fun getAllFoodsOrderedByName() = runTest {
        dao.insertFood(food("Zucchini"))
        dao.insertFood(food("Apple"))
        dao.insertFood(food("Mango"))
        val names = dao.getAllFoods().first().map { it.name }
        assertEquals(listOf("Apple", "Mango", "Zucchini"), names)
    }

    // ── getFoodById ───────────────────────────────────────────────────────────

    @Test
    fun getFoodByIdReturnsCorrectItem() = runTest {
        val id = dao.insertFood(food("Chicken Breast", calories = 165.0))
        val result = dao.getFoodById(id)
        assertNotNull(result)
        assertEquals("Chicken Breast", result!!.name)
        assertEquals(165.0, result.caloriesPer100, 0.001)
    }

    @Test
    fun getFoodByIdReturnsNullForMissingId() = runTest {
        assertNull(dao.getFoodById(999L))
    }

    // ── searchFoods ───────────────────────────────────────────────────────────

    @Test
    fun searchFoodsMatchesPartialName() = runTest {
        dao.insertFood(food("Brown Rice"))
        dao.insertFood(food("White Rice"))
        dao.insertFood(food("Pasta"))

        val results = dao.searchFoods("Rice").first()
        assertEquals(2, results.size)
        assertTrue(results.all { "rice" in it.name.lowercase() })
    }

    @Test
    fun searchFoodsIsCaseInsensitive() = runTest {
        dao.insertFood(food("Oatmeal"))
        val results = dao.searchFoods("oat").first()
        assertEquals(1, results.size)
    }

    @Test
    fun searchFoodsReturnsEmptyForNoMatch() = runTest {
        dao.insertFood(food("Apple"))
        val results = dao.searchFoods("zzz").first()
        assertTrue(results.isEmpty())
    }

    // ── getFoodByBarcode ──────────────────────────────────────────────────────

    @Test
    fun getFoodByBarcodeReturnsMatchingItem() = runTest {
        dao.insertFood(food("Milk", barcode = "1234567890"))
        val result = dao.getFoodByBarcode("1234567890")
        assertNotNull(result)
        assertEquals("Milk", result!!.name)
    }

    @Test
    fun getFoodByBarcodeReturnsNullForMissingBarcode() = runTest {
        assertNull(dao.getFoodByBarcode("0000000000"))
    }

    // ── favorites ─────────────────────────────────────────────────────────────

    @Test
    fun getFavoritesReturnsOnlyFavorites() = runTest {
        val favId = dao.insertFood(food("Eggs", isFavorite = true))
        dao.insertFood(food("Toast", isFavorite = false))
        val favorites = dao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals(favId, favorites[0].id)
    }

    @Test
    fun setFavoriteMarksItemAsFavorite() = runTest {
        val id = dao.insertFood(food("Yogurt", isFavorite = false))
        dao.setFavorite(id, true)
        val item = dao.getFoodById(id)
        assertTrue(item!!.isFavorite)
    }

    @Test
    fun setFavoriteUnmarksItem() = runTest {
        val id = dao.insertFood(food("Cheese", isFavorite = true))
        dao.setFavorite(id, false)
        assertFalse(dao.getFoodById(id)!!.isFavorite)
    }

    // ── recent foods ──────────────────────────────────────────────────────────

    @Test
    fun getRecentFoodsReturnsItemsWithLastUsedOrderedDesc() = runTest {
        val id1 = dao.insertFood(food("Apple"))
        val id2 = dao.insertFood(food("Banana"))
        val id3 = dao.insertFood(food("Carrot"))

        dao.updateLastUsed(id1, 3000L)
        dao.updateLastUsed(id2, 1000L)
        dao.updateLastUsed(id3, 2000L)

        val recent = dao.getRecentFoods(10).first()
        assertEquals(listOf("Apple", "Carrot", "Banana"), recent.map { it.name })
    }

    @Test
    fun getRecentFoodsExcludesItemsWithNoLastUsed() = runTest {
        dao.insertFood(food("Apple"))  // no lastUsed
        val id = dao.insertFood(food("Banana"))
        dao.updateLastUsed(id, 1000L)

        val recent = dao.getRecentFoods(10).first()
        assertEquals(1, recent.size)
        assertEquals("Banana", recent[0].name)
    }

    @Test
    fun getRecentFoodsRespectsLimit() = runTest {
        repeat(10) { i ->
            val id = dao.insertFood(food("Food $i"))
            dao.updateLastUsed(id, i * 100L)
        }
        val recent = dao.getRecentFoods(3).first()
        assertEquals(3, recent.size)
    }

    // ── deleteFood ────────────────────────────────────────────────────────────

    @Test
    fun deleteFoodRemovesItem() = runTest {
        val id = dao.insertFood(food("Spinach"))
        val item = dao.getFoodById(id)!!
        dao.deleteFood(item)
        assertNull(dao.getFoodById(id))
    }

    @Test
    fun deleteFoodByIdRemovesItem() = runTest {
        val id = dao.insertFood(food("Broccoli"))
        dao.deleteFoodById(id)
        assertNull(dao.getFoodById(id))
    }

    // ── system food count ─────────────────────────────────────────────────────

    @Test
    fun getSystemFoodCountCountsOnlySystemFoods() = runTest {
        dao.insertFood(food("Sys1", isSystem = true))
        dao.insertFood(food("Sys2", isSystem = true))
        dao.insertFood(food("User1", isSystem = false))
        assertEquals(2, dao.getSystemFoodCount())
    }

    // ── updateFood ────────────────────────────────────────────────────────────

    @Test
    fun updateFoodPersistsChanges() = runTest {
        val id = dao.insertFood(food("Rice", calories = 100.0))
        val existing = dao.getFoodById(id)!!
        dao.updateFood(existing.copy(caloriesPer100 = 130.0, name = "Brown Rice"))

        val updated = dao.getFoodById(id)!!
        assertEquals("Brown Rice", updated.name)
        assertEquals(130.0, updated.caloriesPer100, 0.001)
    }

    // ── insertAll ─────────────────────────────────────────────────────────────

    @Test
    fun insertAllIgnoresDuplicates() = runTest {
        val item = food("Apple")
        val id = dao.insertFood(item)
        val duplicate = dao.getFoodById(id)!!  // same id
        dao.insertAll(listOf(duplicate, food("Mango")))
        // Apple should not be duplicated
        val all = dao.getAllFoods().first()
        assertEquals(1, all.count { it.name == "Apple" })
        assertEquals(1, all.count { it.name == "Mango" })
    }
}
