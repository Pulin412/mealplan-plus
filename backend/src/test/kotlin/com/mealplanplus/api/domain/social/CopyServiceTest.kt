package com.mealplanplus.api.domain.social

import com.mealplanplus.api.domain.diet.Diet
import com.mealplanplus.api.domain.diet.DietFoodItem
import com.mealplanplus.api.domain.diet.DietFoodItemRepository
import com.mealplanplus.api.domain.diet.DietMeal
import com.mealplanplus.api.domain.diet.DietMealRepository
import com.mealplanplus.api.domain.diet.DietRepository
import com.mealplanplus.api.domain.food.Food
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.meal.Meal
import com.mealplanplus.api.domain.meal.MealFoodItem
import com.mealplanplus.api.domain.meal.MealFoodItemRepository
import com.mealplanplus.api.domain.meal.MealRepository
import com.mealplanplus.api.domain.user.User
import com.mealplanplus.api.domain.user.UserRepository
import com.mealplanplus.api.domain.workout.Exercise
import com.mealplanplus.api.domain.workout.ExerciseRepository
import com.mealplanplus.api.domain.workout.TemplateExercise
import com.mealplanplus.api.domain.workout.TemplateExerciseRepository
import com.mealplanplus.api.domain.workout.TemplateExerciseSet
import com.mealplanplus.api.domain.workout.TemplateExerciseSetRepository
import com.mealplanplus.api.domain.workout.WorkoutTemplate
import com.mealplanplus.api.domain.workout.WorkoutTemplateRepository
import com.mealplanplus.api.generated.model.CopyRequest
import com.mealplanplus.api.generated.model.CopyResultDto
import com.mealplanplus.api.generated.model.ProfileUpdateRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException

