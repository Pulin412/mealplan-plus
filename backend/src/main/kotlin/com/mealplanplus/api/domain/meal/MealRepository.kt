package com.mealplanplus.api.domain.meal

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.util.UUID

interface MealRepository : JpaRepository<Meal, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<Meal>
    fun findByServerId(serverId: UUID): Meal?
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<Meal>
}

interface MealFoodItemRepository : JpaRepository<MealFoodItem, Long> {
    fun findByMealId(mealId: Long): List<MealFoodItem>
    fun deleteByMealId(mealId: Long)
}
