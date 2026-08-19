package com.mealplanplus.api.domain.mcp

import io.micrometer.context.ContextRegistry
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import reactor.core.publisher.Hooks

/**
 * Bridges the thread-local SecurityContext across Reactor's scheduler boundary. Spring AI's MCP server
 * executes tool calls off the servlet thread (e.g. `boundedElastic`), so the uid that [McpAuthFilter]
 * authenticates on the request thread would otherwise be lost — tools would run unauthenticated.
 *
 * Registering a ThreadLocalAccessor for the SecurityContext + enabling automatic context propagation
 * makes Reactor snapshot the context at subscription (on the authenticated request thread) and restore
 * it around tool execution. NB: `enableAutomaticContextPropagation` is a global Reactor hook.
 */
@Configuration
class McpContextPropagationConfig {

    @PostConstruct
    fun enable() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
            "mcp.securityContext",
            { SecurityContextHolder.getContext() },
            { ctx: SecurityContext -> SecurityContextHolder.setContext(ctx) },
            { SecurityContextHolder.clearContext() },
        )
        Hooks.enableAutomaticContextPropagation()
    }
}
