package com.mealplanplus.api.domain.diet

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface DietRepository : JpaRepository<Diet, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<Diet>
    fun findByServerId(serverId: UUID): Diet?
    fun findByFirebaseUidAndName(firebaseUid: String, name: String): Diet?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<Diet>
    fun countByFirebaseUid(firebaseUid: String): Long
}

interface DietMealRepository : JpaRepository<DietMeal, Long> {
    fun findByDietId(dietId: Long): List<DietMeal>
    fun findByDietIdIn(dietIds: Collection<Long>): List<DietMeal>
    fun deleteByDietId(dietId: Long)
}

interface DietFoodItemRepository : JpaRepository<DietFoodItem, Long> {
    fun findByDietId(dietId: Long): List<DietFoodItem>
    fun findByDietIdIn(dietIds: Collection<Long>): List<DietFoodItem>
    fun deleteByDietId(dietId: Long)
}

interface TagRepository : JpaRepository<Tag, Long> {
    fun findByName(name: String): Tag?
    fun findByFirebaseUid(firebaseUid: String): List<Tag>
    fun findByFirebaseUidAndName(firebaseUid: String, name: String): Tag?
    fun findByFirebaseUidAndNameAndEntityType(firebaseUid: String, name: String, entityType: TagEntityType): Tag?
    fun findByEntityType(entityType: TagEntityType): List<Tag>
    fun findByFirebaseUidAndEntityType(firebaseUid: String, entityType: TagEntityType): List<Tag>
}

interface EntityTagRepository : JpaRepository<EntityTag, EntityTagId> {
    fun findByEntityTypeAndEntityId(entityType: TagEntityType, entityId: Long): List<EntityTag>
    fun findByEntityTypeAndEntityIdIn(entityType: TagEntityType, entityIds: Collection<Long>): List<EntityTag>
    fun findByEntityTypeAndTagId(entityType: TagEntityType, tagId: Long): List<EntityTag>
    fun deleteByEntityTypeAndEntityId(entityType: TagEntityType, entityId: Long)
    fun deleteByTagId(tagId: Long)
}
