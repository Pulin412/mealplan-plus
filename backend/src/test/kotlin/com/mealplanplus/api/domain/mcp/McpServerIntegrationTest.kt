package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.admin.AdminController
import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.featureflag.FeatureFlagKey
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.meal.MealService
import com.mealplanplus.api.domain.plan.DayPlanService
import com.mealplanplus.api.generated.model.DayPlanDto
import com.mealplanplus.api.generated.model.DietDto
import com.mealplanplus.api.generated.model.DietMealDto
import com.mealplanplus.api.generated.model.FoodDto
import com.mealplanplus.api.generated.model.FoodUnit
import com.mealplanplus.api.generated.model.MealDto
import com.mealplanplus.api.generated.model.MealFoodItemDto
import com.mealplanplus.api.generated.model.PlannedMealDto
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.McpSyncClient
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.TestPropertySource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * End-to-end proof of the MCP server: a real MCP client connects over SSE to the embedded server,
 * authenticates with a bearer connector token (header on every request), and exercises the tools —
 * covering the flag gate, McpAuthFilter, uid resolution, read/write scope, and the write guardrails.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = [
    "firebase.project-id=test-project",
    "mcp.token-secret=integration-test-secret",
    "app.admin-emails=admin@test",
])
class McpServerIntegrationTest {

    @LocalServerPort var port: Int = 0
    @Autowired lateinit var tokens: McpTokenService
    @Autowired lateinit var flags: FeatureFlagService
    @Autowired lateinit var foodService: FoodService
    @Autowired lateinit var mealService: MealService
    @Autowired lateinit var dietService: DietService
    @Autowired lateinit var dayPlanService: DayPlanService
    @Autowired lateinit var adminController: AdminController

    private val uid = "uid-mcp-test"

    private fun connect(token: String): McpSyncClient {
        val transport = HttpClientStreamableHttpTransport.builder("http://localhost:$port")
            .endpoint("/mcp")
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
    fun `a token minted through the admin endpoint drives the MCP tools`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")

        // Mint through the real admin endpoint as an allow-listed admin (uid = the token's subject).
        val auth = UsernamePasswordAuthenticationToken(uid, null, emptyList()).apply { details = "admin@test" }
        SecurityContextHolder.getContext().authentication = auth
        val minted = try {
            adminController.mintMcpConnectorToken("READ_WRITE").body!!
        } finally {
            SecurityContextHolder.clearContext()
        }
        assertThat(minted.sseEndpointPath).isEqualTo("/mcp")

        // The minted token authenticates a real MCP client end-to-end.
        connect(minted.token).use { client ->
            client.initialize()
            assertThat(client.callTool(McpSchema.CallToolRequest("listDiets", emptyMap<String, Any>())).text())
                .contains("no diets")
        }
    }

