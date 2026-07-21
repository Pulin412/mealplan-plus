package com.mealplanplus.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.auth.AuthRepository
import com.mealplanplus.data.generated.api.UsersApi
import com.mealplanplus.data.local.dao.DietDao
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.local.dao.MealDao
import com.mealplanplus.data.sync.SyncCursorStore
import java.time.Instant
import com.mealplanplus.data.generated.model.UserResponse
import com.mealplanplus.data.generated.model.UserUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val user: UserResponse? = null,
    val error: String? = null,
    val saving: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val usersApi: UsersApi,
    private val authRepo: AuthRepository,
    private val foodDao: FoodDao,
    private val mealDao: MealDao,
    private val dietDao: DietDao,
    private val cursor: SyncCursorStore,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = _state.value.user == null, error = null)
            runCatching { usersApi.getMe() }
                .onSuccess { resp ->
                    val body = resp.body()
                    if (resp.isSuccessful && body != null) _state.value = _state.value.copy(loading = false, user = body)
                    else _state.value = _state.value.copy(loading = false, error = "Couldn't load profile (${resp.code()})")
                }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message) }
        }
    }

    /** Send a partial update (only changed fields set); refresh on success. */
    fun patch(update: UserUpdateRequest) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            runCatching { usersApi.updateMe(update) }
                .onSuccess { it.body()?.let { u -> _state.value = _state.value.copy(user = u) } }
                .onFailure { e -> _state.value = _state.value.copy(error = e.message) }
            _state.value = _state.value.copy(saving = false)
        }
    }

    fun signOut() = authRepo.signOut()

    /** Purge the on-device cache (foods/meals/diets + sync cursor) and sign out. */
    fun clearAllData() {
        viewModelScope.launch {
            runCatching {
                foodDao.deleteAll(); mealDao.deleteAll(); dietDao.deleteAll()
                cursor.set(Instant.EPOCH)
            }
            authRepo.signOut()
        }
    }
}
