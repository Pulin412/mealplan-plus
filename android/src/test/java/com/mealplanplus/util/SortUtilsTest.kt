package com.mealplanplus.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SortUtilsTest {

    // ── naturalSortKey ────────────────────────────────────────────────────────

    @Test fun `Diet-M1 prefix is Diet-M and number is 1`() {
        val (prefix, num) = naturalSortKey("Diet-M1")
        assertEquals("Diet-M", prefix)
        assertEquals(1, num)
    }

    @Test fun `Diet-M10 prefix is Diet-M and number is 10`() {
        val (prefix, num) = naturalSortKey("Diet-M10")
        assertEquals("Diet-M", prefix)
        assertEquals(10, num)
    }

    @Test fun `Diet-R2 prefix is Diet-R and number is 2`() {
        val (prefix, num) = naturalSortKey("Diet-R2")
        assertEquals("Diet-R", prefix)
        assertEquals(2, num)
    }

    @Test fun `Diet-5 prefix is Diet- and number is 5`() {
        val (prefix, num) = naturalSortKey("Diet-5")
        assertEquals("Diet-", prefix)
        assertEquals(5, num)
    }

    @Test fun `non-matching name returns name as prefix and 0`() {
        val (prefix, num) = naturalSortKey("Custom Diet")
        assertEquals("Custom Diet", prefix)
        assertEquals(0, num)
    }

    // ── sortedByNaturalOrder ──────────────────────────────────────────────────

    @Test fun `Diet-M1 sorts before Diet-M2 and before Diet-M10`() {
        val names = listOf("Diet-M10", "Diet-M2", "Diet-M1")
        val sorted = names.sortedByNaturalOrder { it }
        assertEquals(listOf("Diet-M1", "Diet-M2", "Diet-M10"), sorted)
    }

    @Test fun `mixed prefix groups sort Diet- before Diet-M before Diet-R`() {
        val names = listOf("Diet-R1", "Diet-M1", "Diet-1")
        val sorted = names.sortedByNaturalOrder { it }
        // Prefix order: "Diet-" < "Diet-M" < "Diet-R" (alphabetical)
        assertEquals("Diet-1", sorted[0])
        assertEquals("Diet-M1", sorted[1])
        assertEquals("Diet-R1", sorted[2])
    }

    @Test fun `numbers within same prefix group are sorted numerically`() {
        val names = listOf("Diet-M9", "Diet-M11", "Diet-M10", "Diet-M2")
        val sorted = names.sortedByNaturalOrder { it }
        assertEquals(listOf("Diet-M2", "Diet-M9", "Diet-M10", "Diet-M11"), sorted)
    }

    @Test fun `single element list is unchanged`() {
        val names = listOf("Diet-M5")
        assertEquals(names, names.sortedByNaturalOrder { it })
    }

    @Test fun `empty list is unchanged`() {
        val names = emptyList<String>()
        assertEquals(names, names.sortedByNaturalOrder { it })
    }

    @Test fun `sort works on custom objects via selector`() {
        data class Item(val id: Int, val name: String)
        val items = listOf(Item(1, "Diet-M10"), Item(2, "Diet-M2"), Item(3, "Diet-M1"))
        val sorted = items.sortedByNaturalOrder { it.name }
        assertEquals(listOf(3, 2, 1), sorted.map { it.id })
    }

    // ── extractShortDietName ──────────────────────────────────────────────────

    @Test fun `Diet-M1 extracts to M1`() = assertEquals("M1", extractShortDietName("Diet-M1"))
    @Test fun `Diet-M12 extracts to M12`() = assertEquals("M12", extractShortDietName("Diet-M12"))
    @Test fun `Diet-R2 extracts to R2`() = assertEquals("R2", extractShortDietName("Diet-R2"))
    @Test fun `Diet-R12 extracts to R12`() = assertEquals("R12", extractShortDietName("Diet-R12"))
    @Test fun `plain Diet-5 extracts to R5`() = assertEquals("R5", extractShortDietName("Diet-5"))

    @Test fun `long custom name is truncated to 4 chars`() {
        val result = extractShortDietName("Custom Diet Plan")
        assertEquals("Cust", result)
    }

    @Test fun `short custom name is returned as-is`() {
        assertEquals("Keto", extractShortDietName("Keto"))
    }

    @Test fun `name shorter than 4 chars is returned as-is`() {
        assertEquals("LC", extractShortDietName("LC"))
    }
}
