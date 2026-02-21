package com.mealplanplus.ui.screens.auth

import com.mealplanplus.data.model.User
import com.mealplanplus.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel

    private val fakeUser = User(
        id = 1L,
        email = "test@example.com",
        passwordHash = User.hashPassword("password123"),
        displayName = "Test User"
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)
        every { authRepository.isLoggedIn() } returns flowOf(false)
        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- signIn ---

    @Test
    fun signIn_blankEmail_setsError() = runTest {
        viewModel.signIn("", "password123")
        assertEquals("Email and password required", viewModel.uiState.value.error)
    }

    @Test
    fun signIn_blankPassword_setsError() = runTest {
        viewModel.signIn("test@example.com", "")
        assertEquals("Email and password required", viewModel.uiState.value.error)
    }

    @Test
    fun signIn_success_setsLoggedInAndUser() = runTest {
        coEvery { authRepository.signInWithEmail("test@example.com", "password123") } returns
            Result.success(fakeUser)

        viewModel.signIn("test@example.com", "password123")

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals(fakeUser, state.user)
        assertNull(state.error)
    }

    @Test
    fun signIn_wrongPassword_setsError() = runTest {
        coEvery { authRepository.signInWithEmail(any(), any()) } returns
            Result.failure(Exception("Invalid password"))

        viewModel.signIn("test@example.com", "wrongpass")

        val state = viewModel.uiState.value
        assertEquals("Invalid password", state.error)
        assertTrue(!state.isLoggedIn)
    }

    // --- signUp ---

    @Test
    fun signUp_blankName_setsError() = runTest {
        viewModel.signUp("test@example.com", "password123", "password123", "")
        assertEquals("All fields are required", viewModel.uiState.value.error)
    }

    @Test
    fun signUp_blankEmail_setsError() = runTest {
        viewModel.signUp("", "password123", "password123", "Test User")
        assertEquals("All fields are required", viewModel.uiState.value.error)
    }

    @Test
    fun signUp_passwordMismatch_setsError() = runTest {
        viewModel.signUp("test@example.com", "password123", "different", "Test User")
        assertEquals("Passwords do not match", viewModel.uiState.value.error)
    }

    @Test
    fun signUp_shortPassword_setsError() = runTest {
        viewModel.signUp("test@example.com", "abc", "abc", "Test User")
        assertEquals("Password must be at least 6 characters", viewModel.uiState.value.error)
    }

    @Test
    fun signUp_success_setsLoggedIn() = runTest {
        coEvery { authRepository.signUpWithEmail("test@example.com", "password123", "Test User") } returns
            Result.success(fakeUser)

        viewModel.signUp("test@example.com", "password123", "password123", "Test User")

        val state = viewModel.uiState.value
        assertTrue(state.isLoggedIn)
        assertEquals(fakeUser, state.user)
        assertNull(state.error)
    }

    @Test
    fun signUp_duplicateEmail_setsError() = runTest {
        coEvery { authRepository.signUpWithEmail(any(), any(), any()) } returns
            Result.failure(Exception("Email already registered"))

        viewModel.signUp("test@example.com", "password123", "password123", "Test User")

        assertEquals("Email already registered", viewModel.uiState.value.error)
    }

    // --- forgotPassword ---

    @Test
    fun forgotPassword_blankEmail_setsError() = runTest {
        viewModel.forgotPassword("")
        assertEquals("Please enter your email address", viewModel.uiState.value.error)
    }

    @Test
    fun forgotPassword_emailNotFound_setsError() = runTest {
        coEvery { authRepository.getUserByEmail(any()) } returns null

        viewModel.forgotPassword("unknown@example.com")

        assertEquals("No account found with this email", viewModel.uiState.value.error)
    }

    @Test
    fun forgotPassword_emailFound_setsForgotPasswordResult() = runTest {
        coEvery { authRepository.getUserByEmail(any()) } returns fakeUser

        viewModel.forgotPassword("test@example.com")

        val state = viewModel.uiState.value
        assertNotNull(state.forgotPasswordResult)
        assertNull(state.error)
        assertTrue(state.forgotPasswordResult!!.contains("security reasons"))
    }

    // --- signOut ---

    @Test
    fun signOut_clearsStateAndCallsRepo() = runTest {
        // Start logged in
        every { authRepository.isLoggedIn() } returns flowOf(true)
        viewModel = AuthViewModel(authRepository)

        viewModel.signOut()

        val state = viewModel.uiState.value
        assertTrue(!state.isLoggedIn)
        assertNull(state.user)
        coVerify { authRepository.signOut() }
    }

    // --- clearError ---

    @Test
    fun clearError_resetsError() = runTest {
        viewModel.signIn("", "")  // sets error
        assertEquals("Email and password required", viewModel.uiState.value.error)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // --- clearForgotPasswordResult ---

    @Test
    fun clearForgotPasswordResult_resetsResult() = runTest {
        coEvery { authRepository.getUserByEmail(any()) } returns fakeUser
        viewModel.forgotPassword("test@example.com")
        assertNotNull(viewModel.uiState.value.forgotPasswordResult)

        viewModel.clearForgotPasswordResult()
        assertNull(viewModel.uiState.value.forgotPasswordResult)
    }
}
