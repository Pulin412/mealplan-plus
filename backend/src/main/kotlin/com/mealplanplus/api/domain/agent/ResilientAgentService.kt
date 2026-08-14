package com.mealplanplus.api.domain.agent

import org.slf4j.LoggerFactory
import org.springframework.ai.anthropic.AnthropicChatModel
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.openai.OpenAiChatModel
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.web.client.HttpStatusCodeException
import java.time.LocalDate

/**
 * Runs the assistant against the configured provider [chain][AgentProperties], failing over transparently:
 * on a rate-limit / quota / transient error it moves to the next provider; an auth error skips that
 * provider; if all are exhausted it returns a friendly message. UX is unaffected by which provider serves.
 */
@Service
class ResilientAgentService(
    private val props: AgentProperties,
    private val tools: MealPlanToolService,
    // The Anthropic model bean is autoconfigured even without a key, so we can't rely on its presence
    // alone — we also read the key to decide readiness.
    @Autowired(required = false) private val anthropicModel: AnthropicChatModel?,
    @Value("\${spring.ai.anthropic.api-key:}") private val anthropicApiKey: String = "",
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * The model-agnostic operating manual, loaded once from the classpath. Keeping it in a resource
     * file (not a code literal) makes it the single source of truth for behaviour as we swap models
     * through the failover chain — the routing/rules stay identical whichever provider serves.
     */
    private val playbookTemplate: String =
        ClassPathResource("agent/playbook.md").inputStream.bufferedReader().use { it.readText() }

    fun chat(request: AgentChatRequest): AgentChatResponse {
        val today = request.date ?: LocalDate.now().toString()
        val system = systemPrompt(today, request.slot)

        val chain = props.providers.filter { it.enabled && isReady(it) }
        if (chain.isEmpty()) {
            return AgentChatResponse(reply = "No AI provider is configured. Add an API key for one of the " +
                "providers (e.g. GROQ_API_KEY) and try again.")
        }

        for (provider in chain) {
            val model = runCatching { buildModel(provider) }.getOrElse { e ->
                log.warn("agent: could not build provider '{}' ({}); skipping", provider.name, e.message); null
            } ?: continue
            try {
                val reply = ChatClient.builder(model).build()
                    .prompt()
                    .system(system)
                    .user(request.message)
                    .tools(tools)
                    .call()
                    .content() ?: "Sorry, I couldn't produce a response."
                return AgentChatResponse(reply = reply, provider = provider.name)
            } catch (e: Exception) {
                val status = httpStatusOf(e)
                if (status == 401 || status == 403) {
                    log.warn("agent: provider '{}' auth failed (HTTP {}); skipping", provider.name, status)
                } else {
                    // 429 / quota / 5xx / timeout / unknown → fail over to the next provider.
                    log.warn("agent: provider '{}' failed ({}); failing over", provider.name, status ?: e.message)
                }
            }
        }
        return AgentChatResponse(reply = "The assistant is temporarily unavailable — all providers were " +
            "exhausted or rate-limited. Please try again in a bit.")
    }

    /** Read-only view of the chain for a settings screen (never exposes keys). */
    fun listProviders(): List<ProviderStatus> = props.providers.map {
        ProviderStatus(name = it.name, type = it.type, model = it.model, enabled = it.enabled, ready = isReady(it))
    }

    private fun isReady(p: ProviderConfig): Boolean =
        if (p.isOpenAi) p.hasKey else anthropicModel != null && anthropicApiKey.isNotBlank()

    private fun buildModel(p: ProviderConfig): ChatModel {
        if (!p.isOpenAi) return anthropicModel ?: error("anthropic model not available")
        val api = OpenAiApi.builder()
            .baseUrl(p.baseUrl ?: "https://api.openai.com")
            .apiKey(p.apiKey ?: "")
            .completionsPath(p.completionsPath ?: "/v1/chat/completions")
            .build()
        return OpenAiChatModel.builder()
            .openAiApi(api)
            .defaultOptions(OpenAiChatOptions.builder().model(p.model).build())
            .build()
    }

    /** Walk the cause chain for an HTTP status (Spring's RestClient throws HttpStatusCodeException). */
    private fun httpStatusOf(e: Throwable?): Int? {
        var cur = e
        var depth = 0
        while (cur != null && depth < 6) {
            if (cur is HttpStatusCodeException) return cur.statusCode.value()
            cur = cur.cause; depth++
        }
        // Fallback: some providers surface the code only in the message.
        val msg = e?.message ?: return null
        return when {
            msg.contains("429") || msg.contains("rate limit", true) || msg.contains("quota", true) -> 429
            // Anthropic surfaces a bad/missing key as a retry-exhausted "authentication" failure rather
            // than a clean 401 status — classify it as auth so we skip the provider instead of hammering it.
            msg.contains("401") || msg.contains("authentication", true) || msg.contains("unauthorized", true) -> 401
            msg.contains("403") -> 403
            else -> null
        }
    }

    private fun systemPrompt(today: String, slot: String?): String {
        val slotHint = slot?.let { "The user is likely eating $it right now. " } ?: ""
        return playbookTemplate
            .replace("{{TODAY}}", today)
            .replace("{{SLOT_HINT}}", slotHint)
    }
}

/** Safe provider summary for the settings/status endpoint (no secrets). */
data class ProviderStatus(
    val name: String,
    val type: String,
    val model: String,
    val enabled: Boolean,
    val ready: Boolean,
)
