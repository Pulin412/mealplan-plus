package com.mealplanplus.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mealplanplus.Legal
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

    // Onboarding completion is authoritative on the server (once per account, not per device). When
    // the local flag isn't set, confirm with getMe before showing onboarding to avoid flashing it on
    // a device where this account already onboarded.
    private val _checking = MutableStateFlow(!store.done.value)
    val checking: StateFlow<Boolean> = _checking

    init {
        if (!store.done.value) {
            viewModelScope.launch {
                runCatching { usersApi.getMe() }
                    .onSuccess { if (it.isSuccessful && it.body()?.onboardingCompletedAt != null) store.markDone() }
                _checking.value = false
            }
        }
    }

    /** Required personal details — saved before advancing. */
    fun saveDetails(
        name: String, age: Int, sex: UserUpdateRequest.Gender, heightCm: Double, weightKg: Double,
        onSaved: () -> Unit,
    ) = save(
        UserUpdateRequest(
            displayName = name, age = age, gender = sex, heightCm = heightCm, weightKg = weightKg,
            // Consent accepted on the Welcome step; record it with the first profile write.
            privacyPolicyVersion = Legal.PRIVACY_POLICY_VERSION,
        ),
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

    /** Finish / skip the whole flow → persist completion server-side, then unlock the app. */
    fun complete() {
        viewModelScope.launch {
            runCatching { usersApi.updateMe(UserUpdateRequest(onboardingCompleted = true)) }
            store.markDone()
        }
    }
}