    @Test
    fun `an unauthenticated mcp request gets a 401 pointing at the resource metadata`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val resp = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port/mcp")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertThat(resp.statusCode()).isEqualTo(401)
        assertThat(resp.headers().firstValue("WWW-Authenticate").orElse(""))
            .contains("resource_metadata").contains("/.well-known/oauth-protected-resource")
    }

    @Test
    fun `protected resource metadata is publicly served`() {
        val resp = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port/.well-known/oauth-protected-resource")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertThat(resp.statusCode()).isEqualTo(200)
        assertThat(resp.body()).contains("\"resource\"").contains("/mcp").contains("scopes_supported")
    }

    @Test
    fun `a read-only token is refused write operations`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val readOnly = tokens.mint(uid, McpTokenService.Scope.READ)

        connect(readOnly).use { client ->
            client.initialize()
            val args = mapOf("foodId" to 1L, "quantity" to 100.0, "unit" to "GRAM", "slot" to "BREAKFAST", "date" to "2026-08-18")
            assertThat(client.callTool(McpSchema.CallToolRequest("logFood", args)).text()).contains("read-only")

            // The added writes are gated by the same scope check.
            assertThat(client.callTool(McpSchema.CallToolRequest("removeLoggedFood", mapOf("loggedFoodId" to 1L))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("logHealthMetric", mapOf("type" to "WEIGHT", "value" to 70.0))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("createGroceryFromDiet", mapOf("dietId" to 1L))).text())
                .contains("read-only")
        }
    }

    @Test
    fun `a read-write agent can use the extended read and write tools`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")

        // Distinct uid so seeded diets/meals/plans don't leak into the other tests' "fresh uid" assumptions.
        val extUid = "uid-mcp-ext"
        val date = "2026-08-25"
        val food = foodService.create(
            FoodDto(name = "Ext Apple", caloriesPer100 = 52.0, proteinPer100 = 0.3, carbsPer100 = 14.0, fatPer100 = 0.2),
            extUid,
        )
        val meal = mealService.create(
            MealDto(name = "Ext Meal", items = listOf(MealFoodItemDto(foodId = food.id!!, quantity = 100.0, unit = FoodUnit.GRAM))),
            extUid,
        )
        val diet = dietService.create(
            DietDto(
                name = "Ext Diet", targetCalories = 2000.0, targetProtein = 150.0, targetCarbs = 200.0, targetFat = 60.0,
                meals = listOf(DietMealDto(mealId = meal.id!!, dayOfWeek = 0, slot = "BREAKFAST")),
            ),
            extUid,
        )
        dayPlanService.upsert(
            extUid, java.time.LocalDate.parse(date),
            DayPlanDto(date = java.time.LocalDate.parse(date), dietId = diet.id, plannedMeals = listOf(PlannedMealDto(mealId = meal.id!!, slot = "LUNCH"))),
        )
        val token = tokens.mint(extUid, McpTokenService.Scope.READ_WRITE)

        connect(token).use { client ->
            client.initialize()

            // The new tools are registered on the MCP surface.
            assertThat(client.listTools().tools().map { it.name() }).contains(
                "getLoggedFoods", "listMeals", "getDietDetails", "listHealthMetrics", "getDayPlan",
                "listWorkoutTemplates", "listWorkoutSessions", "removeLoggedFood", "logHealthMetric",
                "createGroceryFromDiet",
            )

            // getLoggedFoods: empty for today, then reflects a log, then empty again after removal.
            assertThat(client.callTool(McpSchema.CallToolRequest("getLoggedFoods", emptyMap<String, Any>())).text())
                .contains("Nothing logged")
            client.callTool(McpSchema.CallToolRequest("logFood",
                mapOf("foodId" to food.id, "quantity" to 100.0, "unit" to "GRAM", "slot" to "BREAKFAST", "date" to date)))
            val logged = client.callTool(McpSchema.CallToolRequest("getLoggedFoods", mapOf("date" to date))).text()
            assertThat(logged).contains("Ext Apple").contains("BREAKFAST")
            val loggedId = Regex("id=(\\d+)").find(logged)!!.groupValues[1].toLong()

            // removeLoggedFood undoes it.
            assertThat(client.callTool(McpSchema.CallToolRequest("removeLoggedFood", mapOf("loggedFoodId" to loggedId))).text())
                .contains("Removed")
            assertThat(client.callTool(McpSchema.CallToolRequest("getLoggedFoods", mapOf("date" to date))).text())
                .contains("Nothing logged")

            // listMeals / getDietDetails surface the seeded meal and diet.
            assertThat(client.callTool(McpSchema.CallToolRequest("listMeals", emptyMap<String, Any>())).text())
                .contains("Ext Meal")
            assertThat(client.callTool(McpSchema.CallToolRequest("getDietDetails", mapOf("dietId" to diet.id))).text())
                .contains("Ext Diet").contains("Ext Meal").contains("BREAKFAST")

            // getDayPlan shows the assigned diet (with targets) and the planned meal.
            val dayPlan = client.callTool(McpSchema.CallToolRequest("getDayPlan", mapOf("date" to date))).text()
            assertThat(dayPlan).contains("Diet: Ext Diet").contains("2000 kcal").contains("LUNCH: Ext Meal")

            // createGroceryFromDiet aggregates the diet's foods into a list.
            assertThat(client.callTool(McpSchema.CallToolRequest("createGroceryFromDiet", mapOf("dietId" to diet.id))).text())
                .contains("Ext Diet — Shopping List").contains("Ext Apple")

            // logHealthMetric records a reading (unit defaults to kg for WEIGHT) that listHealthMetrics reads back.
            assertThat(client.callTool(McpSchema.CallToolRequest("logHealthMetric", mapOf("type" to "WEIGHT", "value" to 72.5))).text())
                .contains("Recorded WEIGHT = 72.5 kg")
            assertThat(client.callTool(McpSchema.CallToolRequest("listHealthMetrics", mapOf("type" to "WEIGHT"))).text())
                .contains("72.5").contains("kg")

            // Workout reads degrade gracefully when the user has none.
            assertThat(client.callTool(McpSchema.CallToolRequest("listWorkoutTemplates", emptyMap<String, Any>())).text())
                .contains("no workout templates")
            assertThat(client.callTool(McpSchema.CallToolRequest("listWorkoutSessions", emptyMap<String, Any>())).text())
                .contains("No workout sessions")
        }
    }
}
