package com.mealplanplus.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.model.ProviderStatus
import com.mealplanplus.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiProvidersUiState(
    val loading: Boolean = true,
    val providers: List<ProviderStatus> = emptyList(),
    val error: String? = null,
)

/** Read-only view of the server-side provider failover chain. No editing in v1 (no write endpoint). */
@HiltViewModel
class AiProvidersViewModel @Inject constructor(
    private val repo: AgentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AiProvidersUiState())
    val state: StateFlow<AiProvidersUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val result = repo.providers()
            _state.update { s ->
                result.fold(
                    onSuccess = { p -> s.copy(loading = false, providers = p, error = null) },
                    onFailure = { e -> s.copy(loading = false, error = e.message ?: "Couldn't load providers.") },
                )
            }
        }
    }
}
