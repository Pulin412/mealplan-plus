package com.mealplanplus.api.domain.agent

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * The ordered AI provider failover chain (bound from `agent.providers` in application.yml).
 * The assistant tries providers in list order; a provider is usable only when [ProviderConfig.enabled]
 * and its credentials are present (see [ProviderConfig.type]).
 */
@ConfigurationProperties(prefix = "agent")
data class AgentProperties(
    val providers: List<ProviderConfig> = emptyList(),
)

data class ProviderConfig(
    /** Display id, e.g. "groq". */
    val name: String = "",
    /** "openai" (OpenAI-compatible: Groq/Gemini/Mistral/OpenRouter) or "anthropic" (dedicated client). */
    val type: String = "openai",
    /** OpenAI-compatible host root (no trailing /chat/completions). Ignored for anthropic. */
    val baseUrl: String? = null,
    /** Path appended to baseUrl; default matches most OpenAI-compatible hosts. */
    val completionsPath: String? = null,
    /** Model id to request from this provider. */
    val model: String = "",
    /** API key (from env). Blank ⇒ provider is skipped. Ignored for anthropic (uses ANTHROPIC_API_KEY). */
    val apiKey: String? = null,
    val enabled: Boolean = true,
) {
    /** OpenAI-compatible providers need a non-blank key; anthropic readiness is checked at runtime. */
    val isOpenAi: Boolean get() = type.equals("openai", ignoreCase = true)
    val hasKey: Boolean get() = !apiKey.isNullOrBlank()
}
