package com.mealplanplus.shared.model

/**
 * User-created tag for categorizing diets
 */
data class Tag(
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val color: String? = null,  // Hex color like "#FFEB3B" or null for default
    val createdAt: Long = currentTimeMillis()
) {
    companion object {
        // Predefined tag colors
        const val COLOR_REMISSION = "#FFEB3B"   // Yellow
        const val COLOR_MAINTENANCE = "#4CAF50" // Green
        const val COLOR_SOS = "#F44336"         // Red

        // Color palette for custom tags
        val COLOR_PALETTE = listOf(
            "#FFEB3B", // Yellow
            "#4CAF50", // Green
            "#F44336", // Red
            "#2196F3", // Blue
            "#9C27B0", // Purple
            "#FF9800", // Orange
            "#00BCD4", // Cyan
            "#E91E63", // Pink
            "#795548", // Brown
            "#607D8B"  // Blue Grey
        )

        /**
         * Get default color for known tag names
         */
        fun getDefaultColor(name: String): String {
            return when (name.uppercase()) {
                "REMISSION" -> COLOR_REMISSION
                "MAINTENANCE" -> COLOR_MAINTENANCE
                "SOS" -> COLOR_SOS
                else -> COLOR_PALETTE[0]
            }
        }
    }

    /**
     * Get effective color (stored color or default based on name)
     */
    val effectiveColor: String
        get() = color ?: getDefaultColor(name)
}

/**
 * Junction table: Diet can have multiple tags
 */
data class DietTagCrossRef(
    val dietId: Long,
    val tagId: Long
)
