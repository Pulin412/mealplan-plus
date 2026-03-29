package com.mealplanplus.util

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around [FirebaseRemoteConfig].
 *
 * All feature-flag and remote-config reads flow through this class so that:
 *  - Call sites stay decoupled from the Firebase SDK.
 *  - Unit tests can inject a no-op substitute without needing to mock static singletons.
 *  - All flags have safe local defaults via [FeatureFlag] before the first fetch.
 *
 * Usage:
 *   if (remoteConfig.isEnabled(FeatureFlag.BARCODE_SCANNER)) { ... }
 *   val msg = remoteConfig.getString("welcome_message")
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

    private val TAG = "RemoteConfigManager"

    private val remoteConfig: FirebaseRemoteConfig
        get() = FirebaseRemoteConfig.getInstance()

    // ── Defaults ──────────────────────────────────────────────────────────────

    /**
     * Register in-app defaults for all [FeatureFlag] entries.
     * Must be called before [fetchAndActivate] so that flags have correct values
     * even before the first successful fetch.
     */
    fun applyDefaults() {
        val defaults: Map<String, Any> = FeatureFlag.entries.associate { it.key to it.defaultValue }
        remoteConfig.setDefaultsAsync(defaults)
    }

    // ── Fetch & activate ──────────────────────────────────────────────────────

    /**
     * Fetch the latest config from Firebase and activate it.
     * Returns `true` if new values were fetched and activated, `false` if the
     * cached values are already up-to-date or if the fetch failed (safe fallback
     * to last-known / default values).
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            Log.w(TAG, "Remote Config fetch failed — using cached/default values: ${e.message}")
            false
        }
    }

    // ── Feature flag reads ────────────────────────────────────────────────────

    /**
     * Returns whether a [FeatureFlag] is currently enabled.
     * Reads from the activated (or default) config cache — always fast, never blocking.
     */
    fun isEnabled(flag: FeatureFlag): Boolean = remoteConfig.getBoolean(flag.key)

    // ── Generic reads ─────────────────────────────────────────────────────────

    /** Read any string key from Remote Config (e.g. copy, URLs, JSON). */
    fun getString(key: String): String = remoteConfig.getString(key)

    /** Read any long key from Remote Config (e.g. limits, thresholds). */
    fun getLong(key: String): Long = remoteConfig.getLong(key)

    /** Read any double key from Remote Config (e.g. rates, multipliers). */
    fun getDouble(key: String): Double = remoteConfig.getDouble(key)
}
