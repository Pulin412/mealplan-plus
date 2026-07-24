package com.mealplanplus.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mealplanplus.data.generated.model.UserResponse
import com.mealplanplus.data.generated.model.UserUpdateRequest
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.DmMono
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import com.mealplanplus.ui.theme.MutedFaint
import com.mealplanplus.ui.theme.MutedLight
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.SurfaceMuted
import com.mealplanplus.ui.theme.Teal
import kotlin.math.roundToInt

private sealed interface Editor {
    val label: String
    data class Num(override val label: String, val value: String, val hint: String, val keyboard: KeyboardType, val onSave: (String) -> Unit) : Editor
    data class Opt(override val label: String, val current: String, val options: List<Pair<String, String>>, val onSave: (String) -> Unit) : Editor
    /** Weight, edited in the current unit (kg or lb), saved as kg. */
    data class Weight(val kg: Double?, val imperial: Boolean, val onSaveKg: (Double?) -> Unit) : Editor {
        override val label get() = if (imperial) "Weight (lb)" else "Weight (kg)"
    }
    /** Height, edited in the current unit (cm, or ft + in), saved as cm. */
    data class Height(val cm: Double?, val imperial: Boolean, val onSaveCm: (Double?) -> Unit) : Editor {
        override val label get() = if (imperial) "Height (ft / in)" else "Height (cm)"
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit = {}) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    var editor by remember { mutableStateOf<Editor?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Text("Profile", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
        when {
            state.loading && state.user == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Loading…", fontSize = 13.sp, color = MutedLight) }
            state.user == null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(state.error ?: "Couldn't load profile", fontSize = 13.sp, color = MutedLight) }
            else -> {
                val u = state.user!!
                val imperial = u.units == UserResponse.Units.IMPERIAL
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    IdentityHeader(u)
                    Spacer(Modifier.height(16.dp))

                    Section("Body") {
                        Row2("Name", u.displayName ?: "—") { editor = Editor.Num("Name", u.displayName ?: "", "Your name", KeyboardType.Text) { v -> viewModel.patch(UserUpdateRequest(displayName = v.ifBlank { null })) } }
                        Row2("Height", heightDisplay(u.heightCm, imperial)) { editor = Editor.Height(u.heightCm, imperial) { cm -> viewModel.patch(UserUpdateRequest(heightCm = cm)) } }
                        Row2("Weight", weightDisplay(u.weightKg, imperial)) { editor = Editor.Weight(u.weightKg, imperial) { kg -> viewModel.patch(UserUpdateRequest(weightKg = kg)) } }
                        Row2("Age", u.age?.toString() ?: "—") { editor = Editor.Num("Age", u.age?.toString() ?: "", "years", KeyboardType.Number) { v -> viewModel.patch(UserUpdateRequest(age = v.toIntOrNull())) } }
                        Row2("Sex", genderLabel(u.gender)) { editor = Editor.Opt("Sex", u.gender?.value ?: "", GENDERS) { v -> viewModel.patch(UserUpdateRequest(gender = UserUpdateRequest.Gender.entries.firstOrNull { it.value == v })) } }
                        Row2("Activity", activityLabel(u.activityLevel), last = true) { editor = Editor.Opt("Activity level", u.activityLevel?.value ?: "", ACTIVITIES) { v -> viewModel.patch(UserUpdateRequest(activityLevel = UserUpdateRequest.ActivityLevel.entries.firstOrNull { it.value == v })) } }
                    }

                    Section("Goal & targets") {
                        Row2("Goal", goalLabel(u.goalType)) { editor = Editor.Opt("Goal", u.goalType?.value ?: "", GOALS) { v -> viewModel.patch(UserUpdateRequest(goalType = UserUpdateRequest.GoalType.entries.firstOrNull { it.value == v })) } }
                        Row2("Calorie target", u.targetCalories?.let { "$it kcal" } ?: "—") { editor = Editor.Num("Calorie target (kcal)", u.targetCalories?.toString() ?: "", "e.g. 2200", KeyboardType.Number) { v -> viewModel.patch(UserUpdateRequest(targetCalories = v.toIntOrNull())) } }
                        Row2("Protein", u.targetProtein?.let { "$it g" } ?: "—") { editor = Editor.Num("Protein target (g)", u.targetProtein?.toString() ?: "", "g", KeyboardType.Number) { v -> viewModel.patch(UserUpdateRequest(targetProtein = v.toIntOrNull())) } }
                        Row2("Carbs", u.targetCarbs?.let { "$it g" } ?: "—") { editor = Editor.Num("Carbs target (g)", u.targetCarbs?.toString() ?: "", "g", KeyboardType.Number) { v -> viewModel.patch(UserUpdateRequest(targetCarbs = v.toIntOrNull())) } }
                        Row2("Fat", u.targetFat?.let { "$it g" } ?: "—", last = true) { editor = Editor.Num("Fat target (g)", u.targetFat?.toString() ?: "", "g", KeyboardType.Number) { v -> viewModel.patch(UserUpdateRequest(targetFat = v.toIntOrNull())) } }
                    }

                    Section("Energy") {
                        val bmr = bmr(u)
                        val tdee = bmr?.let { it * activityFactor(u.activityLevel) }
                        ReadRow("BMR", bmr?.let { "${it.roundToInt()} kcal/day" } ?: "Add body stats")
                        ReadRow("TDEE", tdee?.let { "${it.roundToInt()} kcal/day" } ?: "—", last = true)
                    }

                    Section("Preferences") {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text("Units", fontSize = 12.5.sp, color = MutedDark, modifier = Modifier.weight(1f))
                            UnitToggle(imperial) { toImperial -> viewModel.patch(UserUpdateRequest(units = if (toImperial) UserUpdateRequest.Units.IMPERIAL else UserUpdateRequest.Units.METRIC)) }
                        }
                    }

                    Section("Account") {
                        Text("Log out", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink,
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.signOut() }.padding(horizontal = 12.dp, vertical = 13.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
                        Text("Clear all data", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Danger,
                            modifier = Modifier.fillMaxWidth().clickable { confirmClear = true }.padding(horizontal = 12.dp, vertical = 13.dp))
                    }
                    Text("Signed in as ${u.email ?: "—"}", fontSize = 10.sp, color = MutedFaint, modifier = Modifier.padding(top = 12.dp, start = 4.dp))
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    editor?.let { EditSheet(it, onClose = { editor = null }) }
    if (confirmClear) ConfirmClear(onConfirm = { confirmClear = false; viewModel.clearAllData() }, onDismiss = { confirmClear = false })
}

@Composable
private fun IdentityHeader(u: UserResponse) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(14.dp)).padding(14.dp)) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Teal), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = OnAccent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(u.displayName ?: "Set your name", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text(u.email ?: "—", fontSize = 11.sp, color = MutedLight, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MutedFaint, modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 7.dp))
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Surface).border(1.dp, CardBorder, RoundedCornerShape(12.dp))) { content() }
}

