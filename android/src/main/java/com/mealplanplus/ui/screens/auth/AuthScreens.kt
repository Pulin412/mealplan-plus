package com.mealplanplus.ui.screens.auth

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.AppBg
import com.mealplanplus.ui.theme.CardBorder
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.MutedDark
import androidx.compose.material3.OutlinedTextFieldDefaults
import com.mealplanplus.ui.theme.Muted
import com.mealplanplus.ui.theme.OnAccent
import com.mealplanplus.ui.theme.Surface
import com.mealplanplus.ui.theme.Teal

private enum class AuthMode { LOGIN, REGISTER }

/**
 * Login / Register — matches the webapp (`AuthForm.tsx`): a plain "MealPlan+" wordmark, a
 * "Welcome back" / "Create account" subtitle, a card with email + password + primary CTA +
 * "Continue with Google", and a text link to switch modes. Single screen; the mode swaps the
 * subtitle, CTA and switch copy.
 */
@Composable
fun AuthScreen(vm: AuthViewModel, onForgotPassword: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val register = mode == AuthMode.REGISTER
    val enabled = email.isNotBlank() && password.length >= 6 && !ui.isLoading

    Column(
        // Edge-to-edge + rendered outside the app Scaffold → inset from the status/nav bars so
        // top/bottom controls stay tappable (see OnboardingScreen for the same fix).
        modifier = Modifier.fillMaxSize().background(AppBg).systemBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
            // Header
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MealPlan+", color = Teal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(if (register) "Create account" else "Welcome back", color = MutedDark, fontSize = 13.sp)
            }
            Spacer(Modifier.height(24.dp))

            // Card
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Surface)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledField("Email", email, { email = it }, KeyboardType.Email)
                LabeledField(
                    "Password", password, { password = it }, KeyboardType.Password,
                    visual = PasswordVisualTransformation(),
                )

                if (!register) {
                    Text(
                        "Forgot password?",
                        color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.End).clickable { onForgotPassword() },
                    )
                }

                ui.error?.let { Text(it, color = Danger, fontSize = 11.5.sp) }

                Button(
                    onClick = { if (register) vm.register(email, password, "") else vm.signIn(email, password) },
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                ) {
                    if (ui.isLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = OnAccent)
                    else Text(if (register) "Create account" else "Sign in", color = OnAccent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                OrDivider()

                OutlinedButton(
                    onClick = { vm.signInWithGoogle(context) },
                    enabled = !ui.isLoading,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                ) { Text("Continue with Google", color = Ink, fontSize = 13.sp) }
            }

            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(if (register) "Already have an account?  " else "Need an account?  ", color = MutedDark, fontSize = 12.sp)
                Text(
                    if (register) "Sign in" else "Create one",
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

    Column(Modifier.fillMaxSize().background(AppBg).systemBarsPadding()) {
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
                    LabeledField("Email", email, { email = it }, KeyboardType.Email)

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

/** A label above a bordered single-line input, matching the webapp form fields. */
@Composable
private fun LabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType,
    visual: VisualTransformation = VisualTransformation.None,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = MutedDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            singleLine = true,
            visualTransformation = visual,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Ink, unfocusedTextColor = Ink,
                focusedBorderColor = Teal, unfocusedBorderColor = MutedDark,
                cursorColor = Teal,
            ),
        )
    }
}

@Composable
private fun OrDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Divider(Modifier.weight(1f), color = CardBorder)
        Text("or", color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp))
        Divider(Modifier.weight(1f), color = CardBorder)
    }
}
