package com.mealplanplus.util

object GroceryCategory {
    const val DAIRY = "Dairy"
    const val PROTEINS = "Proteins"
    const val GRAINS = "Grains"
    const val VEGETABLES = "Vegetables"
    const val FRUITS = "Fruits"
    const val FATS_OILS = "Fats & Oils"
    const val BEVERAGES = "Beverages"
    const val SPICES = "Spices & Condiments"
    const val OTHER = "Other"

    val all = listOf(DAIRY, PROTEINS, GRAINS, VEGETABLES, FRUITS, FATS_OILS, BEVERAGES, SPICES, OTHER)

    val categoryEmoji = mapOf(
        DAIRY to "🥛",
        PROTEINS to "🥩",
        GRAINS to "🌾",
        VEGETABLES to "🥦",
        FRUITS to "🍎",
        FATS_OILS to "🫙",
        BEVERAGES to "🧃",
        SPICES to "🧂",
        OTHER to "🛒"
    )

    private val dairyKeywords = listOf("milk", "yogurt", "yoghurt", "cheese", "cottage", "cream", "butter", "whey", "paneer", "curd", "ghee", "kefir")
    private val proteinKeywords = listOf("chicken", "beef", "pork", "fish", "egg", "salmon", "tuna", "turkey", "lamb", "shrimp", "tofu", "tempeh", "lentil", "chickpea", "kidney bean", "black bean", "protein", "meat", "mutton", "prawn", "cod", "tilapia", "legume", "dal", "moong")
    private val grainKeywords = listOf("rice", "bread", "pasta", "oat", "wheat", "flour", "quinoa", "barley", "rye", "corn", "tortilla", "noodle", "cereal", "millet", "roti", "chapati", "bagel", "cracker", "granola")
    private val vegetableKeywords = listOf("spinach", "broccoli", "carrot", "tomato", "potato", "onion", "garlic", "pepper", "lettuce", "cabbage", "cucumber", "celery", "zucchini", "mushroom", "kale", "cauliflower", "asparagus", "pea", "green bean", "sweet potato", "beet", "radish", "leek", "ginger", "eggplant", "aubergine", "capsicum", "ladies finger", "okra")
    private val fruitKeywords = listOf("apple", "banana", "berry", "blueberry", "strawberry", "raspberry", "mango", "orange", "grape", "watermelon", "pineapple", "peach", "pear", "cherry", "kiwi", "plum", "apricot", "pomegranate", "lemon", "lime", "melon", "fig", "date", "avocado", "coconut")
    private val fatsOilsKeywords = listOf("oil", "olive oil", "coconut oil", "vinegar", "almond", "walnut", "cashew", "peanut", "pistachio", "sunflower seed", "flaxseed", "chia", "sesame", "mayonnaise", "lard")
    private val beverageKeywords = listOf("juice", "tea", "coffee", "water", "soda", "drink", "smoothie", "protein shake", "almond milk", "oat milk", "soy milk", "coconut water", "kombucha")
    private val spiceKeywords = listOf("salt", "pepper", "cumin", "turmeric", "cinnamon", "paprika", "oregano", "basil", "thyme", "chili", "curry", "coriander", "cardamom", "clove", "nutmeg", "honey", "sugar", "syrup", "sauce", "ketchup", "mustard", "soy sauce", "hot sauce", "dressing", "marinade", "herb", "spice", "vanilla", "yeast", "baking soda", "baking powder")

    fun categorize(foodName: String): String {
        val lower = foodName.lowercase()
        return when {
            dairyKeywords.any { lower.contains(it) } -> DAIRY
            proteinKeywords.any { lower.contains(it) } -> PROTEINS
            fruitKeywords.any { lower.contains(it) } -> FRUITS
            vegetableKeywords.any { lower.contains(it) } -> VEGETABLES
            grainKeywords.any { lower.contains(it) } -> GRAINS
            fatsOilsKeywords.any { lower.contains(it) } -> FATS_OILS
            beverageKeywords.any { lower.contains(it) } -> BEVERAGES
            spiceKeywords.any { lower.contains(it) } -> SPICES
            else -> OTHER
        }
    }
}
