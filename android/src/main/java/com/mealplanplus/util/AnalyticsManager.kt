package com.mealplanplus.util

import android.content.Context
import android.os.Bundle
import androidx.core.os.bundleOf
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [FirebaseAnalytics].
 *
 * All analytics event logging flows through this class so that:
 *  - Call sites stay decoupled from the Firebase SDK.
 *  - Unit tests can inject a substitute by mocking [FirebaseAnalytics.getInstance].
 *  - Every event name is a constant from [AnalyticsEvent] — no raw strings at call sites.
 *
 * Usage:
 *   analytics.logSignIn("email")
 *   analytics.logFoodSearched(query = "chicken", source = "usda")
 *   analytics.logScreenView("HomeScreen")
 */
@Singleton
class AnalyticsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val analytics: FirebaseAnalytics
        get() = FirebaseAnalytics.getInstance(context)

    // ── User identity ─────────────────────────────────────────────────────────

    /** Associate a local user ID with all subsequent events. Call after successful sign-in. */
    fun setUserId(userId: String) {
        analytics.setUserId(userId)
    }

    /** Clear the user identity on sign-out. */
    fun clearUserId() {
        analytics.setUserId(null)
    }

    /** Set a persistent user property visible in audience segments. */
    fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
    }

    // ── Auth events ───────────────────────────────────────────────────────────

    fun logSignIn(provider: String) {
        analytics.logEvent(AnalyticsEvent.SIGN_IN, bundleOf(AnalyticsEvent.Param.PROVIDER to provider))
    }

    fun logSignUp(provider: String) {
        analytics.logEvent(AnalyticsEvent.SIGN_UP, bundleOf(AnalyticsEvent.Param.PROVIDER to provider))
    }

    fun logSignOut() {
        analytics.logEvent(AnalyticsEvent.SIGN_OUT, null)
    }

    // ── Food events ───────────────────────────────────────────────────────────

    fun logFoodSearched(query: String, source: String) {
        analytics.logEvent(
            AnalyticsEvent.FOOD_SEARCHED,
            bundleOf(
                AnalyticsEvent.Param.SEARCH_QUERY to query,
                AnalyticsEvent.Param.SOURCE to source
            )
        )
    }

    fun logFoodAdded(foodName: String, source: String) {
        analytics.logEvent(
            AnalyticsEvent.FOOD_ADDED,
            bundleOf(
                AnalyticsEvent.Param.FOOD_NAME to foodName,
                AnalyticsEvent.Param.SOURCE to source
            )
        )
    }

    fun logBarcodeScanned(success: Boolean) {
        analytics.logEvent(
            AnalyticsEvent.BARCODE_SCANNED,
            bundleOf(AnalyticsEvent.Param.SUCCESS to success)
        )
    }

    // ── Diet events ───────────────────────────────────────────────────────────

    fun logDietCreated(dietId: Long) {
        analytics.logEvent(
            AnalyticsEvent.DIET_CREATED,
            bundleOf(AnalyticsEvent.Param.DIET_ID to dietId)
        )
    }

    fun logDietViewed(dietId: Long) {
        analytics.logEvent(
            AnalyticsEvent.DIET_VIEWED,
            bundleOf(AnalyticsEvent.Param.DIET_ID to dietId)
        )
    }

    // ── Meal plan events ──────────────────────────────────────────────────────

    fun logMealPlanViewed() {
        analytics.logEvent(AnalyticsEvent.MEAL_PLAN_VIEWED, null)
    }

    // ── Grocery events ────────────────────────────────────────────────────────

    fun logGroceryListCreated() {
        analytics.logEvent(AnalyticsEvent.GROCERY_LIST_CREATED, null)
    }

    fun logGroceryListGenerated() {
        analytics.logEvent(AnalyticsEvent.GROCERY_LIST_GENERATED, null)
    }

    // ── Health events ─────────────────────────────────────────────────────────

    fun logHealthMetricLogged(metricType: String) {
        analytics.logEvent(
            AnalyticsEvent.HEALTH_METRIC_LOGGED,
            bundleOf(AnalyticsEvent.Param.METRIC_TYPE to metricType)
        )
    }

    // ── Screen view ───────────────────────────────────────────────────────────

    /** Log an explicit screen view (for Compose screens where auto-tracking doesn't fire). */
    fun logScreenView(screenName: String) {
        analytics.logEvent(
            FirebaseAnalytics.Event.SCREEN_VIEW,
            bundleOf(
                FirebaseAnalytics.Param.SCREEN_NAME to screenName,
                AnalyticsEvent.Param.SCREEN_NAME to screenName
            )
        )
    }
}
