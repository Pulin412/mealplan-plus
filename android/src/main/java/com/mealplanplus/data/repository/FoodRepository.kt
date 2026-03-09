package com.mealplanplus.data.repository

import com.mealplanplus.data.local.FoodDao
import com.mealplanplus.data.local.TagDao
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.FoodTagCrossRef
import com.mealplanplus.data.model.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FoodRepository @Inject constructor(
    private val foodDao: FoodDao,
    private val tagDao: TagDao
) {
    fun getAllFoods(): Flow<List<FoodItem>> = foodDao.getAllFoods()

    fun searchFoods(query: String): Flow<List<FoodItem>> = foodDao.searchFoods(query)

    suspend fun getFoodById(id: Long): FoodItem? = foodDao.getFoodById(id)

    suspend fun getFoodByBarcode(barcode: String): FoodItem? = foodDao.getFoodByBarcode(barcode)

    fun getFavorites(): Flow<List<FoodItem>> = foodDao.getFavorites()

    fun getRecentFoods(limit: Int = 20): Flow<List<FoodItem>> = foodDao.getRecentFoods(limit)

    suspend fun setFavorite(id: Long, isFavorite: Boolean) = foodDao.setFavorite(id, isFavorite)

    suspend fun updateLastUsed(id: Long) = foodDao.updateLastUsed(id)

    suspend fun insertFood(food: FoodItem): Long = foodDao.insertFood(food)

    suspend fun updateFood(food: FoodItem) = foodDao.updateFood(food)

    suspend fun deleteFood(food: FoodItem) = foodDao.deleteFood(food)

    suspend fun deleteFoodById(id: Long) = foodDao.deleteFoodById(id)

    fun getTagsForFood(foodId: Long): Flow<List<Tag>> = tagDao.getTagsForFood(foodId)

    fun getFoodIdsForTag(tagId: Long): Flow<List<Long>> = tagDao.getFoodIdsForTag(tagId)

    suspend fun addTagToFood(foodId: Long, tagId: Long) = tagDao.insertFoodTag(FoodTagCrossRef(foodId, tagId))

    suspend fun removeTagFromFood(foodId: Long, tagId: Long) = tagDao.removeFoodTag(foodId, tagId)

    suspend fun clearFoodTags(foodId: Long) = tagDao.clearFoodTags(foodId)
}
