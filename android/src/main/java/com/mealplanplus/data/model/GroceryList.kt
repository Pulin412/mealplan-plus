package com.mealplanplus.data.model

import androidx.room.*

/**
 * A saved grocery list generated from planned meals
 */
@Entity(
    tableName = "grocery_lists",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class GroceryList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val startDate: String? = null,  // yyyy-MM-dd or null if manual
    val endDate: String? = null,    // yyyy-MM-dd or null if manual
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
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
@Entity(
    tableName = "grocery_items",
    foreignKeys = [
        ForeignKey(
            entity = GroceryList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItem::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("listId"), Index("foodId")]
)
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val foodId: Long? = null,        // Link to FoodItem or null for custom
    val customName: String? = null,  // For items not in food DB
    val quantity: Double,
    val unit: FoodUnit,
    val isChecked: Boolean = false,
    val sortOrder: Int = 0,
    val category: String? = null     // Food category for UI grouping
)

/**
 * GroceryItem with its associated FoodItem (if linked)
 */
data class GroceryItemWithFood(
    @Embedded val item: GroceryItem,
    @Relation(
        parentColumn = "foodId",
        entityColumn = "id"
    )
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
            String.format("%.1f", this)
        }
    }
}

/**
 * GroceryList with all its items
 */
data class GroceryListWithItems(
    @Embedded val list: GroceryList,
    @Relation(
        parentColumn = "id",
        entityColumn = "listId",
        entity = GroceryItem::class
    )
    val items: List<GroceryItemWithFood>
) {
    val checkedCount: Int
        get() = items.count { it.item.isChecked }

    val totalCount: Int
        get() = items.size

    val progressPercent: Float
        get() = if (totalCount > 0) checkedCount.toFloat() / totalCount else 0f
}
