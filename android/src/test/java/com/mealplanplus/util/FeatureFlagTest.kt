package com.mealplanplus.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies FeatureFlag enum: key names, default values, and uniqueness.
 * These act as a contract so that accidental key renames don't silently
 * break Remote Config lookups in production.
 */
class FeatureFlagTest {

    // ── key names ─────────────────────────────────────────────────────────────

    @Test
    fun barcodeScannerFlag_hasCorrectKey() {
        assertEquals("barcode_scanner_enabled", FeatureFlag.BARCODE_SCANNER.key)
    }

    @Test
    fun usdaSearchFlag_hasCorrectKey() {
        assertEquals("usda_search_enabled", FeatureFlag.USDA_SEARCH.key)
    }

    @Test
    fun offSearchFlag_hasCorrectKey() {
        assertEquals("off_search_enabled", FeatureFlag.OFF_SEARCH.key)
    }

    @Test
    fun syncFlag_hasCorrectKey() {
        assertEquals("sync_enabled", FeatureFlag.SYNC.key)
    }

    @Test
    fun groceryAutoGenerateFlag_hasCorrectKey() {
        assertEquals("grocery_auto_generate_enabled", FeatureFlag.GROCERY_AUTO_GENERATE.key)
    }

    @Test
    fun healthTrackingFlag_hasCorrectKey() {
        assertEquals("health_tracking_enabled", FeatureFlag.HEALTH_TRACKING.key)
    }

    // ── default values ────────────────────────────────────────────────────────

    @Test
    fun barcodeScannerFlag_defaultIsTrue() {
        assertTrue(FeatureFlag.BARCODE_SCANNER.defaultValue)
    }

    @Test
    fun usdaSearchFlag_defaultIsTrue() {
        assertTrue(FeatureFlag.USDA_SEARCH.defaultValue)
    }

    @Test
    fun offSearchFlag_defaultIsTrue() {
        assertTrue(FeatureFlag.OFF_SEARCH.defaultValue)
    }

    @Test
    fun syncFlag_defaultIsFalse() {
        // Sync is off by default — backend is not yet live
        assertFalse(FeatureFlag.SYNC.defaultValue)
    }

    @Test
    fun groceryAutoGenerateFlag_defaultIsTrue() {
        assertTrue(FeatureFlag.GROCERY_AUTO_GENERATE.defaultValue)
    }

    @Test
    fun healthTrackingFlag_defaultIsTrue() {
        assertTrue(FeatureFlag.HEALTH_TRACKING.defaultValue)
    }

    // ── uniqueness ────────────────────────────────────────────────────────────

    @Test
    fun allFlagKeys_areUnique() {
        val keys = FeatureFlag.entries.map { it.key }
        assertEquals("Duplicate Remote Config keys detected", keys.size, keys.toSet().size)
    }

    @Test
    fun flagCount_matchesExpected() {
        // This test intentionally fails when a flag is added or removed without updating
        // the rest of the feature (documentation, Remote Config console defaults, etc.).
        assertEquals(6, FeatureFlag.entries.size)
    }
}
