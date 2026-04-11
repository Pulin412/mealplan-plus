package com.mealplanplus.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.GroceryList
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.remote.MealPlanApi
import com.mealplanplus.data.remote.SyncPullResponse
import com.mealplanplus.data.remote.SyncPushResponse
import com.mealplanplus.util.CrashlyticsReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SyncRepositoryTest {

    private lateinit var api: MealPlanApi
    private lateinit var mealDao: MealDao
    private lateinit var dietDao: DietDao
    private lateinit var healthMetricDao: HealthMetricDao
    private lateinit var groceryDao: GroceryDao
    private lateinit var crashlytics: CrashlyticsReporter
    private lateinit var repository: SyncRepository

    private val userId = 1L
    private val firebaseUid = "firebase-uid-123"

    private fun fakeMeal() = Meal(
        id = 1L, name = "Lunch",
        updatedAt = 1000L, syncedAt = null
    )

    private fun fakeDiet() = Diet(
        id = 1L, name = "Keto", updatedAt = 1000L, syncedAt = null
    )

    private fun fakeGroceryList() = GroceryList(
        id = 1L, userId = userId, name = "Week 1", updatedAt = 1000L
    )

    @Before
    fun setUp() {
        api = mockk()
        mealDao = mockk(relaxed = true)
        dietDao = mockk(relaxed = true)
        healthMetricDao = mockk(relaxed = true)
        groceryDao = mockk(relaxed = true)
        crashlytics = mockk(relaxed = true)

        val firebaseUser = mockk<FirebaseUser>()
        every { firebaseUser.uid } returns firebaseUid
        val firebaseAuth = mockk<FirebaseAuth>()
        every { firebaseAuth.currentUser } returns firebaseUser

        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth

        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        repository = SyncRepository(api, mealDao, dietDao, healthMetricDao, groceryDao, crashlytics)
    }

    @After
    fun tearDown() {
        unmockkStatic(FirebaseAuth::class)
        unmockkStatic(Log::class)
    }

    // ── push — success ────────────────────────────────────────────────────────

    @Test
    fun push_withUnsyncedMeals_returnsAcceptedCount() = runTest {
        coEvery { mealDao.getUnsyncedMeals() } returns listOf(fakeMeal())
        coEvery { dietDao.getUnsyncedDiets() } returns emptyList()
        coEvery { healthMetricDao.getUnsyncedMetrics(userId) } returns emptyList()
        coEvery { groceryDao.getUnsyncedGroceryLists(userId) } returns emptyList()
        coEvery { api.push(any()) } returns SyncPushResponse(accepted = 1)

        val result = repository.push(userId)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow())
    }

    @Test
    fun push_withNoUnsyncedItems_returnsZeroWithoutCallingApi() = runTest {
        coEvery { mealDao.getUnsyncedMeals() } returns emptyList()
        coEvery { dietDao.getUnsyncedDiets() } returns emptyList()
        coEvery { healthMetricDao.getUnsyncedMetrics(userId) } returns emptyList()
        coEvery { groceryDao.getUnsyncedGroceryLists(userId) } returns emptyList()

        val result = repository.push(userId)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrThrow())
        coVerify(exactly = 0) { api.push(any()) }
    }

    @Test
    fun push_success_logsBreadcrumb() = runTest {
        coEvery { mealDao.getUnsyncedMeals() } returns listOf(fakeMeal())
        coEvery { dietDao.getUnsyncedDiets() } returns emptyList()
        coEvery { healthMetricDao.getUnsyncedMetrics(userId) } returns emptyList()
        coEvery { groceryDao.getUnsyncedGroceryLists(userId) } returns emptyList()
        coEvery { api.push(any()) } returns SyncPushResponse(accepted = 1)

        repository.push(userId)

        coVerify(exactly = 1) { crashlytics.log("sync_push", "accepted=1") }
    }

    // ── push — failure ────────────────────────────────────────────────────────

    @Test
    fun push_apiThrows_returnsFailure() = runTest {
        coEvery { mealDao.getUnsyncedMeals() } returns listOf(fakeMeal())
        coEvery { dietDao.getUnsyncedDiets() } returns emptyList()
        coEvery { healthMetricDao.getUnsyncedMetrics(userId) } returns emptyList()
        coEvery { groceryDao.getUnsyncedGroceryLists(userId) } returns emptyList()
        coEvery { api.push(any()) } throws RuntimeException("server error")

        val result = repository.push(userId)

        assertTrue(result.isFailure)
    }

    @Test
    fun push_apiThrows_reportsNonFatal() = runTest {
        val exception = RuntimeException("503")
        coEvery { mealDao.getUnsyncedMeals() } returns listOf(fakeMeal())
        coEvery { dietDao.getUnsyncedDiets() } returns emptyList()
        coEvery { healthMetricDao.getUnsyncedMetrics(userId) } returns emptyList()
        coEvery { groceryDao.getUnsyncedGroceryLists(userId) } returns emptyList()
        coEvery { api.push(any()) } throws exception

        repository.push(userId)

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(
                exception,
                context = "sync_push",
                extras = mapOf("userId" to userId.toString())
            )
        }
    }

    @Test
    fun push_success_doesNotReportNonFatal() = runTest {
        coEvery { mealDao.getUnsyncedMeals() } returns emptyList()
        coEvery { dietDao.getUnsyncedDiets() } returns emptyList()
        coEvery { healthMetricDao.getUnsyncedMetrics(userId) } returns emptyList()
        coEvery { groceryDao.getUnsyncedGroceryLists(userId) } returns emptyList()

        repository.push(userId)

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any(), any()) }
    }

    // ── pull — success ────────────────────────────────────────────────────────

    @Test
    fun pull_success_returnsSuccessResult() = runTest {
        coEvery { api.pull(any()) } returns SyncPullResponse()

        val result = repository.pull(userId, since = 0L)

        assertTrue(result.isSuccess)
    }

    @Test
    fun pull_success_logsBreadcrumb() = runTest {
        coEvery { api.pull(any()) } returns SyncPullResponse()

        repository.pull(userId, since = 0L)

        coVerify(exactly = 1) {
            crashlytics.log("sync_pull", match { it.contains("meals=") && it.contains("diets=") })
        }
    }

    // ── pull — failure ────────────────────────────────────────────────────────

    @Test
    fun pull_apiThrows_returnsFailure() = runTest {
        coEvery { api.pull(any()) } throws RuntimeException("network error")

        val result = repository.pull(userId, since = 0L)

        assertTrue(result.isFailure)
    }

    @Test
    fun pull_apiThrows_reportsNonFatal() = runTest {
        val exception = RuntimeException("timeout")
        coEvery { api.pull(any()) } throws exception

        repository.pull(userId, since = 0L)

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(
                exception,
                context = "sync_pull",
                extras = mapOf("userId" to userId.toString())
            )
        }
    }

    @Test
    fun pull_success_doesNotReportNonFatal() = runTest {
        coEvery { api.pull(any()) } returns SyncPullResponse()

        repository.pull(userId, since = 0L)

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any(), any()) }
    }
}
