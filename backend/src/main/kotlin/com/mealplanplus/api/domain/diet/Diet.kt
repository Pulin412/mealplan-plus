package com.mealplanplus.api.domain.diet

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*
import java.io.Serializable

enum class TagEntityType { DIET, EXERCISE, MEAL, FOOD }

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
    val targetFat: Double? = null,
    var isFavorite: Boolean = false
) : SyncableEntity()

@Entity
@Table(name = "diet_meals")
class DietMeal(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val dietId: Long = 0,
    val mealId: Long = 0,
    val dayOfWeek: Int = 0,
    val slot: String = "Lunch",
    val instructions: String? = null
)

@Entity
@Table(name = "diet_food_items")
class DietFoodItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val dietId: Long = 0,
    val foodId: Long = 0,
    val slot: String = "",
    val quantity: Double = 1.0,
    val unit: String = "GRAM"
)

@Entity
@Table(name = "tags")
class Tag(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String? = null,
    val name: String = "",
    val color: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "tag_entity_type", nullable = false)
    val entityType: TagEntityType = TagEntityType.DIET
)

data class EntityTagId(
    val tagId: Long = 0,
    // Must match the entity's @Id type (TagEntityType) — a String here breaks the IdClass mapping.
    val entityType: TagEntityType = TagEntityType.DIET,
    val entityId: Long = 0
) : Serializable

@Entity
@Table(name = "entity_tags")
@IdClass(EntityTagId::class)
class EntityTag(
    @Id val tagId: Long = 0,
    @Id
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "tag_entity_type")
    val entityType: TagEntityType = TagEntityType.DIET,
    @Id val entityId: Long = 0
)
