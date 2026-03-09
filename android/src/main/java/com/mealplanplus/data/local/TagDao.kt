package com.mealplanplus.data.local

import androidx.room.*
import com.mealplanplus.data.model.DietTagCrossRef
import com.mealplanplus.data.model.FoodTagCrossRef
import com.mealplanplus.data.model.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name")
    fun getTagsByUser(userId: Long): Flow<List<Tag>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): Tag?

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN diet_tags dt ON t.id = dt.tagId
        WHERE dt.dietId = :dietId
    """)
    suspend fun getTagsForDiet(dietId: Long): List<Tag>

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN diet_tags dt ON t.id = dt.tagId
        WHERE dt.dietId = :dietId
    """)
    fun getTagsForDietFlow(dietId: Long): Flow<List<Tag>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @Delete
    suspend fun deleteTag(tag: Tag)

    @Query("DELETE FROM tags WHERE id = :tagId")
    suspend fun deleteTagById(tagId: Long)

    // Diet-Tag junction operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietTag(crossRef: DietTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDietTags(crossRefs: List<DietTagCrossRef>)

    @Query("DELETE FROM diet_tags WHERE dietId = :dietId")
    suspend fun clearDietTags(dietId: Long)

    @Query("DELETE FROM diet_tags WHERE dietId = :dietId AND tagId = :tagId")
    suspend fun removeDietTag(dietId: Long, tagId: Long)

    @Query("SELECT COUNT(*) FROM diet_tags WHERE tagId = :tagId")
    suspend fun getDietCountForTag(tagId: Long): Int

    // Food-Tag junction operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodTag(crossRef: FoodTagCrossRef)

    @Query("DELETE FROM food_tag_cross_refs WHERE foodId = :foodId AND tagId = :tagId")
    suspend fun removeFoodTag(foodId: Long, tagId: Long)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN food_tag_cross_refs ft ON t.id = ft.tagId
        WHERE ft.foodId = :foodId
    """)
    fun getTagsForFood(foodId: Long): Flow<List<Tag>>

    @Query("SELECT foodId FROM food_tag_cross_refs WHERE tagId = :tagId")
    fun getFoodIdsForTag(tagId: Long): Flow<List<Long>>

    @Query("DELETE FROM food_tag_cross_refs WHERE foodId = :foodId")
    suspend fun clearFoodTags(foodId: Long)
}
