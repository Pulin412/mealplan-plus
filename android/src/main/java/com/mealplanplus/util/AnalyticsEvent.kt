package com.mealplanplus.util

/**
 * Firebase Analytics event names and parameter keys used in this app.
 *
 * These are plain string constants so that:
 *  - Renaming a key fails the contract tests, preventing silent dashboard breakage.
 *  - Call sites never hard-code strings.
 *
 * Naming convention: snake_case. Standard Firebase event names (sign_in, sign_up, screen_view)
 * are used where they match Firebase's auto-reporting to keep dashboards unified.
 */
object AnalyticsEvent {

    // ── Event names ───────────────────────────────────────────────────────────

    const val SIGN_IN = "sign_in"
    const val SIGN_UP = "sign_up"
    const val SIGN_OUT = "sign_out"

    const val FOOD_SEARCHED = "food_searched"
    const val FOOD_ADDED = "food_added"
    const val BARCODE_SCANNED = "barcode_scanned"

    const val DIET_CREATED = "diet_created"
    const val DIET_VIEWED = "diet_viewed"

    const val MEAL_PLAN_VIEWED = "meal_plan_viewed"

    const val GROCERY_LIST_CREATED = "grocery_list_created"
    const val GROCERY_LIST_GENERATED = "grocery_list_generated"

    const val HEALTH_METRIC_LOGGED = "health_metric_logged"

    // ── Parameter keys ────────────────────────────────────────────────────────

    object Param {
        /** Sign-in/sign-up provider: "email" | "google" */
        const val PROVIDER = "provider"

        /** Search string used in food search */
        const val SEARCH_QUERY = "search_query"

        /** Name of the food item added */
        const val FOOD_NAME = "food_name"

        /** Data source: "local" | "usda" | "off" */
        const val SOURCE = "source"

        /** Local diet ID */
        const val DIET_ID = "diet_id"

        /** Whether an operation succeeded: true | false */
        const val SUCCESS = "success"

        /** Health metric type, e.g. "WEIGHT", "STEPS" */
        const val METRIC_TYPE = "metric_type"

        /** Screen name for explicit screen_view events */
        const val SCREEN_NAME = "screen_name"
    }
}
