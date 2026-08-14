package com.mealplanplus.api.domain.agent

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/agent")
class AgentController(
    private val agent: ResilientAgentService
) {

    @PostMapping("/chat")
    fun chat(@RequestBody request: AgentChatRequest): AgentChatResponse = agent.chat(request)

    /** Read-only view of the provider failover chain (no keys). Settings screen consumes this. */
    @GetMapping("/providers")
    fun providers(): List<ProviderStatus> = agent.listProviders()
}
