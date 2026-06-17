package com.mealplanplus.api.domain.agent

import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.ollama.OllamaChatModel
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AgentConfig {

    // Overrides Spring AI's auto-configured ChatClient.Builder so we can
    // pick the right model at runtime via agent.provider (env: AGENT_PROVIDER).
    // Local dev → ollama (no key needed), prod → anthropic.
    @Bean
    fun chatClientBuilder(
        @Value("\${agent.provider:ollama}") provider: String,
        @Autowired(required = false) ollamaModel: OllamaChatModel?,
        @Autowired(required = false) anthropicModel: AnthropicChatModel?
    ): ChatClient.Builder {
        val model: ChatModel = when (provider) {
            "anthropic" -> anthropicModel
                ?: error("Anthropic model not available — set ANTHROPIC_API_KEY and AGENT_PROVIDER=anthropic")
            else -> ollamaModel
                ?: error("Ollama not available — run: ollama serve")
        }
        return ChatClient.builder(model)
    }
}
