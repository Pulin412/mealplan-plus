package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.featureflag.FeatureFlagKey
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.generated.model.FoodDto
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
 * authenticates with a bearer connector token (header on every request), and exercises the tools —
 * covering the flag gate, McpAuthFilter, uid resolution, read/write scope, and the write guardrails.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = ["firebase.project-id=test-project", "mcp.token-secret=integration-test-secret"])
class McpServerIntegrationTest {

    @LocalServerPort var port: Int = 0
    @Autowired lateinit var tokens: McpTokenService
    @Autowired lateinit var flags: FeatureFlagService
    @Autowired lateinit var foodService: FoodService

    private val uid = "uid-mcp-test"

    private fun connect(token: String): McpSyncClient {
        val transport = HttpClientSseClientTransport.builder("http://localhost:$port")
            .sseEndpoint("/mcp/sse")
            .customizeRequest { it.header("Authorization", "Bearer $token") }
            .build()
        return McpClient.sync(transport).build()
    }

    private fun McpSchema.CallToolResult.text() = (content().first() as McpSchema.TextContent).text()

    @Test
    fun `a read-write agent can list, read, and write through the MCP tools`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val food = foodService.create(
            FoodDto(name = "Test Apple", caloriesPer100 = 52.0, proteinPer100 = 0.3, carbsPer100 = 14.0, fatPer100 = 0.2),
            uid,
        )
        val token = tokens.mint(uid, McpTokenService.Scope.READ_WRITE)

        connect(token).use { client ->
            client.initialize()

            assertThat(client.listTools().tools().map { it.name() })
                .contains("listDiets", "todayDashboard", "getProfile", "searchFoods", "logFood", "createMeal")

            // Read: a fresh uid owns no diets — proves auth → uid → tool execution.
            assertThat(client.callTool(McpSchema.CallToolRequest("listDiets", emptyMap<String, Any>())).text())
                .contains("no diets")

            // Write: log the food, then logging the identical entry again is idempotent.
            val logArgs = mapOf("foodId" to food.id, "quantity" to 100.0, "unit" to "GRAM", "slot" to "BREAKFAST", "date" to "2026-08-18")
            assertThat(client.callTool(McpSchema.CallToolRequest("logFood", logArgs)).text()).contains("Logged Test Apple")
            assertThat(client.callTool(McpSchema.CallToolRequest("logFood", logArgs)).text()).contains("skipped duplicate")

            // Write: create a meal from the food.
            val mealArgs = mapOf("name" to "Test Meal", "foods" to listOf(mapOf("foodId" to food.id, "quantity" to 100.0, "unit" to "GRAM")))
            assertThat(client.callTool(McpSchema.CallToolRequest("createMeal", mealArgs)).text()).contains("Created meal 'Test Meal'")
        }
    }

    @Test
    fun `a read-only token is refused write operations`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val readOnly = tokens.mint(uid, McpTokenService.Scope.READ)

        connect(readOnly).use { client ->
            client.initialize()
            val args = mapOf("foodId" to 1L, "quantity" to 100.0, "unit" to "GRAM", "slot" to "BREAKFAST", "date" to "2026-08-18")
            assertThat(client.callTool(McpSchema.CallToolRequest("logFood", args)).text()).contains("read-only")
        }
    }
}