@Composable
private fun Row2(label: String, value: String, last: Boolean = false, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp)) {
        Text(label, fontSize = 12.5.sp, color = MutedDark, modifier = Modifier.weight(1f))
        Text(value, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MutedFaint, modifier = Modifier.size(18.dp).padding(start = 4.dp))
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
}

@Composable
private fun ReadRow(label: String, value: String, last: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp)) {
        Text(label, fontSize = 12.5.sp, color = MutedDark, modifier = Modifier.weight(1f))
        Text(value, fontFamily = DmMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink)
    }
    if (!last) Box(Modifier.fillMaxWidth().height(1.dp).background(SurfaceMuted))
}

@Composable
private fun UnitToggle(imperial: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(9.dp)).border(1.dp, BorderCool, RoundedCornerShape(9.dp))) {
        listOf(false to "Metric", true to "Imperial").forEach { (imp, lbl) ->
            val on = imp == imperial
            Text(lbl, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = if (on) OnAccent else MutedDark,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (on) Ink else Surface).clickable { if (!on) onChange(imp) }.padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSheet(editor: Editor, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(editor.label, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(bottom = 12.dp))
            when (editor) {
                is Editor.Num -> {
                    var text by remember { mutableStateOf(editor.value) }
                    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).border(1.5.dp, BorderCool, RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 12.dp)) {
                        if (text.isEmpty()) Text(editor.hint, fontSize = 14.sp, color = MutedLight)
                        BasicTextField(text, { text = it }, singleLine = true, cursorBrush = SolidColor(Teal),
                            keyboardOptions = KeyboardOptions(keyboardType = editor.keyboard),
                            textStyle = TextStyle(fontSize = 14.sp, color = Ink), modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Teal).clickable { editor.onSave(text.trim()); onClose() }.padding(vertical = 13.dp)) {
                        Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnAccent)
                    }
                }
                is Editor.Opt -> {
                    editor.options.forEach { (value, label) ->
                        val on = value == editor.current
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { editor.onSave(value); onClose() }.padding(vertical = 12.dp)) {
                            Text(label, fontSize = 13.5.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal, color = if (on) Teal else Ink, modifier = Modifier.weight(1f))
                            if (on) Text("✓", fontSize = 14.sp, color = Teal)
                        }
                    }
                }
                is Editor.Weight -> {
                    val initial = editor.kg?.let { if (editor.imperial) (it * 2.20462).round1() else it.trimNum() } ?: ""
                    var text by remember { mutableStateOf(initial) }
                    NumField(text, { text = it }, if (editor.imperial) "lb" else "kg")
                    Spacer(Modifier.height(14.dp))
                    SaveBtn {
                        val n = text.trim().toDoubleOrNull()
                        editor.onSaveKg(n?.let { if (editor.imperial) it / 2.20462 else it }); onClose()
                    }
                }
                is Editor.Height -> {
                    if (editor.imperial) {
                        val totalIn = editor.cm?.div(2.54) ?: 0.0
                        var ft by remember { mutableStateOf(if (editor.cm == null) "" else (totalIn / 12).toInt().toString()) }
                        var inch by remember { mutableStateOf(if (editor.cm == null) "" else (totalIn % 12).roundToInt().toString()) }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(1f)) { NumField(ft, { ft = it }, "ft") }
                            Box(Modifier.weight(1f)) { NumField(inch, { inch = it }, "in") }
                        }
                        Spacer(Modifier.height(14.dp))
                        SaveBtn {
                            val f = ft.trim().toIntOrNull(); val i = inch.trim().toIntOrNull() ?: 0
                            editor.onSaveCm(f?.let { ((it * 12 + i) * 2.54) }); onClose()
                        }
                    } else {
                        var text by remember { mutableStateOf(editor.cm?.trimNum() ?: "") }
                        NumField(text, { text = it }, "cm")
                        Spacer(Modifier.height(14.dp))
                        SaveBtn { editor.onSaveCm(text.trim().toDoubleOrNull()); onClose() }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmClear(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Clear all data?", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("This wipes the on-device cache (foods, meals, diets) and signs you out. Your data on the server is not deleted.", fontSize = 12.sp, color = MutedLight, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Danger).clickable(onClick = onConfirm).padding(vertical = 13.dp)) {
                Text("Clear & sign out", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnAccent)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clickable(onClick = onDismiss).padding(vertical = 12.dp)) {
                Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MutedDark)
            }
        }
    }
}

