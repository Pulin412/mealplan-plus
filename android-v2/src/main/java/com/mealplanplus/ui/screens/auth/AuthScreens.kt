package com.mealplanplus.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.BorderCool
import com.mealplanplus.ui.theme.Carbs
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.Fat
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Protein
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal

private enum class AuthMode { LOGIN, REGISTER }

/**
 * Screen A — Login / Register (design_v2 prototype). One screen with a segmented Log in /
 * Register toggle, the "macro plate" logo, email + password (show/hide), a name field on
 * Register, a Forgot-password link, and Continue with Google. The submit label and switch copy
 * swap with the mode.
 */
@Composable
fun AuthScreen(vm: AuthViewModel, onForgotPassword: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val register = mode == AuthMode.REGISTER
    val enabled = email.isNotBlank() && password.length >= 6 &&
        (!register || name.isNotBlank()) && !ui.isLoading

    Column(
        modifier = Modifier.fillMaxSize().background(AppBg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            Modifier.widthIn(max = 360.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MacroPlateLogo()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MealPlan", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("+", color = Teal, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))

            AuthModeToggle(mode = mode, onSelect = { mode = it; vm.clearError() })
            Spacer(Modifier.height(16.dp))

            if (register) {
                AuthField(value = name, onValueChange = { name = it }, label = "Name",
                    keyboardType = KeyboardType.Text)
                Spacer(Modifier.height(10.dp))
            }
            AuthField(value = email, onValueChange = { email = it }, label = "Email",
                keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(10.dp))
            AuthField(
                value = password, onValueChange = { password = it }, label = "Password",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Hide password" else "Show password",
                            tint = Muted,
                        )
                    }
                },
            )

            if (!register) {
                TextButton(
                    onClick = onForgotPassword,
                    modifier = Modifier.align(Alignment.End),
                ) { Text("Forgot password?", color = Teal, fontSize = 12.sp) }
            }

            ui.error?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = Danger, fontSize = 12.sp, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(if (register) 16.dp else 8.dp))
            Button(
                onClick = {
                    if (register) vm.register(email, password, name) else vm.signIn(email, password)
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
            ) {
                if (ui.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnAccent)
                else Text(if (register) "Register" else "Log in", color = OnAccent, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(14.dp))
            OrDivider()
            Spacer(Modifier.height(14.dp))

            OutlinedButton(
                onClick = { vm.signInWithGoogle(context) },
                enabled = !ui.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(10.dp),
            ) { Text("Continue with Google", color = Ink) }

            Spacer(Modifier.height(16.dp))
            Text(
                "By continuing you agree to our Terms & Privacy Policy.",
                color = Muted, fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    if (register) "Already have an account?  " else "Need an account?  ",
                    color = Muted, fontSize = 12.sp,
                )
                Text(
                    if (register) "Log in" else "Register",
                    color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        mode = if (register) AuthMode.LOGIN else AuthMode.REGISTER
                        vm.clearError()
                    },
                )
            }
        }
    }
}

/**
 * Forgot-password flow: enter an email → send a Firebase reset link → "check your inbox"
 * confirmation. [onBack] returns to [AuthScreen].
 */
@Composable
fun ForgotPasswordScreen(vm: AuthViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    var email by remember { mutableStateOf("") }
    val sent = ui.resetSentTo

    Column(Modifier.fillMaxSize().background(AppBg)) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { vm.resetUiState(); onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Ink)
            }
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
                if (sent == null) {
                    Text("Reset password", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Enter the email for your account and we'll send you a secure link to set a new password.",
                        color = Muted, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    AuthField(value = email, onValueChange = { email = it }, label = "Email",
                        keyboardType = KeyboardType.Email)

                    ui.error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, color = Danger, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { vm.sendPasswordReset(email) },
                        enabled = email.isNotBlank() && !ui.isLoading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) {
                        if (ui.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnAccent)
                        else Text("Send reset link", color = OnAccent, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Text("Check your inbox", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "We sent a password reset link to $sent. Open it on this device to set a new password.",
                        color = Muted, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = { vm.resetUiState(); onBack() },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    ) { Text("Back to log in", color = OnAccent, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

// ── Building blocks ───────────────────────────────────────────────────────────

/** The "macro plate": a 56dp ring split into protein/carbs/fat arcs around a teal `+`. */
@Composable
private fun MacroPlateLogo() {
    Box(
        modifier = Modifier.size(56.dp).shadow(6.dp, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        // -90° rotation puts the first (protein) arc at the top, matching the prototype.
        Box(
            Modifier.fillMaxSize().rotate(-90f).clip(CircleShape).background(
                Brush.sweepGradient(
                    0.00f to Protein, 0.34f to Protein,
                    0.34f to Carbs,   0.67f to Carbs,
                    0.67f to Fat,     1.00f to Fat,
                )
            )
        )
        Box(Modifier.size(36.dp).clip(CircleShape).background(Surface), contentAlignment = Alignment.Center) {
            Text("+", color = Teal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Full-width two-segment Log in / Register toggle (Ink-filled active segment). */
@Composable
private fun AuthModeToggle(mode: AuthMode, onSelect: (AuthMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).border(1.dp, BorderCool, RoundedCornerShape(9.dp)),
    ) {
        AuthMode.values().forEach { m ->
            val selected = m == mode
            val interaction = remember { MutableInteractionSource() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f).height(36.dp)
                    .background(if (selected) Ink else Surface)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(m) },
            ) {
                Text(
                    if (m == AuthMode.LOGIN) "Log in" else "Register",
                    color = if (selected) Surface else Muted,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label) }, singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailing,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Divider(Modifier.weight(1f), color = CardBorder)
        Text("or", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp))
        Divider(Modifier.weight(1f), color = CardBorder)
    }
}
