package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.diet.DietService
import org.springframework.ai.tool.annotation.Tool
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

/**
 * Tools exposed to a user's OWN AI agent (e.g. Claude) over the MCP server. Deliberately isolated in
 * `domain/mcp` and calling core services directly — nothing else depends on this package, so the whole
 * MCP surface can be removed by deleting `domain/mcp`, its config, and the `mcp_server` flag gate.
 *
 * `uid` is resolved from the SecurityContext, which [McpAuthFilter] populates from the connector token —
 * every tool is therefore scoped to the calling user, same as the rest of the API.
 */
@Service
class McpToolService(
    private val dietService: DietService,
) {
    private val uid: String
        get() = SecurityContextHolder.getContext().authentication?.name ?: ""

    @Tool(description = """
        List the current user's diets (their day-plan templates). Returns each diet's id, name,
        and whether it is marked a favorite. Use this to see what diets exist before referencing one.
    """)
    fun listDiets(): String {
        if (uid.isBlank()) return "Not authenticated."
        val diets = dietService.list(uid)
        if (diets.isEmpty()) return "You have no diets yet."
        return diets.joinToString("\n") { d -> "id=${d.id} | ${d.name}${if (d.isFavorite == true) " (favorite)" else ""}" }
    }
}
