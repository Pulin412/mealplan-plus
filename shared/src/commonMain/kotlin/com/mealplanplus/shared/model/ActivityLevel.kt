package com.mealplanplus.shared.model

enum class ActivityLevel(val displayName: String, val multiplier: Double) {
    SEDENTARY("Sedentary (desk job)", 1.2),
    LIGHT("Lightly active (1-3d/wk)", 1.375),
    MODERATE("Moderately active (3-5d/wk)", 1.55),
    VERY_ACTIVE("Very active (6-7d/wk)", 1.725),
    EXTRA_ACTIVE("Athlete / physical job", 1.9)
}
