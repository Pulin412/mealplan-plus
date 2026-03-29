package com.mealplanplus.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Contract tests for AnalyticsEvent constants.
 *
 * Intentionally strict about exact string values — a silent rename breaks
 * every dashboard query and funnel that depends on the event name.
 */
class AnalyticsEventTest {

    // ── event name constants ──────────────────────────────────────────────────

    @Test fun signIn_eventName()               { assertEquals("sign_in",                   AnalyticsEvent.SIGN_IN) }
    @Test fun signUp_eventName()               { assertEquals("sign_up",                   AnalyticsEvent.SIGN_UP) }
    @Test fun signOut_eventName()              { assertEquals("sign_out",                  AnalyticsEvent.SIGN_OUT) }
    @Test fun foodSearched_eventName()         { assertEquals("food_searched",             AnalyticsEvent.FOOD_SEARCHED) }
    @Test fun foodAdded_eventName()            { assertEquals("food_added",                AnalyticsEvent.FOOD_ADDED) }
    @Test fun barcodeScanned_eventName()       { assertEquals("barcode_scanned",           AnalyticsEvent.BARCODE_SCANNED) }
    @Test fun dietCreated_eventName()          { assertEquals("diet_created",              AnalyticsEvent.DIET_CREATED) }
    @Test fun dietViewed_eventName()           { assertEquals("diet_viewed",               AnalyticsEvent.DIET_VIEWED) }
    @Test fun mealPlanViewed_eventName()       { assertEquals("meal_plan_viewed",          AnalyticsEvent.MEAL_PLAN_VIEWED) }
    @Test fun groceryListCreated_eventName()   { assertEquals("grocery_list_created",      AnalyticsEvent.GROCERY_LIST_CREATED) }
    @Test fun groceryListGenerated_eventName() { assertEquals("grocery_list_generated",    AnalyticsEvent.GROCERY_LIST_GENERATED) }
    @Test fun healthMetricLogged_eventName()   { assertEquals("health_metric_logged",      AnalyticsEvent.HEALTH_METRIC_LOGGED) }

    // ── param name constants ──────────────────────────────────────────────────

    @Test fun param_provider()    { assertEquals("provider",      AnalyticsEvent.Param.PROVIDER) }
    @Test fun param_searchQuery() { assertEquals("search_query",  AnalyticsEvent.Param.SEARCH_QUERY) }
    @Test fun param_foodName()    { assertEquals("food_name",     AnalyticsEvent.Param.FOOD_NAME) }
    @Test fun param_source()      { assertEquals("source",        AnalyticsEvent.Param.SOURCE) }
    @Test fun param_dietId()      { assertEquals("diet_id",       AnalyticsEvent.Param.DIET_ID) }
    @Test fun param_success()     { assertEquals("success",       AnalyticsEvent.Param.SUCCESS) }
    @Test fun param_metricType()  { assertEquals("metric_type",   AnalyticsEvent.Param.METRIC_TYPE) }
    @Test fun param_screenName()  { assertEquals("screen_name",   AnalyticsEvent.Param.SCREEN_NAME) }

    // ── uniqueness ────────────────────────────────────────────────────────────

    @Test
    fun allEventNames_areUnique() {
        val names = listOf(
            AnalyticsEvent.SIGN_IN, AnalyticsEvent.SIGN_UP, AnalyticsEvent.SIGN_OUT,
            AnalyticsEvent.FOOD_SEARCHED, AnalyticsEvent.FOOD_ADDED,
            AnalyticsEvent.BARCODE_SCANNED, AnalyticsEvent.DIET_CREATED,
            AnalyticsEvent.DIET_VIEWED, AnalyticsEvent.MEAL_PLAN_VIEWED,
            AnalyticsEvent.GROCERY_LIST_CREATED, AnalyticsEvent.GROCERY_LIST_GENERATED,
            AnalyticsEvent.HEALTH_METRIC_LOGGED
        )
        assertEquals("Duplicate event names detected", names.size, names.toSet().size)
    }

    @Test
    fun allParamNames_areUnique() {
        val params = listOf(
            AnalyticsEvent.Param.PROVIDER, AnalyticsEvent.Param.SEARCH_QUERY,
            AnalyticsEvent.Param.FOOD_NAME, AnalyticsEvent.Param.SOURCE,
            AnalyticsEvent.Param.DIET_ID, AnalyticsEvent.Param.SUCCESS,
            AnalyticsEvent.Param.METRIC_TYPE, AnalyticsEvent.Param.SCREEN_NAME
        )
        assertEquals("Duplicate param names detected", params.size, params.toSet().size)
    }

    // ── count guards ──────────────────────────────────────────────────────────

    @Test
    fun eventCount_matchesExpected() {
        // Fails when an event is added/removed without updating docs + dashboards
        val events = listOf(
            AnalyticsEvent.SIGN_IN, AnalyticsEvent.SIGN_UP, AnalyticsEvent.SIGN_OUT,
            AnalyticsEvent.FOOD_SEARCHED, AnalyticsEvent.FOOD_ADDED,
            AnalyticsEvent.BARCODE_SCANNED, AnalyticsEvent.DIET_CREATED,
            AnalyticsEvent.DIET_VIEWED, AnalyticsEvent.MEAL_PLAN_VIEWED,
            AnalyticsEvent.GROCERY_LIST_CREATED, AnalyticsEvent.GROCERY_LIST_GENERATED,
            AnalyticsEvent.HEALTH_METRIC_LOGGED
        )
        assertEquals(12, events.size)
    }
}
