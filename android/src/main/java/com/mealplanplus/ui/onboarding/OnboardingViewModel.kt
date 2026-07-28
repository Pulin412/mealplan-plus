package com.mealplanplus.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.data.generated.api.UsersApi
import com.mealplanplus.data.generated.model.UserUpdateRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val store: OnboardingStore,
    private val usersApi: UsersApi,
) : ViewModel() {

    val done: StateFlow<Boolean> = store.done

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving

    /** Required personal details — saved before advancing. */
    fun saveDetails(
        name: String, age: Int, sex: UserUpdateRequest.Gender, heightCm: Double, weightKg: Double,
        onSaved: () -> Unit,
    ) = save(
        UserUpdateRequest(displayName = name, age = age, gender = sex, heightCm = heightCm, weightKg = weightKg),
        onSaved,
    )

    /** Optional targets. */
    fun saveTargets(
        goal: UserUpdateRequest.GoalType?, kcal: Int?, protein: Int?, carbs: Int?, fat: Int?,
        onSaved: () -> Unit,
    ) = save(
        UserUpdateRequest(goalType = goal, targetCalories = kcal, targetProtein = protein, targetCarbs = carbs, targetFat = fat),
        onSaved,
    )

    private fun save(update: UserUpdateRequest, onSaved: () -> Unit) {
        viewModelScope.launch {
            _saving.value = true
            runCatching { usersApi.updateMe(update) }   // non-fatal; editable later in Profile
            _saving.value = false
            onSaved()
        }
    }

    /** Finish / skip the whole flow → unlock the app. */
    fun complete() = store.markDone()
}
