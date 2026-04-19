package com.mealplanplus.ui.screens.profile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.data.local.BackupDataImporter
import com.mealplanplus.data.model.ActivityLevel
import com.mealplanplus.data.model.Diet
import com.mealplanplus.data.model.Exercise
import com.mealplanplus.data.model.ExerciseCategory
import com.mealplanplus.data.model.FoodItem
import com.mealplanplus.data.model.FoodUnit
import com.mealplanplus.data.model.Gender
import com.mealplanplus.data.model.GoalType
import com.mealplanplus.data.model.Meal
import com.mealplanplus.data.model.User
import com.mealplanplus.data.model.WorkoutTemplate
import com.mealplanplus.data.model.WorkoutTemplateCategory
import com.mealplanplus.data.model.WorkoutTemplateSet
import com.mealplanplus.data.model.WorkoutTemplateExercise
import com.mealplanplus.data.repository.AuthRepository
import com.mealplanplus.data.repository.DietRepository
import com.mealplanplus.data.repository.FoodRepository
import com.mealplanplus.data.repository.MealRepository
import com.mealplanplus.data.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import kotlin.math.roundToInt

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isClearing: Boolean = false,
    val saveSuccess: Boolean = false,
    val clearSuccess: Boolean = false,
    val error: String? = null,
    // edit state
    val name: String = "",
    val age: String = "",
    val contact: String = "",
    val weightKg: String = "",
    val heightCm: String = "",
    val gender: Gender? = null,
    val activityLevel: ActivityLevel? = null,
    val targetCalories: String = "",
    val goalType: GoalType? = null,
    // computed estimates (null = insufficient data)
    val computedBmr: Int? = null,
    val computedTdee: Int? = null,
    val computedBodyFatPct: Double? = null,
    // dialogs
    val showClearDataDialog: Boolean = false,
    val showDeleteAccountDialog: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val dietRepository: DietRepository,
    private val mealRepository: MealRepository,
    private val foodRepository: FoodRepository,
    private val workoutRepository: WorkoutRepository,
    private val backupDataImporter: BackupDataImporter,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _importResult = MutableStateFlow<String?>(null)
    val importResult: StateFlow<String?> = _importResult.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            authRepository.getCurrentUserId().collect { userId ->
                if (userId != null) {
                    authRepository.getCurrentUser(userId).collect { user ->
                        val gender = user?.gender?.let { runCatching { Gender.valueOf(it) }.getOrNull() }
                        val activity = user?.activityLevel?.let { runCatching { ActivityLevel.valueOf(it) }.getOrNull() }
                        val goal = user?.goalType?.let { runCatching { GoalType.valueOf(it) }.getOrNull() }
                        val w = user?.weightKg
                        val h = user?.heightCm
                        val a = user?.age
                        val bmr = computeBmr(w, h, a, gender)
                        val tdee = if (bmr != null && activity != null) (bmr * activity.multiplier).roundToInt() else null
                        val bodyFat = computeBodyFat(w, h, a, gender)
                        _uiState.update {
                            it.copy(
                                user = user,
                                isLoading = false,
                                name = user?.displayName ?: "",
                                age = user?.age?.toString() ?: "",
                                contact = user?.contact ?: "",
                                weightKg = user?.weightKg?.let { v -> if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v) } ?: "",
                                heightCm = user?.heightCm?.let { v -> if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v) } ?: "",
                                gender = gender,
                                activityLevel = activity,
                                targetCalories = user?.targetCalories?.toString() ?: "",
                                goalType = goal,
                                computedBmr = bmr,
                                computedTdee = tdee,
                                computedBodyFatPct = bodyFat
                            )
                        }
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    // ─── Field updaters ───────────────────────────────────────────────────────

    fun updateName(v: String) = _uiState.update { it.copy(name = v) }
    fun updateAge(v: String) = _uiState.update { it.copy(age = v) }
    fun updateContact(v: String) = _uiState.update { it.copy(contact = v) }
    fun updateGender(g: Gender) = _uiState.update { recompute(it.copy(gender = g)) }
    fun updateGoalType(g: GoalType) = _uiState.update { it.copy(goalType = g) }
    fun updateTargetCalories(v: String) = _uiState.update { it.copy(targetCalories = v) }

    fun updateWeight(v: String) {
        _uiState.update { recompute(it.copy(weightKg = v)) }
    }

    fun updateHeight(v: String) {
        _uiState.update { recompute(it.copy(heightCm = v)) }
    }

    fun updateActivityLevel(a: ActivityLevel) {
        _uiState.update { state ->
            val newState = state.copy(activityLevel = a)
            val tdee = if (newState.computedBmr != null) (newState.computedBmr * a.multiplier).roundToInt() else null
            // auto-fill targetCalories from TDEE if blank
            val tc = if (newState.targetCalories.isBlank() && tdee != null) tdee.toString() else newState.targetCalories
            newState.copy(computedTdee = tdee, targetCalories = tc)
        }
    }

    private fun recompute(state: ProfileUiState): ProfileUiState {
        val w = state.weightKg.toDoubleOrNull()
        val h = state.heightCm.toDoubleOrNull()
        val a = state.age.toIntOrNull()
        val g = state.gender
        val bmr = computeBmr(w, h, a, g)
        val tdee = if (bmr != null && state.activityLevel != null) (bmr * state.activityLevel.multiplier).roundToInt() else null
        val bodyFat = computeBodyFat(w, h, a, g)
        val tc = if (state.targetCalories.isBlank() && tdee != null) tdee.toString() else state.targetCalories
        return state.copy(computedBmr = bmr, computedTdee = tdee, computedBodyFatPct = bodyFat, targetCalories = tc)
    }

    // ─── BMR / estimates ──────────────────────────────────────────────────────

    // Mifflin-St Jeor
    private fun computeBmr(w: Double?, h: Double?, a: Int?, g: Gender?): Int? {
        if (w == null || h == null || a == null || g == null) return null
        val base = 10.0 * w + 6.25 * h - 5.0 * a
        return when (g) {
            Gender.MALE -> (base + 5).roundToInt()
            Gender.FEMALE -> (base - 161).roundToInt()
            Gender.OTHER -> (base - 78).roundToInt() // avg of male/female offsets
        }
    }

    // Deurenberg BMI-based body fat %
    private fun computeBodyFat(w: Double?, h: Double?, a: Int?, g: Gender?): Double? {
        if (w == null || h == null || a == null || g == null || g == Gender.OTHER) return null
        val hM = h / 100.0
        val bmi = w / (hM * hM)
        val offset = if (g == Gender.MALE) 16.2 else 5.4
        val pct = 1.20 * bmi + 0.23 * a - offset
        return if (pct < 0) null else Math.round(pct * 10) / 10.0
    }

    // ─── Save ─────────────────────────────────────────────────────────────────

    fun saveProfile() {
        val currentUser = _uiState.value.user ?: return
        val state = _uiState.value

        // If targetCalories blank, use TDEE; if still null, store null
        val tc = state.targetCalories.toIntOrNull()
            ?: state.computedTdee

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val updatedUser = currentUser.copy(
                displayName = state.name.ifBlank { null },
                age = state.age.toIntOrNull(),
                contact = state.contact.ifBlank { null },
                weightKg = state.weightKg.toDoubleOrNull(),
                heightCm = state.heightCm.toDoubleOrNull(),
                gender = state.gender?.name,
                activityLevel = state.activityLevel?.name,
                targetCalories = tc,
                goalType = state.goalType?.name
            )
            val result = authRepository.updateProfile(updatedUser)
            result.fold(
                onSuccess = { _uiState.update { it.copy(isSaving = false, saveSuccess = true) } },
                onFailure = { e -> _uiState.update { it.copy(isSaving = false, error = e.message ?: "Failed to save") } }
            )
        }
    }

    // ─── Logout ───────────────────────────────────────────────────────────────

    fun logout() {
        viewModelScope.launch { authRepository.signOut() }
    }

    // ─── Clear / Delete ───────────────────────────────────────────────────────

    fun showClearDataDialog() = _uiState.update { it.copy(showClearDataDialog = true) }
    fun dismissClearDataDialog() = _uiState.update { it.copy(showClearDataDialog = false) }

    fun confirmClearData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true, showClearDataDialog = false) }
            authRepository.clearAllUserData()
            _uiState.update { it.copy(isClearing = false, clearSuccess = true) }
        }
    }

    fun showDeleteAccountDialog() = _uiState.update { it.copy(showDeleteAccountDialog = true) }
    fun dismissDeleteAccountDialog() = _uiState.update { it.copy(showDeleteAccountDialog = false) }

    fun confirmDeleteAccount(onLogout: () -> Unit) {
        val userId = _uiState.value.user?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true, showDeleteAccountDialog = false) }
            authRepository.deleteAccount(userId)
            onLogout()
        }
    }

    // ─── State cleanup ────────────────────────────────────────────────────────

    fun clearSaveSuccess() = _uiState.update { it.copy(saveSuccess = false) }
    fun clearClearSuccess() = _uiState.update { it.copy(clearSuccess = false) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    // ─── Import ───────────────────────────────────────────────────────────────

    fun importDietsFromJson(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText()
                    ?: run { _importResult.value = "Failed to read file"; return@launch }

                val root = org.json.JSONObject(json)
                val diets = root.optJSONArray("diets") ?: run {
                    _importResult.value = "Invalid format: missing 'diets' array"; return@launch
                }

                var imported = 0
                var skipped = 0

                for (i in 0 until diets.length()) {
                    try {
                        val dietJson = diets.getJSONObject(i)
                        val dietName = dietJson.optString("name", "Imported Diet ${i + 1}")
                        val dietDesc = dietJson.optString("description", null)

                        val dietId = dietRepository.insertDiet(
                            Diet(name = dietName, description = dietDesc)
                        )

                        val mealsJson = dietJson.optJSONObject("meals")
                        mealsJson?.keys()?.forEach { slotKey ->
                            try {
                                val mealJson = mealsJson.getJSONObject(slotKey)
                                val instructions = mealJson.optString("instructions", null)

                                val mealId = mealRepository.insertMeal(
                                    Meal(name = "$dietName - $slotKey")
                                )

                                dietRepository.setMealForSlot(dietId, slotKey, mealId)
                                if (!instructions.isNullOrBlank()) {
                                    dietRepository.updateSlotInstructions(dietId, slotKey, instructions)
                                }

                                val itemsJson = mealJson.optJSONArray("items")
                                if (itemsJson != null) {
                                    for (j in 0 until itemsJson.length()) {
                                        try {
                                            val itemJson = itemsJson.getJSONObject(j)
                                            val foodName = itemJson.optString("foodName", "")
                                            val quantity = itemJson.optDouble("quantity", 100.0)
                                            val unitStr = itemJson.optString("unit", "GRAM")
                                            val unit = FoodUnit.fromString(unitStr)

                                            if (foodName.isNotBlank()) {
                                                // Search by name (first match); skip if not found
                                                val food = foodRepository.searchFoods(foodName)
                                                    .firstOrNull()
                                                    ?.firstOrNull { it.name.equals(foodName, ignoreCase = true) }
                                                if (food != null) {
                                                    mealRepository.addFoodToMeal(mealId, food.id, quantity, unit)
                                                }
                                            }
                                        } catch (_: Exception) { /* skip bad item */ }
                                    }
                                }
                            } catch (_: Exception) { skipped++ }
                        }
                        imported++
                    } catch (_: Exception) { skipped++ }
                }

                _importResult.value = "Imported $imported diet(s)${if (skipped > 0) ", $skipped skipped" else ""}"
            } catch (e: Exception) {
                _importResult.value = "Error: ${e.message}"
            }
        }
    }

    // ── Foods ─────────────────────────────────────────────────────────────────

    fun importFoodsFromJson(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText()
                    ?: run { _importResult.value = "Failed to read file"; return@launch }

                val array = JSONArray(json)
                var imported = 0; var skipped = 0
                for (i in 0 until array.length()) {
                    try {
                        val f = array.getJSONObject(i)
                        val name = f.optString("name", "")
                        if (name.isBlank()) { skipped++; continue }
                        val food = FoodItem(
                            name = name,
                            caloriesPer100 = f.optDouble("caloriesPer100", 0.0),
                            proteinPer100 = f.optDouble("proteinPer100", 0.0),
                            carbsPer100 = f.optDouble("carbsPer100", 0.0),
                            fatPer100 = f.optDouble("fatPer100", 0.0),
                            gramsPerPiece = if (f.isNull("gramsPerPiece")) null else f.optDouble("gramsPerPiece"),
                            gramsPerCup = if (f.isNull("gramsPerCup")) null else f.optDouble("gramsPerCup"),
                            gramsPerTbsp = if (f.isNull("gramsPerTbsp")) null else f.optDouble("gramsPerTbsp"),
                            gramsPerTsp = if (f.isNull("gramsPerTsp")) null else f.optDouble("gramsPerTsp"),
                            glycemicIndex = if (f.isNull("glycemicIndex")) null else f.optInt("glycemicIndex"),
                            isSystemFood = true
                        )
                        foodRepository.upsertSystemFood(food)
                        imported++
                    } catch (_: Exception) { skipped++ }
                }
                _importResult.value = "Imported $imported food(s)${if (skipped > 0) ", $skipped skipped" else ""}"
            } catch (e: Exception) {
                _importResult.value = "Error: ${e.message}"
            }
        }
    }

    // ── Exercises ─────────────────────────────────────────────────────────────

    fun importExercisesFromJson(uri: Uri) {
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText()
                    ?: run { _importResult.value = "Failed to read file"; return@launch }

                val array = JSONArray(json)
                var imported = 0; var skipped = 0
                val exercises = mutableListOf<Exercise>()
                for (i in 0 until array.length()) {
                    try {
                        val e = array.getJSONObject(i)
                        val name = e.optString("name", "")
                        if (name.isBlank()) { skipped++; continue }
                        val category = runCatching {
                            ExerciseCategory.valueOf(e.optString("category", "OTHER"))
                        }.getOrDefault(ExerciseCategory.OTHER)
                        exercises.add(Exercise(
                            name = name,
                            category = category,
                            muscleGroup = e.optString("muscleGroup", null).ifNullOrBlank(),
                            equipment = e.optString("equipment", null).ifNullOrBlank(),
                            isSystem = true
                        ))
                        imported++
                    } catch (_: Exception) { skipped++ }
                }
                workoutRepository.upsertSystemExercises(exercises)
                _importResult.value = "Imported $imported exercise(s)${if (skipped > 0) ", $skipped skipped" else ""}"
            } catch (e: Exception) {
                _importResult.value = "Error: ${e.message}"
            }
        }
    }

    // ── Workout Templates ─────────────────────────────────────────────────────

    fun importWorkoutTemplatesFromJson(uri: Uri) {
        val firebaseUid: String = _uiState.value.user?.id
            ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: run { _importResult.value = "Not logged in"; return }
        viewModelScope.launch {
            try {
                val json = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText()
                    ?: run { _importResult.value = "Failed to read file"; return@launch }

                val root = JSONObject(json)
                val templates = root.optJSONArray("templates")
                    ?: run { _importResult.value = "Invalid format: missing 'templates' array"; return@launch }

                var imported = 0; var skipped = 0
                for (i in 0 until templates.length()) {
                    try {
                        val t = templates.getJSONObject(i)
                        val name = t.optString("name", "")
                        if (name.isBlank()) { skipped++; continue }
                        val category = runCatching {
                            WorkoutTemplateCategory.valueOf(t.optString("category", "STRENGTH"))
                        }.getOrDefault(WorkoutTemplateCategory.STRENGTH)

                        val template = WorkoutTemplate(
                            userId = firebaseUid,
                            name = name,
                            category = category,
                            notes = t.optString("notes", null).ifNullOrBlank()
                        )
                        val templateId = workoutRepository.insertTemplate(template)

                        val exercisesArr = t.optJSONArray("exercises")
                        if (exercisesArr != null) {
                            // ── Step 1: insert WorkoutTemplateExercise rows ──
                            val templateExercises = mutableListOf<WorkoutTemplateExercise>()
                            for (j in 0 until exercisesArr.length()) {
                                val ex = exercisesArr.getJSONObject(j)
                                val exName = ex.optString("name", "")
                                if (exName.isBlank()) continue
                                val exercise = workoutRepository.getExerciseByName(exName) ?: continue

                                val setsArr = ex.optJSONArray("sets")
                                val totalSets: Int
                                val firstReps: Int?
                                if (setsArr != null && setsArr.length() > 0) {
                                    totalSets = (0 until setsArr.length()).sumOf { k ->
                                        setsArr.getJSONObject(k).optInt("count", 1)
                                    }
                                    firstReps = setsArr.getJSONObject(0).optInt("reps", 0).takeIf { it > 0 }
                                } else {
                                    totalSets = ex.optInt("sets", 3).coerceAtLeast(1)
                                    firstReps = ex.optInt("reps", 0).takeIf { it > 0 }
                                }

                                templateExercises.add(
                                    WorkoutTemplateExercise(
                                        templateId = templateId,
                                        exerciseId = exercise.id,
                                        orderIndex = j,
                                        targetSets = totalSets,
                                        targetReps = firstReps
                                    )
                                )
                            }

                            if (templateExercises.isNotEmpty()) {
                                workoutRepository.upsertTemplateExercises(templateExercises)

                                // ── Step 2: re-fetch to get DB-assigned IDs ──
                                val inserted = workoutRepository.getTemplateWithExercises(templateId)
                                if (inserted != null) {
                                    // Sort by orderIndex to match JSON order
                                    val sortedExercises = inserted.exercises
                                        .sortedBy { it.templateExercise.orderIndex }

                                    val allSets = mutableListOf<WorkoutTemplateSet>()
                                    sortedExercises.forEachIndexed { idx, exWithDetails ->
                                        val exJson = exercisesArr.optJSONObject(idx) ?: return@forEachIndexed
                                        val setsArr = exJson.optJSONArray("sets") ?: return@forEachIndexed
                                        var setIndex = 0
                                        for (k in 0 until setsArr.length()) {
                                            val setObj = setsArr.getJSONObject(k)
                                            val reps = setObj.optInt("reps", 0).takeIf { it > 0 }
                                            val weightKg = setObj.optDouble("weight", Double.NaN)
                                                .takeIf { !it.isNaN() }
                                            val count = setObj.optInt("count", 1).coerceAtLeast(1)
                                            repeat(count) {
                                                allSets.add(
                                                    WorkoutTemplateSet(
                                                        templateExerciseId = exWithDetails.templateExercise.id,
                                                        setIndex = setIndex++,
                                                        reps = reps,
                                                        weightKg = weightKg
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    if (allSets.isNotEmpty()) {
                                        workoutRepository.insertTemplateSets(allSets)
                                    }
                                }
                            }
                        }
                        imported++
                    } catch (_: Exception) { skipped++ }
                }
                _importResult.value = "Imported $imported template(s)${if (skipped > 0) ", $skipped skipped" else ""}"
            } catch (e: Exception) {
                _importResult.value = "Error: ${e.message}"
            }
        }
    }

    // ── Backup restore ────────────────────────────────────────────────────────

    fun importBackupFromJson(uri: Uri) {
        viewModelScope.launch {
            _importResult.value = null
            val result = backupDataImporter.importFromUri(context, uri)
            _importResult.value = result
        }
    }

    fun clearImportResult() { _importResult.value = null }

    private fun String?.ifNullOrBlank(): String? = if (isNullOrBlank()) null else this
}
