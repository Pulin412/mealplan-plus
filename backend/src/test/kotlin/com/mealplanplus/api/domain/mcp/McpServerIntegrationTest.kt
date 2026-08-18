package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.featureflag.FeatureFlagKey
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.spec.McpSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.TestPropertySource

/**
 * End-to-end proof of the MCP server: a real MCP client connects over SSE to the embedded server,
 * authenticates with a bearer connector token (header on every request), lists the tools, and calls
 * one — exercising the flag gate, McpAuthFilter, uid resolution, and the tool round-trip.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = ["firebase.project-id=test-project", "mcp.token-secret=integration-test-secret"])
class McpServerIntegrationTest {

    @LocalServerPort var port: Int = 0
    @Autowired lateinit var tokens: McpTokenService
    @Autowired lateinit var flags: FeatureFlagService

    private fun connect(token: String): McpSyncClient {
        val transport = HttpClientSseClientTransport.builder("http://localhost:$port")
            .sseEndpoint("/mcp/sse")
            .customizeRequest { it.header("Authorization", "Bearer $token") }
            .build()
        return McpClient.sync(transport).build()
    }

    @Test
    fun `an authenticated agent can list and call the listDiets tool`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val token = tokens.mint("uid-mcp-test", McpTokenService.Scope.READ_WRITE)

        connect(token).use { client ->
            client.initialize()

            val toolNames = client.listTools().tools().map { it.name() }
            assertThat(toolNames).contains("listDiets")

            val result = client.callTool(McpSchema.CallToolRequest("listDiets", emptyMap<String, Any>()))
            val text = (result.content().first() as McpSchema.TextContent).text()
            // A fresh uid owns no diets — proves auth → uid → tool execution wired end to end.
            assertThat(text).contains("no diets")
        }
    }
}