/**
 * P1 — copy engine. Verifies the locked dedupe spec (system reuse, manual match/mismatch,
 * exercise name-match, within-op caching), fresh containers, provenance stamping, and the gate.
 * H2, @Transactional-isolated.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class CopyServiceTest {

    @Autowired lateinit var copy: CopyService
    @Autowired lateinit var social: SocialService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var follows: FollowRepository
    @Autowired lateinit var foods: FoodRepository
    @Autowired lateinit var meals: MealRepository
    @Autowired lateinit var mealItems: MealFoodItemRepository
    @Autowired lateinit var diets: DietRepository
    @Autowired lateinit var dietMeals: DietMealRepository
    @Autowired lateinit var dietFoods: DietFoodItemRepository
    @Autowired lateinit var templates: WorkoutTemplateRepository
    @Autowired lateinit var templateExercises: TemplateExerciseRepository
    @Autowired lateinit var templateSets: TemplateExerciseSetRepository
    @Autowired lateinit var exercises: ExerciseRepository

    private val author = "uid-author"
    private val copier = "uid-copier"

    @BeforeEach
    fun setUp() {
        follows.deleteAll(); users.deleteAll()
        users.save(User(firebaseUid = author, displayName = "Author"))
        users.save(User(firebaseUid = copier, displayName = "Copier"))
    }

    // ── seed helpers ────────────────────────────────────────────────────────

    private fun food(uid: String?, name: String, cal: Double = 100.0, pro: Double = 10.0, carb: Double = 20.0, fat: Double = 5.0) =
        foods.save(Food(firebaseUid = uid, name = name, caloriesPer100 = cal, proteinPer100 = pro,
            carbsPer100 = carb, fatPer100 = fat, unit = "GRAM", isSystemFood = uid == null))

    private fun sharedMeal(uid: String, name: String, foodIds: List<Long>): Meal {
        val m = meals.save(Meal(firebaseUid = uid, name = name, isShared = true))
        foodIds.forEach { mealItems.save(MealFoodItem(mealId = m.id, foodId = it, quantity = 100.0, unit = "GRAM")) }
        return m
    }

    private fun sharedDiet(uid: String, mealIds: List<Long> = emptyList(), directFoodIds: List<Long> = emptyList(), shared: Boolean = true): Diet {
        val d = diets.save(Diet(firebaseUid = uid, name = "Plan", targetCalories = 1800.0, isShared = shared))
        mealIds.forEach { dietMeals.save(DietMeal(dietId = d.id, mealId = it, dayOfWeek = 0, slot = "Breakfast")) }
        directFoodIds.forEach { dietFoods.save(DietFoodItem(dietId = d.id, foodId = it, slot = "Snack", quantity = 50.0, unit = "GRAM")) }
        return d
    }

    private fun copierFoods() = foods.findByFirebaseUid(copier)

    // ── Dedupe: foods ─────────────────────────────────────────────────────────

    @Test
    fun `copying a diet clones containers fresh and creates a new manual food`() {
        val f = food(author, "Author Oats")
        val diet = sharedDiet(author, mealIds = listOf(sharedMeal(author, "Bowl", listOf(f.id)).id))

        val res = copy.copy(copier, author, CopyRequest.EntityType.DIET, diet.serverId)

        assertEquals(CopyResultDto.EntityType.DIET, res.entityType)
        val copied = diets.findByFirebaseUid(copier).single()
        assertEquals(author, copied.copiedFromUid)
        assertEquals(diet.serverId, copied.copiedFromServerId)      // provenance stamped
        assertEquals(1, meals.findByFirebaseUid(copier).size)       // meal cloned fresh
        val newFood = copierFoods().single()                        // manual food copied
        assertEquals(author, newFood.copiedFromUid)
        assertEquals(f.serverId, newFood.copiedFromServerId)
    }

    @Test
    fun `system foods are referenced directly, never copied`() {
        val sys = food(null, "System Rice")
        val diet = sharedDiet(author, mealIds = listOf(sharedMeal(author, "Rice Meal", listOf(sys.id)).id))

        copy.copy(copier, author, CopyRequest.EntityType.DIET, diet.serverId)

        assertEquals(0, copierFoods().size)   // nothing created; global row reused
        val item = mealItems.findByMealId(meals.findByFirebaseUid(copier).single().id).single()
        assertEquals(sys.id, item.foodId)     // points straight at the system food
    }

    @Test
    fun `a manual food matching the copier's own (name+unit+macros) is reused`() {
        food(copier, "Oats", cal = 380.0, pro = 13.0, carb = 67.0, fat = 7.0)         // copier already has it
        val authorFood = food(author, "oats", cal = 380.0, pro = 13.0, carb = 67.0, fat = 7.0)  // case-insensitive match
        val diet = sharedDiet(author, mealIds = listOf(sharedMeal(author, "Bowl", listOf(authorFood.id)).id))

        copy.copy(copier, author, CopyRequest.EntityType.DIET, diet.serverId)

        assertEquals(1, copierFoods().size)   // reused; nothing new created
    }

    @Test
    fun `a manual food with the same name but different macros is created fresh`() {
        food(copier, "Oats", cal = 380.0)
        val authorFood = food(author, "Oats", cal = 200.0)   // genuinely different food
        val diet = sharedDiet(author, mealIds = listOf(sharedMeal(author, "Bowl", listOf(authorFood.id)).id))

        copy.copy(copier, author, CopyRequest.EntityType.DIET, diet.serverId)

        assertEquals(2, copierFoods().size)   // pre-existing + the new one
    }

    @Test
    fun `a food referenced by multiple meals is resolved once (within-op cache)`() {
        val f = food(author, "Egg")
        val m1 = sharedMeal(author, "M1", listOf(f.id))
        val m2 = sharedMeal(author, "M2", listOf(f.id))
        val diet = sharedDiet(author, mealIds = listOf(m1.id, m2.id))

        copy.copy(copier, author, CopyRequest.EntityType.DIET, diet.serverId)

        assertEquals(1, copierFoods().size)                    // resolved once
        assertEquals(2, meals.findByFirebaseUid(copier).size)  // both meals still cloned fresh
    }

    // ── Meals + workouts ──────────────────────────────────────────────────────

    @Test
    fun `copying a meal clones it fresh with resolved foods`() {
        val f = food(author, "Chicken")
        val meal = sharedMeal(author, "Protein Meal", listOf(f.id))

        val res = copy.copy(copier, author, CopyRequest.EntityType.MEAL, meal.serverId)

        assertEquals(CopyResultDto.EntityType.MEAL, res.entityType)
        val copied = meals.findByFirebaseUid(copier).single()
        assertEquals(meal.serverId, copied.copiedFromServerId)
        assertEquals(1, copierFoods().size)
    }

    @Test
    fun `copying a workout dedupes exercises by name and references system ones directly`() {
        val sys = exercises.save(Exercise(firebaseUid = null, name = "Squat", isSystem = true))
        val authorEx = exercises.save(Exercise(firebaseUid = author, name = "Author Curl", isSystem = false))
        val t = templates.save(WorkoutTemplate(firebaseUid = author, name = "Push Day", isShared = true))
        templateExercises.save(TemplateExercise(templateId = t.id, exerciseId = sys.id, orderIndex = 0))
        val te = templateExercises.save(TemplateExercise(templateId = t.id, exerciseId = authorEx.id, orderIndex = 1))
        templateSets.save(TemplateExerciseSet(templateExerciseId = te.id, setNumber = 0, reps = 10, weightKg = 20.0))

        val res = copy.copy(copier, author, CopyRequest.EntityType.WORKOUT_TEMPLATE, t.serverId)

        assertEquals(CopyResultDto.EntityType.WORKOUT_TEMPLATE, res.entityType)
        val newTemplate = templates.findByFirebaseUid(copier).single()
        assertEquals(t.serverId, newTemplate.copiedFromServerId)
        // Only the manual exercise is copied; the system exercise is referenced directly.
        val copierManual = exercises.findByFirebaseUidOrIsSystemTrue(copier).filter { it.firebaseUid == copier }
        assertEquals(1, copierManual.size)
        assertEquals("Author Curl", copierManual.single().name)
        // Sets carried over.
        val newTe = templateExercises.findByTemplateIdOrderByOrderIndex(newTemplate.id)
        assertEquals(2, newTe.size)
        assertEquals(1, templateSets.findByTemplateExerciseIdOrderBySetNumber(newTe[1].id).size)
    }

    // ── Gate (via SocialService.copy) ─────────────────────────────────────────

    @Test
    fun `copy is refused for a non-follower and for unshared sources`() {
        social.updateMyProfile(author, ProfileUpdateRequest(handle = "author"))
        social.updateMyProfile(copier, ProfileUpdateRequest(handle = "copier"))
        val shared = sharedDiet(author)
        val private = sharedDiet(author, shared = false)

        // Not following → 403.
        val forbidden = assertThrows<ResponseStatusException> {
            social.copy(copier, CopyRequest(CopyRequest.EntityType.DIET, "author", shared.serverId))
        }
        assertEquals(HttpStatus.FORBIDDEN, forbidden.statusCode)

        // Following but the source isn't shared → 404.
        social.followUser(copier, "author")
        val notFound = assertThrows<ResponseStatusException> {
            social.copy(copier, CopyRequest(CopyRequest.EntityType.DIET, "author", private.serverId))
        }
        assertEquals(HttpStatus.NOT_FOUND, notFound.statusCode)

        // Following + shared → succeeds.
        val res = social.copy(copier, CopyRequest(CopyRequest.EntityType.DIET, "author", shared.serverId))
        assertNotNull(res.serverId)
        assertEquals(1, diets.findByFirebaseUid(copier).size)
    }
}
