package com.mealplanplus.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AgentUiState(
    val reply: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AgentViewModel @Inject constructor(
    private val agentRepository: AgentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    fun sendMessage(message: String, date: String? = null, slot: String? = null) {
        if (message.isBlank()) return
        _uiState.value = AgentUiState(isLoading = true)
        viewModelScope.launch {
            agentRepository.chat(message, date, slot)
                .onSuccess { reply ->
                    _uiState.value = AgentUiState(reply = reply)
                }
                .onFailure { error ->
                    _uiState.value = AgentUiState(error = error.message ?: "Something went wrong")
                }
        }
    }

    fun clearReply() {
        _uiState.value = AgentUiState()
    }
}