@Composable
private fun NumField(value: String, onChange: (String) -> Unit, suffix: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp)).border(1.5.dp, BorderCool, RoundedCornerShape(11.dp)).padding(horizontal = 12.dp, vertical = 12.dp)) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("0", fontSize = 14.sp, color = MutedLight)
            BasicTextField(value, { onChange(it.filter { c -> c.isDigit() || c == '.' }) }, singleLine = true, cursorBrush = SolidColor(Teal),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = TextStyle(fontSize = 14.sp, color = Ink), modifier = Modifier.fillMaxWidth())
        }
        Text(suffix, fontSize = 12.sp, color = MutedFaint)
    }
}

@Composable
private fun SaveBtn(onClick: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Teal).clickable(onClick = onClick).padding(vertical = 13.dp)) {
        Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = OnAccent)
    }
}

// ── labels + math ──
private val GENDERS = listOf("MALE" to "Male", "FEMALE" to "Female", "OTHER" to "Other")
private val ACTIVITIES = listOf("SEDENTARY" to "Sedentary", "LIGHT" to "Lightly active", "MODERATE" to "Moderately active", "VERY_ACTIVE" to "Very active", "EXTRA_ACTIVE" to "Extra active")
private val GOALS = listOf("LOSE" to "Lose weight", "MAINTAIN" to "Maintain", "GAIN" to "Gain")

private fun genderLabel(g: UserResponse.Gender?) = GENDERS.firstOrNull { it.first == g?.value }?.second ?: "—"
private fun activityLabel(a: UserResponse.ActivityLevel?) = ACTIVITIES.firstOrNull { it.first == a?.value }?.second ?: "—"
private fun goalLabel(g: UserResponse.GoalType?) = GOALS.firstOrNull { it.first == g?.value }?.second ?: "—"

private fun activityFactor(a: UserResponse.ActivityLevel?): Double = when (a) {
    UserResponse.ActivityLevel.SEDENTARY -> 1.2
    UserResponse.ActivityLevel.LIGHT -> 1.375
    UserResponse.ActivityLevel.MODERATE -> 1.55
    UserResponse.ActivityLevel.VERY_ACTIVE -> 1.725
    UserResponse.ActivityLevel.EXTRA_ACTIVE -> 1.9
    null -> 1.55
}

private fun bmr(u: UserResponse): Double? {
    val w = u.weightKg ?: return null; val h = u.heightCm ?: return null; val a = u.age ?: return null
    val base = 10 * w + 6.25 * h - 5 * a
    return when (u.gender) {
        UserResponse.Gender.MALE -> base + 5
        UserResponse.Gender.FEMALE -> base - 161
        else -> base - 78
    }
}

private fun weightDisplay(kg: Double?, imperial: Boolean): String {
    if (kg == null) return "—"
    return if (imperial) "${(kg * 2.20462).round1()} lb" else "${kg.trimNum()} kg"
}

private fun heightDisplay(cm: Double?, imperial: Boolean): String {
    if (cm == null) return "—"
    if (!imperial) return "${cm.trimNum()} cm"
    val totalIn = cm / 2.54
    val ft = (totalIn / 12).toInt(); val inch = (totalIn % 12).roundToInt()
    return "$ft'$inch\""
}

private fun Double.trimNum(): String = if (this % 1.0 == 0.0) toInt().toString() else "%.1f".format(this)
private fun Double.round1(): String = "%.1f".format(this)
