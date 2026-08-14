package com.mealplanplus.api.domain.agent

import com.mealplanplus.api.domain.food.Food
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.log.LoggedFoodRepository
import com.mealplanplus.api.domain.user.ActivityLevelEnum
import com.mealplanplus.api.domain.user.GoalTypeEnum
import com.mealplanplus.api.domain.user.User
import com.mealplanplus.api.domain.user.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Exercises the agent's @Tool functions directly — no LLM, no provider, no API key. The key is only
 * needed for a model to *choose* a tool; the tools themselves are plain backend calls, so their
 * correctness (search+match+log, ambiguity handling, profile formatting) is fully testable offline.
 * H2, @Transactional-isolated.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class MealPlanToolServiceTest {

    @Autowired lateinit var tools: MealPlanToolService
    @Autowired lateinit var foods: FoodRepository
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var loggedFoods: LoggedFoodRepository

    private val uid = "uid-agent-test"
    private val today = LocalDate.now().toString()

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(uid, null)
        users.save(User(firebaseUid = uid, displayName = "Tester"))
    }

    @AfterEach
    fun tearDown() = SecurityContextHolder.clearContext()

    private fun food(name: String, cal: Double = 380.0) =
        foods.save(Food(firebaseUid = uid, name = name, caloriesPer100 = cal, proteinPer100 = 13.0,
            carbsPer100 = 67.0, fatPer100 = 7.0, unit = "GRAM", isSystemFood = false))

    // ── logFoodByName: the one-hop write path ────────────────────────────────

    @Test
    fun `logFoodByName logs in one step on an exact name match`() {
        food("Zoats")

        val result = tools.logFoodByName("Zoats", 100.0, "GRAM", "BREAKFAST", today)

        assertTrue(result.startsWith("Logged"), "should confirm the log, got: $result")
        assertTrue(result.contains("Zoats"))
        assertEquals(1, loggedFoods.count(), "exactly one entry persisted")
    }

    @Test
    fun `logFoodByName logs when there is a single fuzzy match`() {
        food("Zbanana Split")

        val result = tools.logFoodByName("Zbanana", 1.0, "PIECE", "MORNING_SNACK", today)

        assertTrue(result.startsWith("Logged"), "got: $result")
        assertEquals(1, loggedFoods.count())
    }

    @Test
    fun `logFoodByName refuses to guess when the match is ambiguous`() {
        food("Zoatmeal Classic")
        food("Zoatmeal Deluxe")

        val result = tools.logFoodByName("Zoatmeal", 100.0, "GRAM", "BREAKFAST", today)

        assertTrue(result.contains("ambiguous", ignoreCase = true), "should flag ambiguity, got: $result")
        assertTrue(result.contains("id="), "should surface candidate ids for logFood fallback")
        assertEquals(0, loggedFoods.count(), "nothing logged when ambiguous")
    }

    @Test
    fun `logFoodByName reports no match without logging`() {
        val result = tools.logFoodByName("Znotafood", 100.0, "GRAM", "LUNCH", today)

        assertTrue(result.contains("No foods found", ignoreCase = true), "got: $result")
        assertEquals(0, loggedFoods.count())
    }

    @Test
    fun `logFood by id persists and getTodayLog then reflects it`() {
        val f = food("Zchicken", cal = 165.0)

        val logged = tools.logFood(f.id, 200.0, "GRAM", "DINNER", today)
        assertTrue(logged.startsWith("Logged"), "got: $logged")

        val summary = tools.getTodayLog(today)
        assertTrue(summary.contains("DINNER"), "today log should show the entry, got: $summary")
        assertTrue(summary.contains("foodId=${f.id}"))
    }

    // ── getProfile ────────────────────────────────────────────────────────────

    @Test
    fun `getProfile summarises the user's goal and targets`() {
        val u = users.findByFirebaseUid(uid)!!
        u.age = 30; u.weightKg = 75.0; u.goalType = GoalTypeEnum.LOSE_WEIGHT
        u.activityLevel = ActivityLevelEnum.MODERATELY_ACTIVE; u.targetCalories = 2000; u.targetProtein = 150
        users.save(u)

        val result = tools.getProfile()

        assertTrue(result.contains("LOSE_WEIGHT"), "got: $result")
        assertTrue(result.contains("2000 kcal"))
        assertTrue(result.contains("150g protein"))
        assertFalse(result.contains("null"))
    }

    @Test
    fun `getProfile handles a bare profile without inventing data`() {
        // Only the default profile from setUp() — no targets set.
        val result = tools.getProfile()
        assertFalse(result.contains("kcal"), "no target calories should be reported, got: $result")
        assertTrue(result.contains("units"), "preferred units always present")
    }
}
