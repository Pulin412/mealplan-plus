package com.mealplanplus.api.domain.agent

data class AgentChatRequest(
    val message: String,
    val date: String? = null,   // YYYY-MM-DD; defaults to today on the backend
    val slot: String? = null    // optional hint: BREAKFAST / LUNCH / DINNER / etc.
)

data class ToolAction(
    val tool: String,
    val result: String
)

data class AgentChatResponse(
    val reply: String,
    val actionsPerformed: List<ToolAction> = emptyList()
)
