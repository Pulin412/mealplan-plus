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
    primaryKeys = ["userId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Diet::class,
            parentColumns = ["id"],
            childColumns = ["dietId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("userId"), Index("dietId"), Index("userId", "date", unique = true)]
)
data class Plan(
    val userId: Long,
    val date: Long,  // Epoch ms at midnight UTC
    val dietId: Long?,
    val notes: String? = null,
    val isCompleted: Boolean = false
)

/**
 * Plan with associated diet details
 */
data class PlanWithDiet(
    val plan: Plan,
    val diet: Diet?
)

/**
 * Plan with diet name for calendar display (single JOIN query result)
 */
data class PlanWithDietName(
    val userId: Long,
    val date: Long,  // Epoch ms
    val dietId: Long?,
    val isCompleted: Boolean,
    val notes: String?,
    val dietName: String?
)
