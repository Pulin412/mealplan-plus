package com.mealplanplus.shared.repository

import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DietRepository(
    private val database: MealPlanDatabase,
    private val mealRepository: MealRepository
) {

    private val queries = database.dietQueries

    fun getAllDiets(userId: Long): Flow<List<Diet>> {
        return queries.selectAllDiets(userId).asFlowList().map { list ->
            list.map { it.toDiet() }
        }
    }

    suspend fun getDietById(id: Long): Diet? {
        return queries.selectDietById(id).executeAsOneOrNull()?.toDiet()
    }

    suspend fun getDietWithMeals(dietId: Long): DietWithMeals? {
        val diet = getDietById(dietId) ?: return null
        val dietMeals = queries.selectDietMeals(dietId).executeAsList()

        val mealsMap = mutableMapOf<String, MealWithFoods?>()
        val instructionsMap = mutableMapOf<String, String?>()
        for (dm in dietMeals) {
            val mealWithFoods = dm.mealId?.let { mealRepository.getMealWithFoods(it) }
            mealsMap[dm.slotType] = mealWithFoods
            instructionsMap[dm.slotType] = dm.instructions
        }

        return DietWithMeals(diet, mealsMap, instructionsMap)
    }

    fun getDietSummaries(userId: Long): Flow<List<DietSummary>> {
        return queries.selectDietSummaries(userId).asFlowList().map { list ->
            list.map {
                DietSummary(
                    id = it.id,
                    userId = it.userId,
                    name = it.name,
                    description = it.description,
                    createdAt = it.createdAt,
                    mealCount = it.mealCount.toInt(),
                    totalCalories = it.totalCalories.toInt()
                )
            }
        }
    }

    suspend fun insertDiet(diet: Diet): Long {
        queries.insertDiet(
            userId = diet.userId,
            name = diet.name,
            description = diet.description,
            createdAt = diet.createdAt,
            isSystemDiet = if (diet.isSystemDiet) 1L else 0L,
            updatedAt = diet.updatedAt
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateDiet(diet: Diet) {
        queries.updateDiet(
            name = diet.name,
            description = diet.description,
            updatedAt = diet.updatedAt,
            id = diet.id
        )
    }

    suspend fun getUnsyncedDiets(userId: Long): List<Diet> {
        return queries.selectUnsyncedDiets(userId).executeAsList().map { it.toDiet() }
    }

    suspend fun getDietByServerId(serverId: String): Diet? {
        return queries.selectDietByServerId(serverId).executeAsOneOrNull()?.toDiet()
    }

    suspend fun updateDietSyncState(id: Long, serverId: String, syncedAt: Long) {
        queries.updateDietSyncState(serverId = serverId, syncedAt = syncedAt, id = id)
    }

    suspend fun updateDietSyncedAt(id: Long, syncedAt: Long) {
        queries.updateDietSyncedAt(syncedAt = syncedAt, id = id)
    }

    suspend fun deleteDiet(id: Long) {
        queries.deleteDietById(id)
    }

    suspend fun setDietMeal(dietId: Long, slotType: String, mealId: Long?, instructions: String? = null) {
        queries.insertDietMeal(dietId, slotType, mealId, instructions)
    }

    suspend fun removeDietMeal(dietId: Long, slotType: String) {
        queries.deleteDietMeal(dietId, slotType)
    }

    suspend fun clearDietMeals(dietId: Long) {
        queries.deleteDietMealsByDietId(dietId)
    }

    // Tags
    fun getAllTags(userId: Long): Flow<List<Tag>> {
        return queries.selectAllTags(userId).asFlowList().map { list ->
            list.map { it.toTag() }
        }
    }

    suspend fun getTagById(id: Long): Tag? {
        return queries.selectTagById(id).executeAsOneOrNull()?.toTag()
    }

    suspend fun getTagByName(userId: Long, name: String): Tag? {
        return queries.selectTagByName(userId, name).executeAsOneOrNull()?.toTag()
    }

    suspend fun insertTag(tag: Tag): Long {
        queries.insertTag(tag.userId, tag.name, tag.color, tag.createdAt)
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateTag(tag: Tag) {
        queries.updateTag(tag.name, tag.color, tag.id)
    }

    suspend fun deleteTag(id: Long) {
        queries.deleteTagById(id)
    }

    fun getTagsForDiet(dietId: Long): Flow<List<Tag>> {
        return queries.selectTagsForDiet(dietId).asFlowList().map { list ->
            list.map { it.toTag() }
        }
    }

    suspend fun addTagToDiet(dietId: Long, tagId: Long) {
        queries.insertDietTag(dietId, tagId)
    }

    suspend fun removeTagFromDiet(dietId: Long, tagId: Long) {
        queries.deleteDietTag(dietId, tagId)
    }

    suspend fun clearDietTags(dietId: Long) {
        queries.deleteDietTagsByDietId(dietId)
    }

    // MARK: - Snapshot functions for iOS
    @Throws(Exception::class)
    suspend fun getDietSummariesSnapshot(userId: Long): List<DietSummary> {
        return queries.selectDietSummaries(userId).executeAsList().map {
            DietSummary(
                id = it.id,
                userId = it.userId,
                name = it.name,
                description = it.description,
                createdAt = it.createdAt,
                mealCount = it.mealCount.toInt(),
                totalCalories = it.totalCalories.toInt()
            )
        }
    }

    @Throws(Exception::class)
    suspend fun getTagsForDietSnapshot(dietId: Long): List<Tag> {
        return try {
            queries.selectTagsForDiet(dietId).executeAsList().map { it.toTag() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Throws(Exception::class)
    suspend fun getAllTagsSnapshot(userId: Long): List<Tag> {
        return queries.selectAllTags(userId).executeAsList().map { it.toTag() }
    }

    @Throws(Exception::class)
    suspend fun getAllDietsSnapshot(userId: Long): List<Diet> {
        return queries.selectAllDiets(userId).executeAsList().map { it.toDiet() }
    }

    private fun com.mealplanplus.shared.db.Diets.toDiet(): Diet {
        return Diet(
            id = id,
            userId = userId,
            name = name,
            description = description,
            createdAt = createdAt,
            isSystemDiet = isSystemDiet == 1L,
            serverId = serverId,
            updatedAt = updatedAt,
            syncedAt = syncedAt
        )
    }

    private fun com.mealplanplus.shared.db.Tags.toTag(): Tag {
        return Tag(
            id = id,
            userId = userId,
            name = name,
            color = color,
            createdAt = createdAt
        )
    }
}
