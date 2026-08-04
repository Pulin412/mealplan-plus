package com.mealplanplus.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.auth.AuthRepository
import com.mealplanplus.data.cache.ResponseCache
import com.mealplanplus.data.cache.render
import com.mealplanplus.data.generated.api.UsersApi
import com.mealplanplus.data.local.dao.DietDao
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.local.dao.MealDao
import com.mealplanplus.data.sync.SyncCursorStore
import com.mealplanplus.ui.onboarding.OnboardingStore
import java.time.Instant
import com.mealplanplus.data.generated.model.UserResponse
import com.mealplanplus.data.generated.model.UserUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
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
    private val onboardingStore: OnboardingStore,
    private val responseCache: ResponseCache,
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state

    /** The in-flight background refresh, so a local edit ([patch]) can cancel it before writing. */
    private var loadJob: Job? = null

    init { load() }

    /**
     * Read-through cached load: paints the last-known profile instantly on a cold open (no spinner,
     * no waiting on a Cloud Run cold start), then refreshes from the server in the background.
     */
    fun load() {
        loadJob = viewModelScope.launch {
            responseCache.stream<UserResponse>("profile") {
                usersApi.getMe().let { r ->
                    r.body().takeIf { r.isSuccessful }
                        ?: throw IllegalStateException("Couldn't load profile (${r.code()})")
                }
            }.collect { res ->
                _state.update { st ->
                    val r = res.render(hasContent = st.user != null)
                    st.copy(
                        user = if (r.keep) st.user else r.value ?: st.user,
                        loading = r.loading,
                        error = r.error,
                    )
                }
            }
        }
    }

    /** Send a partial update (only changed fields set); refresh on success. */
    fun patch(update: UserUpdateRequest) {
        // Cancel any in-flight background refresh so a stale getMe can't land after and revert the
        // edit; updateMe's response is the newest server state, so we adopt it as authoritative.
        loadJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(saving = true) }
            runCatching { usersApi.updateMe(update) }
                .onSuccess { resp -> resp.body()?.let { u -> _state.update { st -> st.copy(user = u) } } }
                .onFailure { e -> _state.update { it.copy(error = e.message) } }
            _state.update { it.copy(saving = false) }
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

    /**
     * Right-to-erasure: delete all server data, wipe the local cache + onboarding flag, delete the
     * Firebase auth account (option A), then sign out. [onError] fires only if the server delete
     * fails (nothing is signed out in that case).
     */
    fun deleteAccount(onError: (String) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true)
            val resp = runCatching { usersApi.deleteMe() }.getOrNull()
            if (resp == null || !resp.isSuccessful) {
                _state.value = _state.value.copy(saving = false)
                onError(resp?.let { "Delete failed (${it.code()})" } ?: "Network error — please try again")
                return@launch
            }
            runCatching {
                foodDao.deleteAll(); mealDao.deleteAll(); dietDao.deleteAll()
                cursor.set(Instant.EPOCH)
                onboardingStore.reset()
            }
            runCatching { authRepo.deleteCurrentUser() }   // non-fatal: server data already erased
            authRepo.signOut()                              // authState → null → back to login
        }
    }
}
