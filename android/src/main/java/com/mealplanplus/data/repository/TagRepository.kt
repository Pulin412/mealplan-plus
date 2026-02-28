package com.mealplanplus.data.repository

import android.content.Context
import com.mealplanplus.data.local.TagDao
import com.mealplanplus.data.model.DietTagCrossRef
import com.mealplanplus.data.model.Tag
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao,
    @ApplicationContext private val context: Context
) {
    private fun getCurrentUserId(): Long = runBlocking {
        AuthPreferences.getUserId(context).first() ?: throw IllegalStateException("Not logged in")
    }

    fun getTagsByUser(): Flow<List<Tag>> = tagDao.getTagsByUser(getCurrentUserId())

    suspend fun getTagById(tagId: Long): Tag? = tagDao.getTagById(tagId)

    suspend fun getTagsForDiet(dietId: Long): List<Tag> = tagDao.getTagsForDiet(dietId)

    fun getTagsForDietFlow(dietId: Long): Flow<List<Tag>> = tagDao.getTagsForDietFlow(dietId)

    suspend fun createTag(name: String): Long {
        val tag = Tag(
            userId = getCurrentUserId(),
            name = name.trim()
        )
        return tagDao.insertTag(tag)
    }

    suspend fun createTagWithColor(name: String, color: String): Long {
        val tag = Tag(
            userId = getCurrentUserId(),
            name = name.trim(),
            color = color
        )
        return tagDao.insertTag(tag)
    }

    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag)

    suspend fun deleteTagById(tagId: Long) = tagDao.deleteTagById(tagId)

    suspend fun setDietTags(dietId: Long, tagIds: List<Long>) {
        tagDao.clearDietTags(dietId)
        val crossRefs = tagIds.map { DietTagCrossRef(dietId, it) }
        tagDao.insertDietTags(crossRefs)
    }

    suspend fun addTagToDiet(dietId: Long, tagId: Long) {
        tagDao.insertDietTag(DietTagCrossRef(dietId, tagId))
    }

    suspend fun removeTagFromDiet(dietId: Long, tagId: Long) {
        tagDao.removeDietTag(dietId, tagId)
    }

    suspend fun getDietCountForTag(tagId: Long): Int = tagDao.getDietCountForTag(tagId)
}
