package com.mealplanplus.data.repository

import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.local.TagDao
import com.mealplanplus.data.model.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DietRepository @Inject constructor(
    private val dietDao: DietDao,
    private val tagDao: TagDao,
    private val mealRepository: MealRepository
) {
    fun getDietsForUser(userId: Long): Flow<List<Diet>> = dietDao.getDietsForUser(userId)

    fun getDietsWithFullSummaryForUser(userId: Long): Flow<List<DietFullSummary>> =
        dietDao.getDietsWithFullSummaryForUser(userId)

    fun getFavouriteDietsForUser(userId: Long): Flow<List<Diet>> =
        dietDao.getFavouriteDietsForUser(userId)

    suspend fun getDietCountForUser(userId: Long): Int = dietDao.getDietCountForUser(userId)

    /** Unfiltered — for seeder / importer use only. */
    fun getAllDiets(): Flow<List<Diet>> = dietDao.getAllDiets()

    fun getDietsWithFullSummary(): Flow<List<DietFullSummary>> = dietDao.getDietsWithFullSummary()

    suspend fun getDietById(id: Long): Diet? = dietDao.getDietById(id)

    suspend fun getTagsForDiet(dietId: Long): List<Tag> = tagDao.getTagsForDiet(dietId)

    suspend fun getTagsForDiets(dietIds: List<Long>): Map<Long, List<Tag>> {
        if (dietIds.isEmpty()) return emptyMap()
        return tagDao.getTagsForDiets(dietIds)
            .groupBy({ it.dietId }, { it.toTag() })
    }

    suspend fun getDietFoodNamesAndSlots(
        dietIds: List<Long>
    ): Pair<Map<Long, List<String>>, Map<Long, Set<String>>> {
        if (dietIds.isEmpty()) return emptyMap<Long, List<String>>() to emptyMap()
        val foodRows = dietDao.getFoodNamesForDiets(dietIds)
        val mealRows = dietDao.getDietMealsForDiets(dietIds)
        val foodNames = foodRows.groupBy({ it.dietId }, { it.foodName })
        val slots = mealRows.groupBy({ it.dietId }, { it.slotType })
            .mapValues { (_, v) -> v.toSet() }
        return foodNames to slots
    }

    suspend fun getDietWithMeals(dietId: Long): DietWithMeals? {
        val diet = dietDao.getDietById(dietId) ?: return null
        val dietMeals = dietDao.getDietMeals(dietId)
        val mealsMap = mutableMapOf<String, MealWithFoods?>()
        for (dm in dietMeals) {
            mealsMap[dm.slotType] = dm.mealId?.let { mealRepository.getMealWithFoods(it) }
        }
        val instructionsMap = dietMeals.associate { it.slotType to it.instructions }
        return DietWithMeals(diet, mealsMap, instructionsMap)
    }

    suspend fun insertDiet(diet: Diet): Long = dietDao.insertDiet(diet)

    suspend fun updateDiet(diet: Diet) = dietDao.updateDiet(diet)

    suspend fun toggleFavourite(diet: Diet) = dietDao.setFavourite(diet.id, !diet.isFavourite)

    fun getFavouriteDiets(): Flow<List<Diet>> = dietDao.getFavouriteDiets()

    suspend fun deleteDiet(diet: Diet) = dietDao.deleteDiet(diet)

    suspend fun setMealForSlot(dietId: Long, slotType: String, mealId: Long?) {
        val existing = dietDao.getDietMeal(dietId, slotType)
        dietDao.insertDietMeal(DietMeal(dietId, slotType, mealId, existing?.instructions))
    }

    suspend fun updateSlotInstructions(dietId: Long, slotType: String, instructions: String?) {
        val existing = dietDao.getDietMeal(dietId, slotType)
        if (existing != null) {
            dietDao.updateDietMealInstructions(dietId, slotType, instructions)
        } else {
            dietDao.insertDietMeal(DietMeal(dietId, slotType, null, instructions))
        }
    }

    suspend fun removeMealFromSlot(dietId: Long, slotType: String) {
        dietDao.removeMealFromDiet(dietId, slotType)
    }

    suspend fun duplicateDiet(dietId: Long, newName: String): Long {
        val original = getDietWithMeals(dietId) ?: return -1
        val newDiet = original.diet.copy(id = 0, name = newName)
        val newDietId = dietDao.insertDiet(newDiet)

        val dietMeals = dietDao.getDietMeals(dietId)
        dietDao.insertDietMeals(dietMeals.map { it.copy(dietId = newDietId) })

        val tags = tagDao.getTagsForDiet(dietId)
        tagDao.insertDietTags(tags.map { DietTagCrossRef(newDietId, it.id) })

        return newDietId
    }

    suspend fun setDietTags(dietId: Long, tagIds: List<Long>) {
        tagDao.clearDietTags(dietId)
        tagDao.insertDietTags(tagIds.map { DietTagCrossRef(dietId, it) })
    }

    suspend fun getDietCount(): Int = dietDao.getDietCount()
}
