package com.mealplanplus.shared.model

/**
 * A saved grocery list generated from planned meals
 */
data class GroceryList(
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val startDate: String? = null,  // yyyy-MM-dd or null if manual
    val endDate: String? = null,    // yyyy-MM-dd or null if manual
    val createdAt: Long = currentTimeMillis(),
    val updatedAt: Long = currentTimeMillis()
) {
    val dateRangeDisplay: String?
        get() = when {
            startDate != null && endDate != null && startDate == endDate -> startDate
            startDate != null && endDate != null -> "$startDate to $endDate"
            startDate != null -> "From $startDate"
            else -> null
        }
}

/**
 * Individual item in a grocery list
 */
data class GroceryItem(
    val id: Long = 0,
    val listId: Long,
    val foodId: Long? = null,        // Link to FoodItem or null for custom
    val customName: String? = null,  // For items not in food DB
    val quantity: Double,
    val unit: FoodUnit,
    val isChecked: Boolean = false,
    val sortOrder: Int = 0,
    val category: String? = null     // GroceryCategory constant for UI grouping
)

/**
 * GroceryItem with its associated FoodItem (if linked)
 */
data class GroceryItemWithFood(
    val item: GroceryItem,
    val food: FoodItem?
) {
    val displayName: String
        get() = item.customName ?: food?.name ?: "Unknown"

    val displayQuantity: String
        get() = "${item.quantity.formatQuantity()} ${item.unit.shortLabel}"

    private fun Double.formatQuantity(): String {
        return if (this == this.toLong().toDouble()) {
            this.toLong().toString()
        } else {
            // Round to 1 decimal place
            val rounded = (this * 10).toLong() / 10.0
            val intPart = rounded.toLong()
            val decPart = ((rounded - intPart) * 10).toInt()
            "$intPart.$decPart"
        }
    }
}

/**
 * GroceryList with all its items
 */
data class GroceryListWithItems(
    val list: GroceryList,
    val items: List<GroceryItemWithFood>
) {
    val checkedCount: Int
        get() = items.count { it.item.isChecked }

    val totalCount: Int
        get() = items.size

    val progressPercent: Float
        get() = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f
}
