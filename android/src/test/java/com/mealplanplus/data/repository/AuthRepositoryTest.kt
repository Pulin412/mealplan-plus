package com.mealplanplus.data.repository

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.mealplanplus.data.local.DailyLogDao
import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.local.UserDao
import com.mealplanplus.data.local.UserDataSeeder
import com.mealplanplus.data.model.User
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.CrashlyticsReporter
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var userDao: UserDao
    private lateinit var userDataSeeder: UserDataSeeder
    private lateinit var healthMetricDao: HealthMetricDao
    private lateinit var dailyLogDao: DailyLogDao
    private lateinit var planDao: PlanDao
    private lateinit var groceryDao: GroceryDao
    private lateinit var dietDao: DietDao
    private lateinit var mealDao: MealDao
    private lateinit var crashlytics: CrashlyticsReporter
    private lateinit var context: Context
    private lateinit var repository: AuthRepository

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser

    private val fakeUser = User(id = 1L, email = "test@example.com", passwordHash = "", displayName = "Test")

    @Before
    fun setUp() {
        userDao = mockk(relaxed = true)
        userDataSeeder = mockk(relaxed = true)
        healthMetricDao = mockk(relaxed = true)
        dailyLogDao = mockk(relaxed = true)
        planDao = mockk(relaxed = true)
        groceryDao = mockk(relaxed = true)
        dietDao = mockk(relaxed = true)
        mealDao = mockk(relaxed = true)
        crashlytics = mockk(relaxed = true)
        context = mockk(relaxed = true)

        firebaseUser = mockk(relaxed = true)
        every { firebaseUser.uid } returns "uid-abc"
        every { firebaseUser.email } returns "test@example.com"
        every { firebaseUser.displayName } returns "Test"

        firebaseAuth = mockk(relaxed = true)
        mockkStatic(FirebaseAuth::class)
        every { FirebaseAuth.getInstance() } returns firebaseAuth

        mockkObject(AuthPreferences)
        coEvery { AuthPreferences.setLoggedIn(any(), any()) } returns Unit
        coEvery { AuthPreferences.setProviderSubjectMapping(any(), any(), any(), any()) } returns Unit
        coEvery { AuthPreferences.getUserIdForProviderSubject(any(), any(), any()) } returns null
        coEvery { AuthPreferences.clearAuth(any()) } returns Unit

        mockkStatic("kotlinx.coroutines.tasks.TasksKt")
        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } returns false
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns mockk(relaxed = true)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        repository = AuthRepository(
            userDao, userDataSeeder, healthMetricDao, dailyLogDao,
            planDao, groceryDao, dietDao, mealDao, crashlytics, context
        )
    }

    @After
    fun tearDown() {
        unmockkObject(AuthPreferences)
        unmockkStatic(FirebaseAuth::class)
        unmockkStatic("kotlinx.coroutines.tasks.TasksKt")
        unmockkStatic(TextUtils::class)
        unmockkStatic(Uri::class)
        unmockkStatic(Log::class)
    }

    // ── signInWithEmail — success ─────────────────────────────────────────────

    @Test
    fun signInWithEmail_success_setsUserIdAndLogsBreadcrumb() = runTest {
        val authResult = mockk<AuthResult>()
        every { authResult.user } returns firebaseUser
        val task = mockk<Task<AuthResult>>()
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns task
        coEvery { task.await() } returns authResult
        coEvery { userDao.getUserByEmail("test@example.com") } returns fakeUser

        repository.signInWithEmail("test@example.com", "password")

        coVerify(exactly = 1) { crashlytics.setUserId("1") }
        coVerify(exactly = 1) { crashlytics.log("sign_in", "provider=email") }
    }

    // ── signInWithEmail — unexpected exception ────────────────────────────────

    @Test
    fun signInWithEmail_unexpectedException_reportsNonFatal() = runTest {
        val exception = RuntimeException("unexpected")
        val task = mockk<Task<AuthResult>>()
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns task
        coEvery { task.await() } throws exception

        repository.signInWithEmail("test@example.com", "password")

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(exception, context = "sign_in_email")
        }
    }

    @Test
    fun signInWithEmail_knownFirebaseException_doesNotReportNonFatal() = runTest {
        val task = mockk<Task<AuthResult>>()
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns task
        coEvery { task.await() } throws mockk<FirebaseAuthInvalidCredentialsException>(relaxed = true)

        repository.signInWithEmail("test@example.com", "wrongpassword")

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any()) }
    }

    // ── signUpWithEmail — success ─────────────────────────────────────────────

    @Test
    fun signUpWithEmail_success_setsUserIdAndLogsBreadcrumb() = runTest {
        val authResult = mockk<AuthResult>()
        every { authResult.user } returns firebaseUser
        val createTask = mockk<Task<AuthResult>>()
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns createTask
        coEvery { createTask.await() } returns authResult

        // updateProfile task must also be mocked — it's called with the display name
        val updateProfileTask = mockk<Task<Void>>()
        every { firebaseUser.updateProfile(any()) } returns updateProfileTask
        coEvery { updateProfileTask.await() } returns mockk()

        coEvery { userDao.insertUser(any()) } returns 2L

        repository.signUpWithEmail("test@example.com", "password123", "Test")

        coVerify(exactly = 1) { crashlytics.setUserId("2") }
        coVerify(exactly = 1) { crashlytics.log("sign_up", "provider=email") }
    }

    // ── signUpWithEmail — unexpected exception ────────────────────────────────

    @Test
    fun signUpWithEmail_unexpectedException_reportsNonFatal() = runTest {
        val exception = RuntimeException("db failure")
        val task = mockk<Task<AuthResult>>()
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns task
        coEvery { task.await() } throws exception

        repository.signUpWithEmail("test@example.com", "password", "Test")

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(exception, context = "sign_up_email")
        }
    }

    @Test
    fun signUpWithEmail_collisionException_doesNotReportNonFatal() = runTest {
        val task = mockk<Task<AuthResult>>()
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns task
        coEvery { task.await() } throws mockk<FirebaseAuthUserCollisionException>(relaxed = true)

        repository.signUpWithEmail("existing@example.com", "password", "Test")

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any()) }
    }

    // ── signOut ───────────────────────────────────────────────────────────────

    @Test
    fun signOut_clearsUserIdAndLogsBreadcrumb() = runTest {
        repository.signOut()

        coVerify(exactly = 1) { crashlytics.clearUserId() }
        coVerify(exactly = 1) { crashlytics.log("sign_out") }
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    fun updateProfile_dbException_reportsNonFatal() = runTest {
        val exception = RuntimeException("constraint violation")
        coEvery { userDao.updateUser(any()) } throws exception

        repository.updateProfile(fakeUser)

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(exception, context = "update_profile")
        }
    }

    @Test
    fun updateProfile_success_doesNotReportNonFatal() = runTest {
        coEvery { userDao.updateUser(any()) } returns Unit

        repository.updateProfile(fakeUser)

        coVerify(exactly = 0) { crashlytics.recordNonFatal(any(), any()) }
    }

    // ── deleteAccount ─────────────────────────────────────────────────────────

    @Test
    fun deleteAccount_success_logsAccountDeletedAndClearsUserId() = runTest {
        every { firebaseAuth.currentUser } returns firebaseUser
        val deleteTask = mockk<Task<Void>>(relaxed = true)
        every { firebaseUser.delete() } returns deleteTask
        coEvery { deleteTask.await() } returns mockk()

        repository.deleteAccount(fakeUser.id)

        coVerify(exactly = 1) { crashlytics.log("account_deleted") }
        // clearUserId() is called once in deleteAccount and once more inside signOut()
        coVerify(atLeast = 1) { crashlytics.clearUserId() }
    }

    @Test
    fun deleteAccount_exception_reportsNonFatal() = runTest {
        val exception = RuntimeException("delete failed")
        coEvery { userDao.deleteUser(any()) } throws exception

        repository.deleteAccount(fakeUser.id)

        coVerify(exactly = 1) {
            crashlytics.recordNonFatal(exception, context = "delete_account")
        }
    }
}
