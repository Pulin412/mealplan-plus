package com.mealplanplus.util

/**
 * Natural sort comparator for diet names (Diet-1, Diet-2, ... Diet-10, Diet-11)
 * Handles both "Diet-X" (Remission) and "Diet-MX" (Maintenance) formats
 */
fun naturalSortKey(name: String): Pair<String, Int> {
    val regex = Regex("^(Diet-M?)(\\d+).*$")
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
