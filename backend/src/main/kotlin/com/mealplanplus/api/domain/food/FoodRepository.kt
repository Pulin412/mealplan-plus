package com.mealplanplus.api.domain.food

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface FoodRepository : JpaRepository<Food, Long> {
    fun findByFirebaseUid(firebaseUid: String): List<Food>
    fun findByFirebaseUidOrIsSystemFoodTrue(firebaseUid: String): List<Food>
    fun findByIsSystemFoodTrue(): List<Food>
    fun findByFirebaseUidAndUpdatedAtAfter(firebaseUid: String, since: Instant): List<Food>
    fun findByIsSystemFoodTrueAndUpdatedAtAfter(since: Instant): List<Food>
    fun findByServerId(serverId: UUID): Food?

    @Query("""
        SELECT f FROM Food f
        WHERE (f.isSystemFood = true OR f.firebaseUid = :firebaseUid)
          AND (:query = '' OR LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(f.brand, '')) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY f.isSystemFood DESC, f.name ASC
    """)
    fun searchByNameOrBrand(
        firebaseUid: String,
        query: String,
        pageable: Pageable
    ): Page<Food>
}
