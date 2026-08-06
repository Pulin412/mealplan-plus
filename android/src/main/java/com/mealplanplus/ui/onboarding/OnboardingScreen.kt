package com.mealplanplus.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.Legal
import com.mealplanplus.ui.components.Stepper
import com.mealplanplus.data.generated.model.UserUpdateRequest
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal

/**
 * Blocking first-run onboarding (shown before the app + bottom nav):
 *   0 Welcome → 1 Personal details (REQUIRED) → 2 Targets (skippable) → 3 Tips.
 * Personal details can't be skipped/left empty; the global "Skip" only appears past that step.
 * Completing/skipping calls [OnboardingViewModel.complete], which unlocks the app.
 */
@Composable
fun OnboardingScreen(vm: OnboardingViewModel = hiltViewModel()) {
    val saving by vm.saving.collectAsState()
    var step by remember { mutableStateOf(0) }
    var consented by remember { mutableStateOf(false) }

    // required details
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf<UserUpdateRequest.Gender?>(null) }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }

    // optional targets
    var goal by remember { mutableStateOf<UserUpdateRequest.GoalType?>(null) }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    val detailsValid = name.isNotBlank() && (age.toIntOrNull() ?: 0) > 0 && sex != null &&
        (height.toDoubleOrNull() ?: 0.0) > 0 && (weight.toDoubleOrNull() ?: 0.0) > 0

    // Onboarding renders outside the app's Scaffold (NavGraph), and the app is edge-to-edge, so the
    // content must inset itself away from the status/nav bars — otherwise the top "Skip" hides under
    // the status bar and the bottom "Skip for now" hides under the navigation bar (untappable on
    // devices with a 3-button nav bar / smaller screens). background() stays edge-to-edge; only the
    // content is inset — matching what Scaffold does for the rest of the app.
    Column(Modifier.fillMaxSize().background(AppBg).systemBarsPadding().padding(horizontal = 24.dp)) {
        // top bar: progress dots + global skip (only past the required step)
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(4) { i ->
                Spacer(
                    Modifier.padding(end = 6.dp).height(6.dp).width(if (i == step) 20.dp else 6.dp)
                        .clip(RoundedCornerShape(3.dp)).background(if (i == step) Teal else BorderCool),
                )
            }
            Spacer(Modifier.weight(1f))
            if (step >= 2) {
                Text("Skip", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { vm.complete() })
            }
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
        ) {
            when (step) {
                0 -> WelcomeStep(consented) { consented = it }
                1 -> DetailsStep(name, { name = it }, age, { age = it }, sex, { sex = it }, height, { height = it }, weight, { weight = it })
                2 -> TargetsStep(goal, { goal = it }, kcal, { kcal = it }, protein, { protein = it }, carbs, { carbs = it }, fat, { fat = it })
                else -> TipsStep()
            }
        }

        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            when (step) {
                0 -> PrimaryButton("Get started", enabled = consented) { step = 1 }
                1 -> PrimaryButton("Continue", enabled = detailsValid && !saving, loading = saving) {
                    vm.saveDetails(name.trim(), age.toInt(), sex!!, height.toDouble(), weight.toDouble()) { step = 2 }
                }
                2 -> {
                    PrimaryButton("Save & continue", enabled = !saving, loading = saving) {
                        vm.saveTargets(goal, kcal.toIntOrNull(), protein.toIntOrNull(), carbs.toIntOrNull(), fat.toIntOrNull()) { step = 3 }
                    }
                    Text("Skip for now", color = Muted, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable { step = 3 },
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
                else -> PrimaryButton("Start using MealPlan+") { vm.complete() }
            }
        }
    }
}

