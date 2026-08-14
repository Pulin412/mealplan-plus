package com.mealplanplus.api.domain.agent

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Binds the `agent.providers` failover chain. Provider ChatModels are built per-request inside
 * [ResilientAgentService] (one OpenAI-compatible client per provider), so there is no single
 * auto-configured ChatClient bean anymore — the chain replaced the old AGENT_PROVIDER switch.
 */
@Configuration
@EnableConfigurationProperties(AgentProperties::class)
class AgentConfig
