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
class TagDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TagDao
    private lateinit var dietDao: DietDao
    private lateinit var foodDao: FoodDao

    private val userId = 1L

    @Before
    fun setup() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.tagDao()
        dietDao = db.dietDao()
        foodDao = db.foodDao()

        db.userDao().insertUser(User(id = userId, email = "t@t.com", passwordHash = "x", displayName = "T"))
    }

    @After
    fun tearDown() = db.close()

    private suspend fun insertTag(name: String, color: String = "#FF0000"): Long =
        dao.insertTag(Tag(userId = userId, name = name, color = color))

    private suspend fun insertDiet(name: String): Long =
        dietDao.insertDiet(Diet(userId = userId, name = name))

    private suspend fun insertFood(name: String): Long =
        foodDao.insertFood(FoodItem(name = name, caloriesPer100 = 100.0, proteinPer100 = 5.0, carbsPer100 = 10.0, fatPer100 = 2.0))

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    fun insertAndGetTagById() = runTest {
        val id = insertTag("Low Carb", "#4CAF50")
        val tag = dao.getTagById(id)
        assertNotNull(tag)
        assertEquals("Low Carb", tag!!.name)
        assertEquals("#4CAF50", tag.color)
    }

    @Test
    fun getTagsByUser_returnsTagsInNameOrder() = runTest {
        insertTag("Zebra")
        insertTag("Apple")
        insertTag("Mango")

        val tags = dao.getTagsByUser(userId).first()
        assertEquals(3, tags.size)
        assertEquals("Apple", tags[0].name)
        assertEquals("Mango", tags[1].name)
        assertEquals("Zebra", tags[2].name)
    }

    @Test
    fun updateTag_changesNameAndColor() = runTest {
        val id = insertTag("Old", "#000000")
        dao.updateTag(Tag(id = id, userId = userId, name = "New", color = "#FFFFFF"))
        val tag = dao.getTagById(id)!!
        assertEquals("New", tag.name)
        assertEquals("#FFFFFF", tag.color)
    }

    @Test
    fun deleteTag_removesFromDb() = runTest {
        val id = insertTag("Temp")
        dao.deleteTag(dao.getTagById(id)!!)
        assertNull(dao.getTagById(id))
    }

    @Test
    fun deleteTagById_removesFromDb() = runTest {
        val id = insertTag("Temp2")
        dao.deleteTagById(id)
        assertNull(dao.getTagById(id))
    }

    // ── Diet–Tag junction ─────────────────────────────────────────────────────

    @Test
    fun insertDietTag_and_getTagsForDiet() = runTest {
        val tagId = insertTag("Keto")
        val dietId = insertDiet("Low Carb Diet")
        dao.insertDietTag(DietTagCrossRef(dietId = dietId, tagId = tagId))

        val tags = dao.getTagsForDiet(dietId)
        assertEquals(1, tags.size)
        assertEquals("Keto", tags[0].name)
    }

    @Test
    fun insertDietTags_bulk() = runTest {
        val t1 = insertTag("Keto")
        val t2 = insertTag("High Protein")
        val dietId = insertDiet("Power Diet")

        dao.insertDietTags(listOf(
            DietTagCrossRef(dietId = dietId, tagId = t1),
            DietTagCrossRef(dietId = dietId, tagId = t2)
        ))

        assertEquals(2, dao.getTagsForDiet(dietId).size)
    }

    @Test
    fun clearDietTags_removesAllTagsForDiet() = runTest {
        val t1 = insertTag("T1")
        val t2 = insertTag("T2")
        val dietId = insertDiet("Some Diet")
        dao.insertDietTag(DietTagCrossRef(dietId = dietId, tagId = t1))
        dao.insertDietTag(DietTagCrossRef(dietId = dietId, tagId = t2))

        dao.clearDietTags(dietId)
        assertTrue(dao.getTagsForDiet(dietId).isEmpty())
    }

    @Test
    fun removeDietTag_removesSingleTag() = runTest {
        val t1 = insertTag("Keep")
        val t2 = insertTag("Remove")
        val dietId = insertDiet("Diet")
        dao.insertDietTag(DietTagCrossRef(dietId = dietId, tagId = t1))
        dao.insertDietTag(DietTagCrossRef(dietId = dietId, tagId = t2))

        dao.removeDietTag(dietId, t2)
        val tags = dao.getTagsForDiet(dietId)
        assertEquals(1, tags.size)
        assertEquals("Keep", tags[0].name)
    }

    @Test
    fun getTagsForDietFlow_emitsUpdates() = runTest {
        val tagId = insertTag("Flow Tag")
        val dietId = insertDiet("Flow Diet")

        // Initially empty
        assertTrue(dao.getTagsForDietFlow(dietId).first().isEmpty())

        dao.insertDietTag(DietTagCrossRef(dietId = dietId, tagId = tagId))
        assertEquals(1, dao.getTagsForDietFlow(dietId).first().size)
    }

    // ── Batch diet–tag query ──────────────────────────────────────────────────

    @Test
    fun getTagsForDiets_returnsFlatListWithDietId() = runTest {
        val tLowCarb = insertTag("Low Carb")
        val tHighPro = insertTag("High Protein")
        val d1 = insertDiet("Diet 1")
        val d2 = insertDiet("Diet 2")

        dao.insertDietTag(DietTagCrossRef(dietId = d1, tagId = tLowCarb))
        dao.insertDietTag(DietTagCrossRef(dietId = d2, tagId = tHighPro))
        dao.insertDietTag(DietTagCrossRef(dietId = d2, tagId = tLowCarb))

        val rows = dao.getTagsForDiets(listOf(d1, d2))
        val grouped = rows.groupBy { it.dietId }

        assertEquals(1, grouped[d1]?.size)
        assertEquals(2, grouped[d2]?.size)

        assertTrue(grouped[d1]!!.any { it.name == "Low Carb" })
        assertTrue(grouped[d2]!!.any { it.name == "High Protein" })
        assertTrue(grouped[d2]!!.any { it.name == "Low Carb" })
    }

    // ── getDietCountForTag ────────────────────────────────────────────────────

    @Test
    fun getDietCountForTag_countsAssignedDiets() = runTest {
        val tagId = insertTag("Common Tag")
        val d1 = insertDiet("Diet A")
        val d2 = insertDiet("Diet B")
        dao.insertDietTag(DietTagCrossRef(dietId = d1, tagId = tagId))
        dao.insertDietTag(DietTagCrossRef(dietId = d2, tagId = tagId))

        assertEquals(2, dao.getDietCountForTag(tagId))
    }

    @Test
    fun getDietCountForTag_returnsZeroForUnusedTag() = runTest {
        val tagId = insertTag("Unused")
        assertEquals(0, dao.getDietCountForTag(tagId))
    }

    // ── Food–Tag junction ─────────────────────────────────────────────────────

    @Test
    fun insertFoodTag_and_getFoodIdsForTag() = runTest {
        val tagId = insertTag("Protein Food")
        val f1 = insertFood("Chicken")
        val f2 = insertFood("Eggs")

        dao.insertFoodTag(FoodTagCrossRef(foodId = f1, tagId = tagId))
        dao.insertFoodTag(FoodTagCrossRef(foodId = f2, tagId = tagId))

        val foodIds = dao.getFoodIdsForTag(tagId).first()
        assertEquals(2, foodIds.size)
        assertTrue(foodIds.containsAll(listOf(f1, f2)))
    }

    @Test
    fun removeFoodTag_removesFromJunction() = runTest {
        val tagId = insertTag("Removable")
        val foodId = insertFood("Some Food")
        dao.insertFoodTag(FoodTagCrossRef(foodId = foodId, tagId = tagId))

        dao.removeFoodTag(foodId, tagId)
        assertTrue(dao.getFoodIdsForTag(tagId).first().isEmpty())
    }

    @Test
    fun clearFoodTags_removesAllTagsForFood() = runTest {
        val t1 = insertTag("Tag A")
        val t2 = insertTag("Tag B")
        val foodId = insertFood("Multi-Tag Food")
        dao.insertFoodTag(FoodTagCrossRef(foodId = foodId, tagId = t1))
        dao.insertFoodTag(FoodTagCrossRef(foodId = foodId, tagId = t2))

        dao.clearFoodTags(foodId)
        assertTrue(dao.getFoodIdsForTag(t1).first().isEmpty())
        assertTrue(dao.getFoodIdsForTag(t2).first().isEmpty())
    }
}
