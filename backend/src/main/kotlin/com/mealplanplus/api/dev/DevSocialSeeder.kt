package com.mealplanplus.api.dev

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.mealplanplus.api.domain.diet.Diet
import com.mealplanplus.api.domain.diet.DietMeal
import com.mealplanplus.api.domain.diet.DietMealRepository
import com.mealplanplus.api.domain.diet.DietRepository
import com.mealplanplus.api.domain.food.Food
import com.mealplanplus.api.domain.food.FoodRepository
import com.mealplanplus.api.domain.meal.Meal
import com.mealplanplus.api.domain.meal.MealFoodItem
import com.mealplanplus.api.domain.meal.MealFoodItemRepository
import com.mealplanplus.api.domain.meal.MealRepository
import com.mealplanplus.api.domain.social.Follow
import com.mealplanplus.api.domain.social.FollowRepository
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
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * DEV-ONLY. Seeds a handful of *login-able* dummy accounts (real Firebase email/password users, so
 * you can sign in as them on either client) each with shared diets/meals/exercises/workouts, and
 * wires a follow graph among them (and, optionally, following a configured primary tester).
 *
 * Activated only when `dev.seed-social.firebase-api-key` is set — never in prod/tests/CI. Firebase
 * accounts persist across restarts (only the H2 data is re-seeded), so it signs in on later runs.
 *
 * Local use:
 *   DEV_SEED_SOCIAL_FIREBASE_API_KEY=<web-api-key> \
 *   DEV_SEED_SOCIAL_PRIMARY_UID=<your-firebase-uid> \
 *   ./gradlew bootRun
 */
@Component
@ConditionalOnProperty("dev.seed-social.firebase-api-key")
class DevSocialSeeder(
    private val users: UserRepository,
    private val follows: FollowRepository,
    private val diets: DietRepository,
    private val dietMeals: DietMealRepository,
    private val meals: MealRepository,
    private val mealItems: MealFoodItemRepository,
    private val foods: FoodRepository,
    private val templates: WorkoutTemplateRepository,
    private val templateExercises: TemplateExerciseRepository,
    private val templateSets: TemplateExerciseSetRepository,
    private val exercises: ExerciseRepository,
    private val mapper: ObjectMapper,
    @Value("\${dev.seed-social.firebase-api-key}") private val apiKey: String,
    @Value("\${dev.seed-social.primary-uid:}") private val primaryUid: String,
    @Value("\${dev.seed-social.password:mealplan123}") private val password: String,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(javaClass)
    private val http: HttpClient = HttpClient.newHttpClient()

    private data class Dummy(val email: String, val handle: String, val name: String, val bio: String)

    private val dummies = listOf(
        Dummy("alex@mealplan.test", "alex_fit", "Alex Carter", "Powerlifting + high-protein meal preps 💪"),
        Dummy("priya@mealplan.test", "priya_eats", "Priya Sharma", "Vegetarian macros & balanced plates 🥗"),
        Dummy("sam@mealplan.test", "sam_lifts", "Sam Okoye", "PPL splits and lean bulking 🏋️"),
        Dummy("maya@mealplan.test", "maya_meals", "Maya Nolan", "Quick cutting meals under 500 kcal 🔥"),
        Dummy("leo@mealplan.test", "leo_gains", "Leo Martins", "Full-body + calorie-dense recipes 🍚"),
    )

    @Transactional
    override fun run(args: ApplicationArguments?) {
        val seeded = mutableListOf<Pair<Dummy, String>>()   // dummy + resolved firebase uid
        dummies.forEach { d ->
            val uid = runCatching { firebaseUid(d.email) }.getOrNull()
            if (uid == null) { log.warn("DevSocialSeeder: could not create/sign-in {} — skipping", d.email); return@forEach }
            seedUser(d, uid)
            seeded += d to uid
        }
        // Follow graph: each dummy follows the next (a ring) + the primary tester follows none by
        // default; the dummies all follow the primary so the tester sees followers.
        seeded.forEachIndexed { i, (_, uid) ->
            val next = seeded[(i + 1) % seeded.size].second
            if (uid != next) follows.save(Follow(followerUid = uid, followeeUid = next))
            if (primaryUid.isNotBlank() && uid != primaryUid) follows.save(Follow(followerUid = uid, followeeUid = primaryUid))
        }
        log.info("DevSocialSeeder: {} login-able dummy accounts ready (password '{}'): {}",
            seeded.size, password, seeded.joinToString { it.first.email })
    }

    private fun seedUser(d: Dummy, uid: String) {
        val existing = users.findByFirebaseUid(uid)
        if (existing == null) {
            users.save(User(firebaseUid = uid, email = d.email, displayName = d.name,
                handle = d.handle, avatarSeed = d.handle, isSearchable = true, bio = d.bio))
        } else {
            existing.handle = d.handle; existing.displayName = d.name; existing.avatarSeed = d.handle
            existing.isSearchable = true; existing.bio = d.bio; users.save(existing)
        }

        val first = d.name.split(" ").first()
        val food = foods.save(Food(firebaseUid = uid, name = "$first's Oats", caloriesPer100 = 380.0,
            proteinPer100 = 13.0, carbsPer100 = 67.0, fatPer100 = 7.0, unit = "GRAM"))
        val meal = meals.save(Meal(firebaseUid = uid, name = "${d.handle} Breakfast Bowl",
            slots = listOf("Breakfast"), isShared = true))
        mealItems.save(MealFoodItem(mealId = meal.id, foodId = food.id, quantity = 100.0, unit = "GRAM"))

        val diet = diets.save(Diet(firebaseUid = uid, name = "${d.name} Day Plan",
            targetCalories = 1900.0, targetProtein = 160.0, isShared = true))
        dietMeals.save(DietMeal(dietId = diet.id, mealId = meal.id, dayOfWeek = 0, slot = "Breakfast"))

        val ex = exercises.save(Exercise(firebaseUid = uid, name = "${d.handle} Bench Press", isSystem = false))
        val tmpl = templates.save(WorkoutTemplate(firebaseUid = uid, name = "${d.name} Push Day", isShared = true))
        val te = templateExercises.save(TemplateExercise(templateId = tmpl.id, exerciseId = ex.id, orderIndex = 0))
        templateSets.save(TemplateExerciseSet(templateExerciseId = te.id, setNumber = 0, reps = 8, weightKg = 60.0))
    }

    /** signUp (create) the account, or signInWithPassword if it already exists. Returns the uid (localId). */
    private fun firebaseUid(email: String): String {
        val payload = mapper.writeValueAsString(mapOf("email" to email, "password" to password, "returnSecureToken" to true))
        val signUp = post("accounts:signUp", payload)
        if (signUp.statusCode() == 200) return mapper.readTree(signUp.body())["localId"].asText()
        // Already exists (or other) → try sign-in.
        val signIn = post("accounts:signInWithPassword", payload)
        if (signIn.statusCode() == 200) return mapper.readTree(signIn.body())["localId"].asText()
        val err: JsonNode = runCatching { mapper.readTree(signIn.body()) }.getOrNull() ?: mapper.createObjectNode()
        error("Firebase auth failed for $email: ${err.path("error").path("message").asText(signIn.body())}")
    }

    private fun post(action: String, body: String): HttpResponse<String> {
        val req = HttpRequest.newBuilder(URI.create("https://identitytoolkit.googleapis.com/v1/$action?key=$apiKey"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body)).build()
        return http.send(req, HttpResponse.BodyHandlers.ofString())
    }
}
