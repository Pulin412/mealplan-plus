package com.mealplanplus.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.model.User
import com.mealplanplus.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getCurrentUserId().collect { userId ->
                if (userId != null) {
                    authRepository.getCurrentUser(userId).collect { user ->
                        _uiState.update { it.copy(user = user, isLoading = false) }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun updateProfile(name: String?, age: Int?, contact: String?) {
        val currentUser = _uiState.value.user ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val updatedUser = currentUser.copy(
                displayName = name ?: currentUser.displayName,
                age = age ?: currentUser.age,
                contact = contact ?: currentUser.contact
            )
            val result = authRepository.updateProfile(updatedUser)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(user = user, isSaving = false, saveSuccess = true)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(isSaving = false, error = e.message ?: "Failed to update profile")
                    }
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
