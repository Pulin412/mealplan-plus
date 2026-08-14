package com.mealplanplus.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.repository.AgentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChatRole { USER, ASSISTANT }

data class ChatMessage(val role: ChatRole, val text: String)

data class AgentChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val sending: Boolean = false,
    /** Which provider served the last reply — shown as a small chip. */
    val provider: String? = null,
)

/**
 * Drives the assistant chat. One [StateFlow]; all mutations go through `_state.update { }` so an
 * in-flight send and any future concurrent loader can't clobber each other (project StateFlow rule).
 */
@HiltViewModel
class AgentChatViewModel @Inject constructor(
    private val repo: AgentRepository,
    private val store: AgentConversationStore,
) : ViewModel() {

    // Seed from the shared store so history survives leaving and re-entering the chat screen.
    private val _state = MutableStateFlow(
        AgentChatUiState(messages = store.snapshot(), provider = store.lastProvider),
    )
    val state: StateFlow<AgentChatUiState> = _state.asStateFlow()

    fun send(text: String) {
        val msg = text.trim()
        if (msg.isEmpty() || _state.value.sending) return
        _state.update { it.copy(messages = it.messages + ChatMessage(ChatRole.USER, msg), sending = true) }
        persist()
        viewModelScope.launch {
            val result = repo.chat(msg)
            _state.update { s ->
                result.fold(
                    onSuccess = { r ->
                        s.copy(
                            messages = s.messages + ChatMessage(ChatRole.ASSISTANT, r.reply),
                            sending = false,
                            provider = r.provider,
                        )
                    },
                    onFailure = { e ->
                        s.copy(
                            messages = s.messages + ChatMessage(ChatRole.ASSISTANT, e.message ?: "Something went wrong."),
                            sending = false,
                        )
                    },
                )
            }
            persist()
        }
    }

    /** Mirror the current messages into the process-lived store so a recreated VM restores them. */
    private fun persist() = store.replace(_state.value.messages, _state.value.provider)
}
