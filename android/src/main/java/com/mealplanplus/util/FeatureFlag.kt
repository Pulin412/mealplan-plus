package com.mealplanplus.util

/**
 * All Firebase Remote Config feature flags used in this app.
 *
 * Each entry maps a Remote Config key to a safe local default that is applied
 * before the first successful fetch (cold start, no network, etc.).
 *
 * Adding a flag here automatically:
 *  - Registers it in [RemoteConfigManager.applyDefaults] so Firebase uses the correct default
 *  - Documents the expected default for the Remote Config console
 *
 * Naming convention: snake_case, ending in `_enabled` for boolean toggles.
 */
enum class FeatureFlag(val key: String, val defaultValue: Boolean) {

    /** Barcode scanner for food lookup. Kill-switch in case of CameraX/MLKit issues. */
    BARCODE_SCANNER("barcode_scanner_enabled", true),

    /** USDA FoodData Central search. Can be disabled if API key is rate-limited. */
    USDA_SEARCH("usda_search_enabled", true),

    /** OpenFoodFacts product search and barcode lookup. */
    OFF_SEARCH("off_search_enabled", true),

    /** Background sync with the backend. Off by default — backend not yet live. */
    SYNC("sync_enabled", false),

    /** Automatic grocery list generation from a diet's meal plan. */
    GROCERY_AUTO_GENERATE("grocery_auto_generate_enabled", true),

    /** Health metrics tracking screen. */
    HEALTH_TRACKING("health_tracking_enabled", true),

    /** Firebase Analytics event collection. Off by default until pre-launch privacy review. */
    ANALYTICS_ENABLED("analytics_enabled", false),
}
