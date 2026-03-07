package com.mealplanplus.api.domain.food

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface FoodRepository : JpaRepository<Food, Long> {
    fun findByFirebaseUidOrIsSystemFoodTrue(firebaseUid: String): List<Food>
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<Food>
    fun findByIsSystemFoodTrueAndUpdatedAtAfter(since: Instant): List<Food>
    fun findByServerId(serverId: UUID): Food?
}
