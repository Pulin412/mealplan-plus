package com.mealplanplus.util

/**
 * Natural, case-insensitive comparison for display names so numbers sort by value, not lexically:
 * "M1, M2, … M9, M10" instead of "M1, M10, M11, … M2". Digit runs are compared as numbers (ignoring
 * leading zeros); everything else compares char-by-char lowercased.
 */
fun compareNatural(a: String, b: String): Int {
    val s1 = a.lowercase()
    val s2 = b.lowercase()
    var i = 0
    var j = 0
    while (i < s1.length && j < s2.length) {
        val c1 = s1[i]
        val c2 = s2[j]
        if (c1.isDigit() && c2.isDigit()) {
            var i2 = i
            while (i2 < s1.length && s1[i2].isDigit()) i2++
            var j2 = j
            while (j2 < s2.length && s2[j2].isDigit()) j2++
            val n1 = s1.substring(i, i2).trimStart('0').ifEmpty { "0" }
            val n2 = s2.substring(j, j2).trimStart('0').ifEmpty { "0" }
            val cmp = if (n1.length != n2.length) n1.length - n2.length else n1.compareTo(n2)
            if (cmp != 0) return cmp
            i = i2
            j = j2
        } else {
            if (c1 != c2) return c1.compareTo(c2)
            i++
            j++
        }
    }
    return (s1.length - i) - (s2.length - j)
}

/** Comparator form of [compareNatural], for use with `sortedWith(compareBy(NaturalOrder) { it.name })`. */
val NaturalOrder: Comparator<String> = Comparator { a, b -> compareNatural(a, b) }
