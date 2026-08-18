package com.mealplanplus.ui.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.api.AdminApi
import com.mealplanplus.data.generated.model.FeatureFlagResponse
import com.mealplanplus.data.generated.model.FeatureFlagUpdateRequest
import com.mealplanplus.data.remote.ApiErrors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUiState(
    val loading: Boolean = true,
    val flags: List<FeatureFlagResponse> = emptyList(),
    val error: String? = null,
    /** Keys currently being written, so their row shows a pending state and ignores re-taps. */
    val pending: Set<String> = emptySet(),
)

/**
 * Admin feature-flag management. Online-only (no Room) — the server gates every call by the email
 * allowlist and returns 403 for non-admins, so this screen is only reachable when [UserResponse.isAdmin].
 * Single StateFlow updated exclusively via [update] so concurrent load/toggle can't clobber each other.
 */
@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminApi: AdminApi,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { adminApi.listFeatureFlags() }
                .onSuccess { resp ->
                    if (resp.isSuccessful) {
                        _state.update { it.copy(loading = false, flags = resp.body().orEmpty(), error = null) }
                    } else {
                        _state.update { it.copy(loading = false, error = ApiErrors.messageFor(resp)) }
                    }
                }
                .onFailure { e -> _state.update { it.copy(loading = false, error = ApiErrors.messageFor(e)) } }
        }
    }

    /** Optimistically flip the row, PUT the change, and reconcile with the server's echo (revert on failure). */
    fun setEnabled(key: String, enabled: Boolean) {
        if (_state.value.pending.contains(key)) return
        _state.update { s ->
            s.copy(
                pending = s.pending + key,
                flags = s.flags.map { if (it.key == key) it.copy(enabled = enabled) else it },
            )
        }
        viewModelScope.launch {
            runCatching { adminApi.setFeatureFlag(key, FeatureFlagUpdateRequest(enabled = enabled)) }
                .onSuccess { resp ->
                    val updated = resp.body()
                    if (resp.isSuccessful && updated != null) {
                        _state.update { s ->
                            s.copy(
                                pending = s.pending - key,
                                flags = s.flags.map { if (it.key == key) updated else it },
                            )
                        }
                    } else {
                        revert(key, ApiErrors.messageFor(resp))
                    }
                }
                .onFailure { e -> revert(key, ApiErrors.messageFor(e)) }
        }
    }

    private fun revert(key: String, message: String) {
        _state.update { s ->
            s.copy(
                pending = s.pending - key,
                error = message,
                flags = s.flags.map { if (it.key == key) it.copy(enabled = !it.enabled) else it },
            )
        }
    }
}