@Composable
private fun WelcomeStep(consented: Boolean, onConsent: (Boolean) -> Unit) {
    val ctx = LocalContext.current
    Column {
        Column(Modifier.fillMaxWidth().padding(bottom = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Text("MealPlan", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("+", color = Teal, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text("Let's get you set up — takes a minute.", color = MutedDark, fontSize = 13.sp)
        }
        listOf(
            "🍽️" to "Log meals by slot",
            "📋" to "Plan diets & groceries",
            "💪" to "Workouts & health",
        ).forEach { (icon, title) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(icon, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Text(title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        // Consent gate — must accept before continuing (health data is stored).
        Row(Modifier.fillMaxWidth().padding(top = 28.dp), verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = consented, onCheckedChange = onConsent,
                colors = CheckboxDefaults.colors(checkedColor = Teal),
            )
            Spacer(Modifier.width(4.dp))
            Column(Modifier.padding(top = 12.dp)) {
                Text(
                    "I agree to the Privacy Policy and consent to my health data being stored to provide the app.",
                    color = MutedDark, fontSize = 12.sp, lineHeight = 16.sp,
                )
                Text(
                    "Read the Privacy Policy", color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp).clickable {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Legal.PRIVACY_POLICY_URL)))
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailsStep(
    name: String, onName: (String) -> Unit, age: String, onAge: (String) -> Unit,
    sex: UserUpdateRequest.Gender?, onSex: (UserUpdateRequest.Gender) -> Unit,
    height: String, onHeight: (String) -> Unit, weight: String, onWeight: (String) -> Unit,
) {
    Column {
        Text("About you", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("We use this to personalize your targets & health tracking. All fields required.",
            color = MutedDark, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))

        Field("Name", name, onName, numeric = false)
        Spacer(Modifier.height(4.dp))
        Text("Sex", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp, top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(UserUpdateRequest.Gender.MALE to "Male", UserUpdateRequest.Gender.FEMALE to "Female", UserUpdateRequest.Gender.OTHER to "Other")
                .forEach { (v, label) -> Chip(label, sex == v, Modifier.weight(1f)) { onSex(v) } }
        }
        Spacer(Modifier.height(12.dp))
        // Plain stacked numeric fields (like the login form) — the old horizontal ruler picker
        // overflowed the screen on smaller devices.
        Field("Age (years)", age, onAge)
        Field("Height (cm)", height, onHeight)
        Field("Weight (kg)", weight, onWeight)
    }
}

@Composable
private fun StepperRow(label: String, value: String, onChange: (String) -> Unit, step: Int, suffix: String) {
    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Stepper(value.toIntOrNull() ?: 0, { onChange(it.toString()) }, step = step, suffix = suffix, modifier = Modifier.width(170.dp))
    }
}

@Composable
private fun TargetsStep(
    goal: UserUpdateRequest.GoalType?, onGoal: (UserUpdateRequest.GoalType) -> Unit,
    kcal: String, onKcal: (String) -> Unit, protein: String, onProtein: (String) -> Unit,
    carbs: String, onCarbs: (String) -> Unit, fat: String, onFat: (String) -> Unit,
) {
    Column {
        Text("Your targets", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Sets your daily goal on the Today ring. Optional — change anytime in Profile.",
            color = MutedDark, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp, bottom = 16.dp))
        Text("Goal", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(UserUpdateRequest.GoalType.LOSE to "Lose", UserUpdateRequest.GoalType.MAINTAIN to "Maintain", UserUpdateRequest.GoalType.GAIN to "Gain")
                .forEach { (v, label) -> Chip(label, goal == v, Modifier.weight(1f)) { onGoal(v) } }
        }
        Spacer(Modifier.height(4.dp))
        StepperRow("Calories", kcal, onKcal, 10, "kcal")
        StepperRow("Protein", protein, onProtein, 5, "g")
        StepperRow("Carbs", carbs, onCarbs, 5, "g")
        StepperRow("Fat", fat, onFat, 5, "g")
    }
}

@Composable
private fun TipsStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("🎉", fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text("You're all set", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("A couple of tips to get going:", color = MutedDark, fontSize = 12.5.sp, modifier = Modifier.padding(top = 2.dp, bottom = 18.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                "Tap a slot on Today to log a meal.",
                "Foods, Meals & Diets live under the More tab.",
                "You already have 100+ foods ready to search.",
            ).forEach { tip ->
                Row {
                    Text("✓", color = Teal, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(tip, color = MutedDark, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier.fillMaxWidth(), numeric: Boolean = true) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Text(label, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = { if (numeric) onValueChange(it.filter(Char::isDigit)) else onValueChange(it) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Teal, unfocusedBorderColor = CardBorder),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        color = if (selected) OnAccent else Ink,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Teal else Surface)
            .border(1.dp, if (selected) Teal else BorderCool, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun PrimaryButton(label: String, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Teal),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnAccent)
        else Text(label, color = OnAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
