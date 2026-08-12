package com.mealplanplus.api.domain.meal

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*

@Entity
@Table(name = "meals")
class Meal(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val name: String = "",
    var notes: String? = null,
    var isFavorite: Boolean = false,
    @Convert(converter = StringListConverter::class)
    @Column(columnDefinition = "text")
    var slots: List<String> = emptyList(),
    // Social (V9): per-item share toggle + copy provenance.
    var isShared: Boolean = false,
    var copiedFromUid: String? = null,
    var copiedFromServerId: java.util.UUID? = null
) : SyncableEntity()

@Entity
@Table(name = "meal_food_items")
class MealFoodItem(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val mealId: Long = 0,
    val foodId: Long = 0,
    val quantity: Double = 0.0,
    val unit: String = "GRAM",
    val notes: String? = null
)
