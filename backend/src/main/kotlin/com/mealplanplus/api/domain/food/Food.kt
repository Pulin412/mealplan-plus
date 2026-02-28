package com.mealplanplus.api.domain.food

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*

@Entity
@Table(name = "foods")
class Food(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** null = system food (shared across all users) */
    val firebaseUid: String? = null,

    val name: String = "",
    val brand: String? = null,
    val barcode: String? = null,

    val caloriesPer100: Double = 0.0,
    val proteinPer100: Double = 0.0,
    val carbsPer100: Double = 0.0,
    val fatPer100: Double = 0.0,

    val gramsPerPiece: Double? = null,
    val gramsPerCup: Double? = null,
    val gramsPerTbsp: Double? = null,
    val gramsPerTsp: Double? = null,

    val glycemicIndex: Int? = null,
    val isSystemFood: Boolean = false
) : SyncableEntity()
