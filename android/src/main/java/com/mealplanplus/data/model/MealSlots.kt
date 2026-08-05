package com.mealplanplus.data.model

/**
 * Canonical meal slots — the single source of truth for the slot picker (what gets
 * saved) and for display ordering, shared across every screen and the diet mapper.
 *
 * These are the exact strings persisted in the backend/DB, so slots round-trip
 * without any normalization. Mirrors the backend's `DashboardService.CANONICAL_SLOTS`
 * and the webapp's `MEAL_SLOTS`; keep the three lists in sync.
 */
val MEAL_SLOTS: List<String> = listOf(
    "Early Morning", "Breakfast", "Noon", "Lunch", "Evening",
    "Pre-Workout", "Post-Workout", "Dinner", "Post-Dinner",
)
