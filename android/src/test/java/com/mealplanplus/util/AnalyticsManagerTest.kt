package com.mealplanplus.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class AnalyticsManagerTest {

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var context: Context
    private lateinit var manager: AnalyticsManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        firebaseAnalytics = mockk(relaxed = true)
        mockkStatic(FirebaseAnalytics::class)
        every { FirebaseAnalytics.getInstance(any()) } returns firebaseAnalytics

        // Bundle is an Android class not available in unit tests — mock its constructor
        // and all put* methods so bundleOf(...) calls don't throw
        mockkConstructor(Bundle::class)
        every { anyConstructed<Bundle>().putString(any(), any()) } returns Unit
        every { anyConstructed<Bundle>().putBoolean(any(), any()) } returns Unit
        every { anyConstructed<Bundle>().putLong(any(), any()) } returns Unit
        every { anyConstructed<Bundle>().putCharSequence(any(), any()) } returns Unit

        manager = AnalyticsManager(context)
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAnalytics::class)
        unmockkConstructor(Bundle::class)
    }

    // ── user identity ─────────────────────────────────────────────────────────

    @Test
    fun setUserId_delegatesToFirebase() {
        manager.setUserId("42")
        verify(exactly = 1) { firebaseAnalytics.setUserId("42") }
    }

    @Test
    fun clearUserId_setsNullOnFirebase() {
        manager.clearUserId()
        verify(exactly = 1) { firebaseAnalytics.setUserId(null) }
    }

    @Test
    fun setUserProperty_delegatesToFirebase() {
        manager.setUserProperty("plan_type", "free")
        verify(exactly = 1) { firebaseAnalytics.setUserProperty("plan_type", "free") }
    }

    // ── auth events ───────────────────────────────────────────────────────────

    @Test
    fun logSignIn_logsSignInEvent() {
        manager.logSignIn("email")
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.SIGN_IN, any()) }
    }

    @Test
    fun logSignIn_google_logsSignInEvent() {
        manager.logSignIn("google")
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.SIGN_IN, any()) }
    }

    @Test
    fun logSignUp_logsSignUpEvent() {
        manager.logSignUp("email")
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.SIGN_UP, any()) }
    }

    @Test
    fun logSignOut_logsSignOutEvent() {
        manager.logSignOut()
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.SIGN_OUT, any()) }
    }

    // ── food events ───────────────────────────────────────────────────────────

    @Test
    fun logFoodSearched_logsFoodSearchedEvent() {
        manager.logFoodSearched(query = "chicken", source = "usda")
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.FOOD_SEARCHED, any()) }
    }

    @Test
    fun logFoodAdded_logsFoodAddedEvent() {
        manager.logFoodAdded(foodName = "Chicken Breast", source = "usda")
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.FOOD_ADDED, any()) }
    }

    @Test
    fun logBarcodeScanned_success_logsBarcodeScannedEvent() {
        manager.logBarcodeScanned(success = true)
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.BARCODE_SCANNED, any()) }
    }

    @Test
    fun logBarcodeScanned_failure_logsBarcodeScannedEvent() {
        manager.logBarcodeScanned(success = false)
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.BARCODE_SCANNED, any()) }
    }

    // ── diet events ───────────────────────────────────────────────────────────

    @Test
    fun logDietCreated_logsDietCreatedEvent() {
        manager.logDietCreated(dietId = 7L)
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.DIET_CREATED, any()) }
    }

    @Test
    fun logDietViewed_logsDietViewedEvent() {
        manager.logDietViewed(dietId = 7L)
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.DIET_VIEWED, any()) }
    }

    // ── plan / grocery / health events ────────────────────────────────────────

    @Test
    fun logMealPlanViewed_logsMealPlanViewedEvent() {
        manager.logMealPlanViewed()
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.MEAL_PLAN_VIEWED, any()) }
    }

    @Test
    fun logGroceryListCreated_logsGroceryListCreatedEvent() {
        manager.logGroceryListCreated()
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.GROCERY_LIST_CREATED, any()) }
    }

    @Test
    fun logGroceryListGenerated_logsGroceryListGeneratedEvent() {
        manager.logGroceryListGenerated()
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.GROCERY_LIST_GENERATED, any()) }
    }

    @Test
    fun logHealthMetricLogged_logsHealthMetricEvent() {
        manager.logHealthMetricLogged(metricType = "WEIGHT")
        verify(exactly = 1) { firebaseAnalytics.logEvent(AnalyticsEvent.HEALTH_METRIC_LOGGED, any()) }
    }

    // ── screen view ───────────────────────────────────────────────────────────

    @Test
    fun logScreenView_logsScreenViewEvent() {
        manager.logScreenView("HomeScreen")
        verify(exactly = 1) { firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, any()) }
    }
}
