package com.mealplanplus.api.domain.mcp

import com.mealplanplus.api.domain.admin.AdminController
import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.featureflag.FeatureFlagKey
import com.mealplanplus.api.domain.featureflag.FeatureFlagService
import com.mealplanplus.api.domain.food.Food
import com.mealplanplus.api.domain.food.FoodRepository
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
    @Autowired lateinit var foodRepo: FoodRepository
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
    fun `food tools - searchFoods exposes units, createFood dedupes, deleteFood protects system foods`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val fUid = "uid-mcp-food"
        // A shared system food with a count unit — protected from deletion, and its unit must surface in search.
        val paneer = foodRepo.save(
            Food(
                name = "ZParq Paneer", caloriesPer100 = 265.0, proteinPer100 = 18.0, carbsPer100 = 1.0, fatPer100 = 20.0,
                unit = "PIECE", gramsPerPiece = 50.0, isSystemFood = true,
            ),
        )
        val token = tokens.mint(fUid, McpTokenService.Scope.READ_WRITE)

        connect(token).use { client ->
            client.initialize()
            assertThat(client.listTools().tools().map { it.name() }).contains("createFood", "deleteFood")

            // searchFoods surfaces the natural unit + grams-per-piece (the paneer fix).
            val search = client.callTool(McpSchema.CallToolRequest("searchFoods", mapOf("query" to "ZParq Paneer"))).text()
            assertThat(search).contains("ZParq Paneer").contains("unit=PIECE").contains("≈ 50g")

            // createFood makes a user-owned food and returns its id.
            val createArgs = mapOf("name" to "ZParq Skyr", "caloriesPer100" to 63.0, "proteinPer100" to 11.0, "carbsPer100" to 4.0, "fatPer100" to 0.2)
            val created = client.callTool(McpSchema.CallToolRequest("createFood", createArgs)).text()
            assertThat(created).contains("Created food 'ZParq Skyr'")
            val newId = Regex("id=(\\d+)").find(created)!!.groupValues[1].toLong()

            // A second identical createFood reuses instead of duplicating.
            assertThat(client.callTool(McpSchema.CallToolRequest("createFood", createArgs)).text()).contains("already exists")

            // deleteFood removes the user's own food…
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteFood", mapOf("foodId" to newId))).text())
                .contains("Deleted food 'ZParq Skyr'")

            // …but refuses a shared system food.
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteFood", mapOf("foodId" to paneer.id))).text())
                .contains("shared system food")
        }
    }

    @Test
    fun `meal tools - searchMeals finds by name, createMeal dedupes, deleteMeal removes own meals`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val mUid = "uid-mcp-meal"
        val food = foodService.create(
            FoodDto(name = "ZQ Rice", caloriesPer100 = 130.0, proteinPer100 = 2.7, carbsPer100 = 28.0, fatPer100 = 0.3),
            mUid,
        )
        val token = tokens.mint(mUid, McpTokenService.Scope.READ_WRITE)

        connect(token).use { client ->
            client.initialize()
            assertThat(client.listTools().tools().map { it.name() }).contains("searchMeals", "deleteMeal")

            val create = client.callTool(McpSchema.CallToolRequest("createMeal",
                mapOf("name" to "ZQ Paneer Bowl", "foods" to listOf(mapOf("foodId" to food.id, "quantity" to 150.0, "unit" to "GRAM"))))).text()
            assertThat(create).contains("Created meal 'ZQ Paneer Bowl'")
            val mealId = Regex("id=(\\d+)").find(create)!!.groupValues[1].toLong()

            // Same name → reused, not duplicated.
            assertThat(client.callTool(McpSchema.CallToolRequest("createMeal",
                mapOf("name" to "ZQ Paneer Bowl", "foods" to emptyList<Any>()))).text()).contains("already exists")

            // searchMeals matches by substring.
            assertThat(client.callTool(McpSchema.CallToolRequest("searchMeals", mapOf("query" to "paneer bowl"))).text())
                .contains("ZQ Paneer Bowl").contains("food(s)")

            // A slot-tagged meal is filtered by slot (case-insensitive), showing its slot tags.
            mealService.create(
                MealDto(
                    name = "ZQ Morning Oats",
                    items = listOf(MealFoodItemDto(foodId = food.id!!, quantity = 50.0, unit = FoodUnit.GRAM)),
                    slots = listOf("Breakfast"),
                ),
                mUid,
            )
            assertThat(client.callTool(McpSchema.CallToolRequest("searchMeals", mapOf("slot" to "BREAKFAST"))).text())
                .contains("ZQ Morning Oats").contains("slots: Breakfast")
            // The un-tagged Paneer Bowl is not returned for a slot filter, and Dinner matches nothing.
            assertThat(client.callTool(McpSchema.CallToolRequest("searchMeals", mapOf("slot" to "DINNER"))).text())
                .contains("No meals found")
            // Name + slot together still work.
            assertThat(client.callTool(McpSchema.CallToolRequest("searchMeals", mapOf("query" to "oats", "slot" to "breakfast"))).text())
                .contains("ZQ Morning Oats")

            // deleteMeal removes it, and it's then gone.
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteMeal", mapOf("mealId" to mealId))).text())
                .contains("Deleted meal 'ZQ Paneer Bowl'")
            assertThat(client.callTool(McpSchema.CallToolRequest("searchMeals", mapOf("query" to "ZQ Paneer Bowl"))).text())
                .contains("No meals found")
        }
    }

    @Test
    fun `diet tools - createDiet builds by slot, searchDiets finds, deleteDiet removes`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val dUid = "uid-mcp-diet"
        val food = foodService.create(
            FoodDto(name = "ZD Egg", caloriesPer100 = 143.0, proteinPer100 = 13.0, carbsPer100 = 1.0, fatPer100 = 10.0), dUid)
        val meal = mealService.create(
            MealDto(name = "ZD Oats", items = listOf(MealFoodItemDto(foodId = food.id!!, quantity = 60.0, unit = FoodUnit.GRAM))), dUid)
        val token = tokens.mint(dUid, McpTokenService.Scope.READ_WRITE)

        connect(token).use { client ->
            client.initialize()
            assertThat(client.listTools().tools().map { it.name() }).contains("createDiet", "searchDiets", "deleteDiet")

            // A meal in Breakfast + a loose food in Lunch; slots given in mixed case, resolved to canonical.
            val entries = listOf(
                mapOf("slot" to "breakfast", "mealId" to meal.id),
                mapOf("slot" to "Lunch", "foodId" to food.id, "quantity" to 100.0, "unit" to "GRAM"),
            )
            val created = client.callTool(McpSchema.CallToolRequest("createDiet",
                mapOf("name" to "ZD Cutting Day", "entries" to entries, "targetCalories" to 1800.0))).text()
            assertThat(created).contains("Created diet 'ZD Cutting Day'").contains("1 meal(s)").contains("1 food(s)")
            val dietId = Regex("id=(\\d+)").find(created)!!.groupValues[1].toLong()

            // getDietDetails reflects the canonical slots + the meal.
            val details = client.callTool(McpSchema.CallToolRequest("getDietDetails", mapOf("dietId" to dietId))).text()
            assertThat(details).contains("ZD Cutting Day").contains("Breakfast").contains("ZD Oats")

            // Same name → reused; an unknown slot is rejected with the valid list.
            assertThat(client.callTool(McpSchema.CallToolRequest("createDiet",
                mapOf("name" to "ZD Cutting Day", "entries" to emptyList<Any>()))).text()).contains("already exists")
            assertThat(client.callTool(McpSchema.CallToolRequest("createDiet",
                mapOf("name" to "ZD Bad Slot", "entries" to listOf(mapOf("slot" to "Brunch", "mealId" to meal.id))))).text())
                .contains("Invalid slot")

            // searchDiets finds it, then deleteDiet removes it.
            assertThat(client.callTool(McpSchema.CallToolRequest("searchDiets", mapOf("query" to "cutting"))).text())
                .contains("ZD Cutting Day")
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteDiet", mapOf("dietId" to dietId))).text())
                .contains("Deleted diet 'ZD Cutting Day'")
            assertThat(client.callTool(McpSchema.CallToolRequest("searchDiets", mapOf("query" to "cutting"))).text())
                .contains("No diets found")
        }
    }

    @Test
    fun `exercise and workout tools - create, search by name and tag, delete`() {
        flags.setEnabled(FeatureFlagKey.MCP_SERVER.key, enabled = true, updatedBy = "test")
        val wUid = "uid-mcp-workout"
        val token = tokens.mint(wUid, McpTokenService.Scope.READ_WRITE)

        connect(token).use { client ->
            client.initialize()
            assertThat(client.listTools().tools().map { it.name() }).contains(
                "listExercises", "searchExercises", "createExercise", "deleteExercise",
                "searchWorkouts", "createWorkout", "deleteWorkout",
            )

            // createExercise with a type + a tag (tag created on the fly).
            val createEx = client.callTool(McpSchema.CallToolRequest("createExercise",
                mapOf("name" to "ZW Bench Press", "type" to "strength", "tags" to listOf("Chest")))).text()
            assertThat(createEx).contains("Created exercise 'ZW Bench Press'").contains("STRENGTH")
            val exId = Regex("id=(\\d+)").find(createEx)!!.groupValues[1].toLong()

            // Same name → reused; an unknown type is rejected.
            assertThat(client.callTool(McpSchema.CallToolRequest("createExercise",
                mapOf("name" to "ZW Bench Press", "type" to "STRENGTH"))).text()).contains("already exists")
            assertThat(client.callTool(McpSchema.CallToolRequest("createExercise",
                mapOf("name" to "ZW Bad", "type" to "YOGA"))).text()).contains("Invalid type")

            // searchExercises by name and by tag.
            assertThat(client.callTool(McpSchema.CallToolRequest("searchExercises", mapOf("query" to "bench"))).text())
                .contains("ZW Bench Press").contains("tags: Chest")
            assertThat(client.callTool(McpSchema.CallToolRequest("searchExercises", mapOf("tag" to "chest"))).text())
                .contains("ZW Bench Press")
            assertThat(client.callTool(McpSchema.CallToolRequest("searchExercises", mapOf("tag" to "legs"))).text())
                .contains("No exercises found")

            // createWorkout from the exercise, with a tag.
            val createW = client.callTool(McpSchema.CallToolRequest("createWorkout",
                mapOf("name" to "ZW Push Day", "exerciseIds" to listOf(exId), "tags" to listOf("Upper")))).text()
            assertThat(createW).contains("Created workout 'ZW Push Day'").contains("1 exercise(s)")
            val wId = Regex("id=(\\d+)").find(createW)!!.groupValues[1].toLong()

            assertThat(client.callTool(McpSchema.CallToolRequest("searchWorkouts", mapOf("query" to "push"))).text())
                .contains("ZW Push Day")
            assertThat(client.callTool(McpSchema.CallToolRequest("searchWorkouts", mapOf("tag" to "upper"))).text())
                .contains("ZW Push Day")

            // Delete the workout first (it references the exercise), then the exercise.
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteWorkout", mapOf("workoutId" to wId))).text())
                .contains("Deleted workout 'ZW Push Day'")
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteExercise", mapOf("exerciseId" to exId))).text())
                .contains("Deleted exercise 'ZW Bench Press'")
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
            assertThat(client.callTool(McpSchema.CallToolRequest("createFood",
                mapOf("name" to "X", "caloriesPer100" to 1.0, "proteinPer100" to 0.0, "carbsPer100" to 0.0, "fatPer100" to 0.0))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteFood", mapOf("foodId" to 1L))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteMeal", mapOf("mealId" to 1L))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("createDiet",
                mapOf("name" to "X", "entries" to emptyList<Any>()))).text()).contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteDiet", mapOf("dietId" to 1L))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("createExercise", mapOf("name" to "X"))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteExercise", mapOf("exerciseId" to 1L))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("createWorkout", mapOf("name" to "X"))).text())
                .contains("read-only")
            assertThat(client.callTool(McpSchema.CallToolRequest("deleteWorkout", mapOf("workoutId" to 1L))).text())
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
