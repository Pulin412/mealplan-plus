package com.mealplanplus.shared.model

/**
 * A planned diet for a specific date
 */
data class Plan(
    val userId: Long,
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

/**
 * Plan with diet name for calendar display (single JOIN query result)
 */
data class PlanWithDietName(
    val userId: Long,
    val date: String,
    val dietId: Long?,
    val isCompleted: Boolean,
    val notes: String?,
    val dietName: String?
)
