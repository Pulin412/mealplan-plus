package com.mealplanplus.api.domain.diet

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface DietRepository : JpaRepository<Diet, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<Diet>
    fun findByServerId(serverId: UUID): Diet?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<Diet>
    fun countByFirebaseUid(firebaseUid: String): Long
}

interface DietMealRepository : JpaRepository<DietMeal, Long> {
    fun findByDietId(dietId: Long): List<DietMeal>
    fun deleteByDietId(dietId: Long)
}

interface TagRepository : JpaRepository<Tag, Long> {
    fun findByName(name: String): Tag?
    fun findByFirebaseUid(firebaseUid: String): List<Tag>
    fun findByFirebaseUidAndName(firebaseUid: String, name: String): Tag?
}

interface DietTagCrossRefRepository : JpaRepository<DietTagCrossRef, Long> {
    fun findByDietId(dietId: Long): List<DietTagCrossRef>
    fun deleteByDietId(dietId: Long)
    fun deleteByTagId(tagId: Long)
}
