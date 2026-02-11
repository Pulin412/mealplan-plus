package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A planned diet for a specific date
 */
@Entity(
    tableName = "plans",
    foreignKeys = [
        ForeignKey(
            entity = Diet::class,
            parentColumns = ["id"],
            childColumns = ["dietId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("dietId"), Index("date", unique = true)]
)
data class Plan(
    @PrimaryKey
    val date: String,  // ISO format: yyyy-MM-dd
    val dietId: Long?,
    val notes: String? = null,
    val isCompleted: Boolean = false  // true when user "finishes" the planned diet
)

/**
 * Plan with associated diet details
 */
data class PlanWithDiet(
    val plan: Plan,
    val diet: Diet?
)
