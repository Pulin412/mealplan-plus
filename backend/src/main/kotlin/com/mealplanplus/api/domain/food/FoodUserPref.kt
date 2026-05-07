package com.mealplanplus.api.domain.food

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.io.Serializable

/** Composite PK for FoodUserPref */
data class FoodUserPrefId(
    val firebaseUid: String = "",
    val foodId: Long = 0
) : Serializable

@Entity
@Table(name = "food_user_prefs")
@IdClass(FoodUserPrefId::class)
class FoodUserPref(
    @Id val firebaseUid: String = "",
    @Id val foodId: Long = 0,
    var isFavorite: Boolean = false
)

interface FoodUserPrefRepository : JpaRepository<FoodUserPref, FoodUserPrefId> {
    fun findByFirebaseUid(firebaseUid: String): List<FoodUserPref>
    fun findByFirebaseUidAndFoodId(firebaseUid: String, foodId: Long): FoodUserPref?
}
