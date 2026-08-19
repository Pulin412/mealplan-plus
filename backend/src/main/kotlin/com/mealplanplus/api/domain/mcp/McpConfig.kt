package com.mealplanplus.api.domain.mcp

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Registers our MCP tools with Spring AI's MCP server. The server auto-config picks up this
 * [ToolCallbackProvider] bean and exposes exactly these tools (there is no other provider bean, so
 * nothing from the in-app agent leaks onto the MCP surface).
 */
@Configuration
class McpConfig {

    @Bean
    fun mcpToolCallbackProvider(mcpToolService: McpToolService): ToolCallbackProvider =
        MethodToolCallbackProvider.builder()
            .toolObjects(mcpToolService)
            .build()
}
