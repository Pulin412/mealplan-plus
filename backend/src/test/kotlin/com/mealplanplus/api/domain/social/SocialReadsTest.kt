package com.mealplanplus.api.domain.social

import com.mealplanplus.api.domain.diet.DietService
import com.mealplanplus.api.domain.food.FoodService
import com.mealplanplus.api.domain.meal.MealService
import com.mealplanplus.api.domain.user.User
import com.mealplanplus.api.domain.user.UserRepository
import com.mealplanplus.api.domain.workout.WorkoutService
import com.mealplanplus.api.generated.model.DietDto
import com.mealplanplus.api.generated.model.DietMealDto
import com.mealplanplus.api.generated.model.ExerciseDto
import com.mealplanplus.api.generated.model.FoodDto
import com.mealplanplus.api.generated.model.FoodUnit
import com.mealplanplus.api.generated.model.MealDto
import com.mealplanplus.api.generated.model.MealFoodItemDto
import com.mealplanplus.api.generated.model.ProfileUpdateRequest
import com.mealplanplus.api.generated.model.TemplateExerciseDto
import com.mealplanplus.api.generated.model.WorkoutTemplateDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

/**
 * P2 — followers-gated, block-enforced reads of shared templates, plus the share toggles.
 * Runs on H2 in CI (no Docker). @Transactional rolls each test back for isolation
 * (this suite writes to diets/meals/foods/workouts, not just the social tables).
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class SocialReadsTest {

    @Autowired lateinit var social: SocialService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var follows: FollowRepository
    @Autowired lateinit var blocks: BlockRepository
    @Autowired lateinit var reports: ContentReportRepository
    @Autowired lateinit var diets: DietService
    @Autowired lateinit var meals: MealService
    @Autowired lateinit var workouts: WorkoutService
    @Autowired lateinit var foods: FoodService

    private val author = "uid-author"
    private val viewer = "uid-viewer"

    @BeforeEach
    fun setUp() {
        reports.deleteAll(); follows.deleteAll(); blocks.deleteAll(); users.deleteAll()
        users.save(User(firebaseUid = author, displayName = "Author"))
        users.save(User(firebaseUid = viewer, displayName = "Viewer"))
        social.updateMyProfile(author, ProfileUpdateRequest(handle = "author"))
        social.updateMyProfile(viewer, ProfileUpdateRequest(handle = "viewer"))
    }

    /** A shared diet → one meal → one food. Returns the diet's serverId. */
    private fun seedSharedDiet(shared: Boolean = true): UUID {
        val food = foods.create(
            FoodDto(name = "Oats", caloriesPer100 = 380.0, proteinPer100 = 13.0, carbsPer100 = 67.0, fatPer100 = 7.0),
            author,
        )
        val meal = meals.create(
            MealDto(name = "Breakfast Bowl", items = listOf(
                MealFoodItemDto(foodId = food.id!!, foodServerId = food.serverId, quantity = 100.0, unit = FoodUnit.GRAM))),
            author,
        )
        val diet = diets.create(
            DietDto(name = "Cut Plan", targetCalories = 1800.0, meals = listOf(
                DietMealDto(mealId = meal.id!!, mealServerId = meal.serverId, dayOfWeek = 0, slot = "Breakfast"))),
            author,
        )
        if (shared) diets.toggleShare(diet.serverId!!, author)
        return diet.serverId!!
    }

    private fun follow() = social.followUser(viewer, "author")

    // ── Gating ───────────────────────────────────────────────────────────────

    @Test
    fun `non-follower is gated with 403 on list and detail`() {
        val sid = seedSharedDiet()
        val listEx = assertThrows<ResponseStatusException> { social.listSharedDiets(viewer, "author") }
        assertEquals(HttpStatus.FORBIDDEN, listEx.statusCode)
        val detailEx = assertThrows<ResponseStatusException> { social.getSharedDiet(viewer, "author", sid) }
        assertEquals(HttpStatus.FORBIDDEN, detailEx.statusCode)
    }

    @Test
    fun `follower sees shared diet while unshared is excluded`() {
        seedSharedDiet(shared = true)
        seedSharedDiet(shared = false)   // a second diet left private
        follow()
        val list = social.listSharedDiets(viewer, "author")
        assertEquals(1, list.size)
        assertEquals("Cut Plan", list[0].name)
        assertEquals("1800 kcal", list[0].subtitle)
    }

    @Test
    fun `shared diet detail bundles the referenced meal and food`() {
        val sid = seedSharedDiet()
        follow()
        val bundle = social.getSharedDiet(viewer, "author", sid)
        assertEquals("Cut Plan", bundle.diet.name)
        assertEquals(1, bundle.meals!!.size)
        assertEquals("Breakfast Bowl", bundle.meals!![0].name)
        assertEquals(1, bundle.foods!!.size)
        assertEquals("Oats", bundle.foods!![0].name)
    }

    @Test
    fun `blocking severs access to the shared library`() {
        val sid = seedSharedDiet()
        follow()
        social.blockUser(author, "viewer")   // author blocks viewer → severs follow
        val ex = assertThrows<ResponseStatusException> { social.getSharedDiet(viewer, "author", sid) }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }

    @Test
    fun `author can read their own shared library without following`() {
        val sid = seedSharedDiet()
        val bundle = social.getSharedDiet(author, "author", sid)
        assertEquals("Cut Plan", bundle.diet.name)
        assertEquals(1, social.listSharedDiets(author, "author").size)
    }

    @Test
    fun `unshared item detail 404s even for a follower`() {
        val sid = seedSharedDiet(shared = false)
        follow()
        val ex = assertThrows<ResponseStatusException> { social.getSharedDiet(viewer, "author", sid) }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    // ── Share toggle ─────────────────────────────────────────────────────────

    @Test
    fun `toggling share flips visibility to followers`() {
        seedSharedDiet(shared = false)
        follow()
        assertTrue(social.listSharedDiets(viewer, "author").isEmpty())
        val ownerDiet = diets.list(author).first()
        diets.toggleShare(ownerDiet.serverId!!, author)
        assertEquals(1, social.listSharedDiets(viewer, "author").size)
    }

    // ── Meals + workouts ───────────────────────────────────────────────────────

    @Test
    fun `shared meal detail bundles its foods`() {
        val food = foods.create(
            FoodDto(name = "Chicken", caloriesPer100 = 165.0, proteinPer100 = 31.0, carbsPer100 = 0.0, fatPer100 = 3.6),
            author,
        )
        val meal = meals.create(
            MealDto(name = "Protein Meal", items = listOf(
                MealFoodItemDto(foodId = food.id!!, foodServerId = food.serverId, quantity = 200.0, unit = FoodUnit.GRAM))),
            author,
        )
        meals.toggleShare(meal.serverId!!, author)
        follow()
        val bundle = social.getSharedMeal(viewer, "author", meal.serverId!!)
        assertEquals("Protein Meal", bundle.meal.name)
        assertEquals(1, bundle.foods!!.size)
        assertEquals("Chicken", bundle.foods!![0].name)
    }

    @Test
    fun `shared workout detail bundles its exercises`() {
        val exercise = workouts.createExercise(ExerciseDto(name = "Bench Press"), author)
        val template = workouts.createTemplate(
            WorkoutTemplateDto(name = "Push Day", exercises = listOf(
                TemplateExerciseDto(exerciseId = exercise.id!!, orderIndex = 0))),
            author,
        )
        workouts.toggleTemplateShare(template.serverId!!, author)
        follow()
        val bundle = social.getSharedWorkout(viewer, "author", template.serverId!!)
        assertEquals("Push Day", bundle.workout.name)
        assertEquals(1, bundle.exercises!!.size)
        assertEquals("Bench Press", bundle.exercises!![0].name)
    }
}
