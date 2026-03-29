package com.mealplanplus.util

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemoteConfigManagerTest {

    private lateinit var firebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var manager: RemoteConfigManager

    @Before
    fun setUp() {
        firebaseRemoteConfig = mockk(relaxed = true)
        mockkStatic(FirebaseRemoteConfig::class)
        every { FirebaseRemoteConfig.getInstance() } returns firebaseRemoteConfig
        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        manager = RemoteConfigManager()
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseRemoteConfig::class)
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
        unmockkStatic(Log::class)
    }

    // ── applyDefaults ─────────────────────────────────────────────────────────

    @Test
    fun applyDefaults_callsSetDefaultsAsync() {
        manager.applyDefaults()
        verify(exactly = 1) { firebaseRemoteConfig.setDefaultsAsync(any<Map<String, Any>>()) }
    }

    @Test
    fun applyDefaults_includesAllFlagKeysInDefaultsMap() {
        val slot = slot<Map<String, Any>>()
        every { firebaseRemoteConfig.setDefaultsAsync(capture(slot)) } returns mockk(relaxed = true)

        manager.applyDefaults()

        val keys = slot.captured.keys
        FeatureFlag.entries.forEach { flag ->
            assertTrue("Missing key: ${flag.key}", keys.contains(flag.key))
        }
    }

    @Test
    fun applyDefaults_defaultValuesMatchFlagDefaults() {
        val slot = slot<Map<String, Any>>()
        every { firebaseRemoteConfig.setDefaultsAsync(capture(slot)) } returns mockk(relaxed = true)

        manager.applyDefaults()

        FeatureFlag.entries.forEach { flag ->
            assertEquals(
                "Wrong default for ${flag.key}",
                flag.defaultValue,
                slot.captured[flag.key]
            )
        }
    }

    // ── fetchAndActivate ──────────────────────────────────────────────────────

    @Test
    fun fetchAndActivate_returnsTrue_whenFirebaseReturnsTrue() = runTest {
        val task = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns task
        coEvery { task.await() } returns true

        val result = manager.fetchAndActivate()

        assertTrue(result)
    }

    @Test
    fun fetchAndActivate_returnsFalse_whenFirebaseReturnsFalse() = runTest {
        val task = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns task
        coEvery { task.await() } returns false

        val result = manager.fetchAndActivate()

        assertFalse(result)
    }

    @Test
    fun fetchAndActivate_returnsTrue_onException_usingDefault() = runTest {
        val task = mockk<Task<Boolean>>()
        every { firebaseRemoteConfig.fetchAndActivate() } returns task
        coEvery { task.await() } throws RuntimeException("network error")

        // Should not throw — returns false (cached/default values still usable)
        val result = manager.fetchAndActivate()

        assertFalse(result)
    }

    // ── isEnabled ─────────────────────────────────────────────────────────────

    @Test
    fun isEnabled_delegatesToGetBoolean_withFlagKey() {
        every { firebaseRemoteConfig.getBoolean("barcode_scanner_enabled") } returns true

        val result = manager.isEnabled(FeatureFlag.BARCODE_SCANNER)

        assertTrue(result)
        verify(exactly = 1) { firebaseRemoteConfig.getBoolean("barcode_scanner_enabled") }
    }

    @Test
    fun isEnabled_returnsFalse_whenFlagIsDisabled() {
        every { firebaseRemoteConfig.getBoolean("sync_enabled") } returns false

        val result = manager.isEnabled(FeatureFlag.SYNC)

        assertFalse(result)
    }

    @Test
    fun isEnabled_usesCorrectKeyForEachFlag() {
        FeatureFlag.entries.forEach { flag ->
            every { firebaseRemoteConfig.getBoolean(flag.key) } returns flag.defaultValue
            manager.isEnabled(flag)
            verify { firebaseRemoteConfig.getBoolean(flag.key) }
        }
    }

    // ── getString ─────────────────────────────────────────────────────────────

    @Test
    fun getString_delegatesToFirebase() {
        every { firebaseRemoteConfig.getString("welcome_message") } returns "Hello!"

        val result = manager.getString("welcome_message")

        assertEquals("Hello!", result)
        verify(exactly = 1) { firebaseRemoteConfig.getString("welcome_message") }
    }

    // ── getLong ───────────────────────────────────────────────────────────────

    @Test
    fun getLong_delegatesToFirebase() {
        every { firebaseRemoteConfig.getLong("max_items") } returns 50L

        val result = manager.getLong("max_items")

        assertEquals(50L, result)
        verify(exactly = 1) { firebaseRemoteConfig.getLong("max_items") }
    }

    // ── getDouble ─────────────────────────────────────────────────────────────

    @Test
    fun getDouble_delegatesToFirebase() {
        every { firebaseRemoteConfig.getDouble("discount_rate") } returns 0.15

        val result = manager.getDouble("discount_rate")

        assertEquals(0.15, result, 0.0001)
        verify(exactly = 1) { firebaseRemoteConfig.getDouble("discount_rate") }
    }
}
