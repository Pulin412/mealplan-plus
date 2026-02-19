package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.local.TagDao
import com.mealplanplus.data.model.*
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietRepository @Inject constructor(
    private val dietDao: DietDao,
    private val tagDao: TagDao,
    private val mealRepository: MealRepository,
    @ApplicationContext private val context: Context
) {
    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    fun getDietsByUser(): Flow<List<Diet>> = dietDao.getDietsByUser(getCurrentUserId())

    fun getDietsWithSummary(): Flow<List<DietSummary>> = dietDao.getDietsWithSummaryByUser(getCurrentUserId())

    fun getDietsWithFullSummary(): Flow<List<DietFullSummary>> = dietDao.getDietsWithFullSummaryByUser(getCurrentUserId())

    suspend fun getDietById(id: Long): Diet? = dietDao.getDietById(id)

    suspend fun getTagsForDiet(dietId: Long): List<Tag> = tagDao.getTagsForDiet(dietId)

    suspend fun getDietWithMeals(dietId: Long): DietWithMeals? {
        val diet = dietDao.getDietById(dietId) ?: return null
        val dietMeals = dietDao.getDietMeals(dietId)
        val mealsMap = mutableMapOf<String, MealWithFoods?>()

        for (dm in dietMeals) {
            mealsMap[dm.slotType] = dm.mealId?.let { mealRepository.getMealWithFoods(it) }
        }

        return DietWithMeals(diet, mealsMap)
    }

    suspend fun insertDiet(diet: Diet): Long {
        val dietWithUserId = diet.copy(userId = getCurrentUserId())
        return dietDao.insertDiet(dietWithUserId)
    }

    suspend fun updateDiet(diet: Diet) = dietDao.updateDiet(diet)

    suspend fun deleteDiet(diet: Diet) = dietDao.deleteDiet(diet)

    suspend fun setMealForSlot(dietId: Long, slotType: String, mealId: Long?) {
        dietDao.insertDietMeal(DietMeal(dietId, slotType, mealId))
    }

    suspend fun removeMealFromSlot(dietId: Long, slotType: String) {
        dietDao.removeMealFromDiet(dietId, slotType)
    }

    suspend fun duplicateDiet(dietId: Long, newName: String): Long {
        val original = getDietWithMeals(dietId) ?: return -1
        val newDiet = original.diet.copy(id = 0, name = newName)
        val newDietId = dietDao.insertDiet(newDiet)

        val dietMeals = dietDao.getDietMeals(dietId)
        val newDietMeals = dietMeals.map { it.copy(dietId = newDietId) }
        dietDao.insertDietMeals(newDietMeals)

        // Copy tags too
        val tags = tagDao.getTagsForDiet(dietId)
        val newTagRefs = tags.map { DietTagCrossRef(newDietId, it.id) }
        tagDao.insertDietTags(newTagRefs)

        return newDietId
    }

    suspend fun setDietTags(dietId: Long, tagIds: List<Long>) {
        tagDao.clearDietTags(dietId)
        val crossRefs = tagIds.map { DietTagCrossRef(dietId, it) }
        tagDao.insertDietTags(crossRefs)
    }

    suspend fun getDietCount(): Int = dietDao.getDietCountByUser(getCurrentUserId())
}
