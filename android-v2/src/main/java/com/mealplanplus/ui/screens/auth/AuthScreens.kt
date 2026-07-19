package com.mealplanplus.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.Danger
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Teal

@Composable
fun LoginScreen(vm: AuthViewModel, onNavigateRegister: () -> Unit) =
    AuthForm(
        vm = vm,
        title = "Welcome back",
        cta = "Sign in",
        altPrompt = "Need an account?",
        altLabel = "Create one",
        onAlt = onNavigateRegister,
        onSubmit = vm::signIn,
    )

@Composable
fun RegisterScreen(vm: AuthViewModel, onNavigateLogin: () -> Unit) =
    AuthForm(
        vm = vm,
        title = "Create account",
        cta = "Create account",
        altPrompt = "Already have an account?",
        altLabel = "Sign in",
        onAlt = onNavigateLogin,
        onSubmit = vm::register,
    )

@Composable
private fun AuthForm(
    vm: AuthViewModel,
    title: String,
    cta: String,
    altPrompt: String,
    altLabel: String,
    onAlt: () -> Unit,
    onSubmit: (String, String) -> Unit,
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val enabled = email.isNotBlank() && password.length >= 6 && !ui.isLoading

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(Modifier.widthIn(max = 360.dp).fillMaxWidth()) {
            Text("MealPlan+", color = Teal, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Ink, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("Email") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("Password") }, singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            ui.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Danger, fontSize = 12.sp)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSubmit(email, password) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Teal),
            ) {
                if (ui.isLoading) CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                else Text(cta)
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { vm.signInWithGoogle(context) },
                enabled = !ui.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Continue with Google", color = Ink) }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAlt, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("$altPrompt  ", color = Ink, fontSize = 12.sp)
                Text(altLabel, color = Teal, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
