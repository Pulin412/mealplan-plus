package com.mealplanplus.ui.screens.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.mealplanplus.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repo: AuthRepository,
) : ViewModel() {

    /** Drives the top-level auth gate. */
    val authState: StateFlow<FirebaseUser?> = repo.authState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), repo.currentUser)

    private val _ui = MutableStateFlow(AuthUiState())
    val ui: StateFlow<AuthUiState> = _ui.asStateFlow()

    fun signIn(email: String, password: String) = run { repo.signIn(email, password) }
    fun register(email: String, password: String) = run { repo.register(email, password) }
    fun signInWithGoogle(activityContext: Context) = run { repo.signInWithGoogle(activityContext) }
    fun clearError() { _ui.value = _ui.value.copy(error = null) }

    /** Runs an auth action with shared loading/error handling. */
    private fun run(block: suspend () -> Unit) {
        viewModelScope.launch {
            _ui.value = AuthUiState(isLoading = true)
            try {
                block()
                _ui.value = AuthUiState()   // success — gate switches via authState
            } catch (e: Exception) {
                _ui.value = AuthUiState(error = e.message ?: "Something went wrong")
            }
        }
    }
}
