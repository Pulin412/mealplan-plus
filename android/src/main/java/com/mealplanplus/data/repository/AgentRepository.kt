package com.mealplanplus.data.repository

import com.mealplanplus.data.generated.api.AssistantApi
import com.mealplanplus.data.generated.model.AgentChatRequest
import com.mealplanplus.data.generated.model.AgentChatResponse
import com.mealplanplus.data.generated.model.ProviderStatus
import com.mealplanplus.data.remote.ApiErrors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backend-mediated AI assistant. Thin wrapper over the generated [AssistantApi]: the backend owns the
 * provider failover chain and the tool-calling, so the client just sends a message and renders the reply.
 * Errors are surfaced as user-safe strings via [ApiErrors] (never raw HTTP detail).
 */
@Singleton
class AgentRepository @Inject constructor(
    private val api: AssistantApi,
) {
    /** Send one message; [AgentChatResponse.reply] is ready to render, [AgentChatResponse.provider] names who served it. */
    suspend fun chat(message: String): Result<AgentChatResponse> = runCatching {
        val resp = api.agentChat(AgentChatRequest(message = message))
        if (!resp.isSuccessful) error(ApiErrors.messageFor(resp))
        resp.body() ?: error("Empty response from assistant.")
    }

    /** Read-only status of the provider failover chain (no keys). */
    suspend fun providers(): Result<List<ProviderStatus>> = runCatching {
        val resp = api.getAgentProviders()
        if (!resp.isSuccessful) error(ApiErrors.messageFor(resp))
        resp.body() ?: emptyList()
    }
}
