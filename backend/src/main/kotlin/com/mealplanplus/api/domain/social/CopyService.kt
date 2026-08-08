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
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.util.UUID
import kotlin.math.abs

/**
 * Deep-clones another user's shared template into the caller's own library (issue #175, P1).
 *
 * Dedupe (locked spec): only **leaves** (foods, exercises) are deduplicated. Containers — the
 * diet/meal/workout template, and a diet's constituent meals — are always created fresh.
 *  - System foods/exercises are referenced directly (global rows valid for everyone).
 *  - Manual foods are matched against the copier's own by (normalizedName, unit, per-100 macros);
 *    manual exercises by normalizedName only. Match → reuse; no match → create fresh, stamped
 *    with provenance (copiedFromUid / copiedFromServerId).
 * Resolutions are cached within a single copy op (a food referenced by 3 meals is resolved once).
 *
 * Callers MUST enforce the follows/block gate + is_shared before delegating here; this service
 * re-checks author ownership + is_shared as a safety net (404 otherwise).
 */
@Service
class CopyService(
    private val dietRepo: DietRepository,
    private val dietMealRepo: DietMealRepository,
    private val dietFoodItemRepo: DietFoodItemRepository,
    private val mealRepo: MealRepository,
    private val mealFoodItemRepo: MealFoodItemRepository,
    private val foodRepo: FoodRepository,
    private val templateRepo: WorkoutTemplateRepository,
    private val templateExerciseRepo: TemplateExerciseRepository,
    private val templateSetRepo: TemplateExerciseSetRepository,
    private val exerciseRepo: ExerciseRepository,
) {
    @Transactional
    fun copy(copierUid: String, authorUid: String, entityType: CopyRequest.EntityType, sourceServerId: UUID): CopyResultDto =
        when (entityType) {
            CopyRequest.EntityType.DIET -> copyDiet(copierUid, authorUid, sourceServerId)
            CopyRequest.EntityType.MEAL -> copyMeal(copierUid, authorUid, sourceServerId)
            CopyRequest.EntityType.WORKOUT_TEMPLATE -> copyWorkout(copierUid, authorUid, sourceServerId)
        }

    private fun copyDiet(copierUid: String, authorUid: String, sourceServerId: UUID): CopyResultDto {
        val src = dietRepo.findByServerId(sourceServerId)?.takeIf { it.firebaseUid == authorUid && it.isShared }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
        val foods = FoodResolver(copierUid, authorUid)

        val newDiet = dietRepo.save(
            Diet(
                firebaseUid = copierUid, name = src.name, description = src.description,
                targetCalories = src.targetCalories, targetProtein = src.targetProtein,
                targetCarbs = src.targetCarbs, targetFat = src.targetFat,
                copiedFromUid = authorUid, copiedFromServerId = src.serverId,
            )
        )

        // Constituent meals are cloned fresh (meal-level dedupe is deferred to v2).
        val newMealIdByAuthorMealId = HashMap<Long, Long>()
        val dietMeals = dietMealRepo.findByDietId(src.id)
        dietMeals.map { it.mealId }.distinct().forEach { authorMealId ->
            val srcMeal = mealRepo.findById(authorMealId).orElse(null) ?: return@forEach
            val newMeal = mealRepo.save(
                Meal(
                    firebaseUid = copierUid, name = srcMeal.name, slots = srcMeal.slots,
                    copiedFromUid = authorUid, copiedFromServerId = srcMeal.serverId,
                )
            )
            mealFoodItemRepo.findByMealId(srcMeal.id).forEach { item ->
                mealFoodItemRepo.save(
                    MealFoodItem(
                        mealId = newMeal.id, foodId = foods.resolve(item.foodId),
                        quantity = item.quantity, unit = item.unit, notes = item.notes,
                    )
                )
            }
            newMealIdByAuthorMealId[authorMealId] = newMeal.id
        }
        dietMeals.forEach { dm ->
            val newMealId = newMealIdByAuthorMealId[dm.mealId] ?: return@forEach
            dietMealRepo.save(
                DietMeal(dietId = newDiet.id, mealId = newMealId, dayOfWeek = dm.dayOfWeek,
                    slot = dm.slot, instructions = dm.instructions)
            )
        }
        dietFoodItemRepo.findByDietId(src.id).forEach { fi ->
            dietFoodItemRepo.save(
                DietFoodItem(dietId = newDiet.id, foodId = foods.resolve(fi.foodId),
                    slot = fi.slot, quantity = fi.quantity, unit = fi.unit)
            )
        }
        return CopyResultDto(CopyResultDto.EntityType.DIET, newDiet.serverId, newDiet.name)
    }

    private fun copyMeal(copierUid: String, authorUid: String, sourceServerId: UUID): CopyResultDto {
        val src = mealRepo.findByServerId(sourceServerId)?.takeIf { it.firebaseUid == authorUid && it.isShared }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
        val foods = FoodResolver(copierUid, authorUid)
        val newMeal = mealRepo.save(
            Meal(
                firebaseUid = copierUid, name = src.name, slots = src.slots,
                copiedFromUid = authorUid, copiedFromServerId = src.serverId,
            )
        )
        mealFoodItemRepo.findByMealId(src.id).forEach { item ->
            mealFoodItemRepo.save(
                MealFoodItem(mealId = newMeal.id, foodId = foods.resolve(item.foodId),
                    quantity = item.quantity, unit = item.unit, notes = item.notes)
            )
        }
        return CopyResultDto(CopyResultDto.EntityType.MEAL, newMeal.serverId, newMeal.name)
    }

    private fun copyWorkout(copierUid: String, authorUid: String, sourceServerId: UUID): CopyResultDto {
        val src = templateRepo.findByServerId(sourceServerId)?.takeIf { it.firebaseUid == authorUid && it.isShared }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Not found")
        val exercises = ExerciseResolver(copierUid, authorUid)
        val newTemplate = templateRepo.save(
            WorkoutTemplate(
                firebaseUid = copierUid, name = src.name, category = src.category, notes = src.notes,
                copiedFromUid = authorUid, copiedFromServerId = src.serverId,
            )
        )
        templateExerciseRepo.findByTemplateIdOrderByOrderIndex(src.id).forEach { te ->
            val newTe = templateExerciseRepo.save(
                TemplateExercise(templateId = newTemplate.id, exerciseId = exercises.resolve(te.exerciseId),
                    orderIndex = te.orderIndex, notes = te.notes)
            )
            templateSetRepo.findByTemplateExerciseIdOrderBySetNumber(te.id).forEach { s ->
                templateSetRepo.save(
                    TemplateExerciseSet(templateExerciseId = newTe.id, setNumber = s.setNumber,
                        reps = s.reps, weightKg = s.weightKg)
                )
            }
        }
        return CopyResultDto(CopyResultDto.EntityType.WORKOUT_TEMPLATE, newTemplate.serverId, newTemplate.name)
    }

    // ── Leaf resolvers (dedupe + provenance, cached per copy op) ─────────────────

    private inner class FoodResolver(val copierUid: String, val authorUid: String) {
        private val mine = foodRepo.findByFirebaseUid(copierUid).toMutableList()
        private val cache = HashMap<Long, Long>()

        fun resolve(authorFoodId: Long): Long = cache.getOrPut(authorFoodId) {
            val src = foodRepo.findById(authorFoodId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Referenced food missing")
            }
            if (src.isSystemFood) src.id
            else mine.firstOrNull { matches(it, src) }?.id ?: create(src).id
        }

        private fun matches(a: Food, b: Food) =
            normalize(a.name) == normalize(b.name) && a.unit == b.unit &&
                eq(a.caloriesPer100, b.caloriesPer100) && eq(a.proteinPer100, b.proteinPer100) &&
                eq(a.carbsPer100, b.carbsPer100) && eq(a.fatPer100, b.fatPer100)

        private fun create(src: Food): Food = foodRepo.save(
            Food(
                firebaseUid = copierUid, name = src.name, brand = src.brand, barcode = src.barcode,
                category = src.category, caloriesPer100 = src.caloriesPer100, proteinPer100 = src.proteinPer100,
                carbsPer100 = src.carbsPer100, fatPer100 = src.fatPer100, unit = src.unit,
                gramsPerPiece = src.gramsPerPiece, gramsPerCup = src.gramsPerCup,
                gramsPerTbsp = src.gramsPerTbsp, gramsPerTsp = src.gramsPerTsp,
                glycemicIndex = src.glycemicIndex, isSystemFood = false,
                copiedFromUid = authorUid, copiedFromServerId = src.serverId,
            )
        ).also { mine.add(it) }
    }

    private inner class ExerciseResolver(val copierUid: String, val authorUid: String) {
        private val mine = exerciseRepo.findByFirebaseUidOrIsSystemTrue(copierUid)
            .filter { !it.isSystem && it.firebaseUid == copierUid }.toMutableList()
        private val cache = HashMap<Long, Long>()

        fun resolve(authorExerciseId: Long): Long = cache.getOrPut(authorExerciseId) {
            val src = exerciseRepo.findById(authorExerciseId).orElseThrow {
                ResponseStatusException(HttpStatus.NOT_FOUND, "Referenced exercise missing")
            }
            if (src.isSystem) src.id
            else mine.firstOrNull { normalize(it.name) == normalize(src.name) }?.id ?: create(src).id
        }

        private fun create(src: Exercise): Exercise = exerciseRepo.save(
            Exercise(
                firebaseUid = copierUid, name = src.name, description = src.description, isSystem = false,
                copiedFromUid = authorUid, copiedFromServerId = src.serverId,
            )
        ).also { mine.add(it) }
    }

    private fun normalize(s: String) = s.lowercase().trim().replace(Regex("\\s+"), " ")
    private fun eq(a: Double, b: Double) = abs(a - b) < 0.01
}
