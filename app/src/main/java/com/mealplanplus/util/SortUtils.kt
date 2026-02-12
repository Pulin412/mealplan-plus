package com.mealplanplus.util

/**
 * Natural sort comparator for diet names (Diet-1, Diet-2, ... Diet-10, Diet-11)
 * Handles "Diet-X" (plain), "Diet-MX" (Maintenance), and "Diet-RX" (Remission) formats
 */
fun naturalSortKey(name: String): Pair<String, Int> {
    val regex = Regex("^(Diet-[MR]?)(\\d+).*$")
    val match = regex.find(name)
    return if (match != null) {
        val prefix = match.groupValues[1]
        val number = match.groupValues[2].toIntOrNull() ?: 0
        Pair(prefix, number)
    } else {
        Pair(name, 0)
    }
}

/**
 * Sort a list using natural sort for diet names
 */
fun <T> List<T>.sortedByNaturalOrder(selector: (T) -> String): List<T> {
    return sortedWith(compareBy(
        { naturalSortKey(selector(it)).first },
        { naturalSortKey(selector(it)).second }
    ))
}

/**
 * Extract short name for calendar display
 * "Diet-M1" → "M1", "Diet-R12" → "R12", "Diet-5" → "R5", "Custom Diet" → "Cust"
 */
fun extractShortDietName(name: String): String {
    return when {
        name.startsWith("Diet-M") -> name.removePrefix("Diet-")  // M1, M2...
        name.startsWith("Diet-R") -> name.removePrefix("Diet-")  // R1, R12...
        // Plain "Diet-X" numbers → prefix with R (Remission)
        name.startsWith("Diet-") -> "R" + name.removePrefix("Diet-")
        name.length > 4 -> name.take(4)
        else -> name
    }
}
