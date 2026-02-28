package com.mealplanplus.api.domain.diet

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*

@Entity
@Table(name = "diets")
class Diet(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val name: String = "",
    val description: String? = null,
    val targetCalories: Double? = null,
    val targetProtein: Double? = null,
    val targetCarbs: Double? = null,
    val targetFat: Double? = null
) : SyncableEntity()

@Entity
@Table(name = "diet_meals")
class DietMeal(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val dietId: Long = 0,
    val mealId: Long = 0,
    val dayOfWeek: Int = 0,  // 1=Mon..7=Sun, 0=any
    val slot: String = "Lunch",
    val instructions: String? = null
)

@Entity
@Table(name = "tags")
class Tag(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val name: String = ""
)

@Entity
@Table(name = "diet_tag_cross_refs")
class DietTagCrossRef(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val dietId: Long = 0,
    val tagId: Long = 0
)
