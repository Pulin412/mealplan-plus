package com.mealplanplus.data.repository

import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietRepository @Inject constructor(
    private val dietDao: DietDao,
    private val mealRepository: MealRepository
) {
    fun getAllDiets(): Flow<List<Diet>> = dietDao.getAllDiets()

    suspend fun getDietById(id: Long): Diet? = dietDao.getDietById(id)

    suspend fun getDietWithMeals(dietId: Long): DietWithMeals? {
        val diet = dietDao.getDietById(dietId) ?: return null
        val dietMeals = dietDao.getDietMeals(dietId)
        val mealsMap = mutableMapOf<String, MealWithFoods?>()

        for (dm in dietMeals) {
            mealsMap[dm.slotType] = dm.mealId?.let { mealRepository.getMealWithFoods(it) }
        }

        return DietWithMeals(diet, mealsMap)
    }

    suspend fun insertDiet(diet: Diet): Long = dietDao.insertDiet(diet)

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

        return newDietId
    }
}
