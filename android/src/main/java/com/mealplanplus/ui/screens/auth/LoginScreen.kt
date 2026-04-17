package com.mealplanplus.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.mealplanplus.BuildConfig
import com.mealplanplus.R
import com.mealplanplus.ui.theme.BrandGreen
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoginSuccess()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Green header ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandGreen)
                    .padding(start = 8.dp, end = 22.dp, top = 36.dp, bottom = 22.dp)
            ) {
                Column {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = "Welcome back 👋",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Sign in to continue to MealPlan+",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                }
            }

            // ── Form body ──────────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 22.dp)
            ) {
                // Email
                FieldLabel("Email")
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it; viewModel.clearError() },
                    placeholder = { Text("you@example.com", color = Color(0xFFBBBBBB)) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFFBBBBBB))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(11.dp),
                    colors = authFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password
                FieldLabel("Password")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; viewModel.clearError() },
                    placeholder = { Text("••••••••", color = Color(0xFFBBBBBB)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFBBBBBB))
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color(0xFFBBBBBB)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    isError = uiState.error != null,
                    supportingText = uiState.error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    shape = RoundedCornerShape(11.dp),
                    colors = authFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // Forgot password
                Box(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Text(
                            text = "Forgot Password?",
                            color = BrandGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Sign In button
                Button(
                    onClick = { viewModel.signIn(email, password) },
                    enabled = !uiState.isLoading && email.isNotBlank() && password.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(13.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Sign In →", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // OR divider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "  or  ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google button
                if (BuildConfig.OAUTH_GOOGLE_ANDROID_ENABLED) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.clearError()
                                val activity = context as? Activity ?: run {
                                    viewModel.setOAuthError("Google sign-in requires an active screen context.")
                                    return@launch
                                }
                                val webClientId = context.getString(R.string.google_web_client_id).trim()
                                if (webClientId.isBlank() || webClientId.contains("REPLACE")) {
                                    viewModel.setOAuthError("Google OAuth is not configured. Set google_web_client_id first.")
                                    return@launch
                                }
                                try {
                                    val credentialManager = CredentialManager.create(context)
                                    val request = GetCredentialRequest.Builder()
                                        .addCredentialOption(
                                            GetGoogleIdOption.Builder()
                                                .setServerClientId(webClientId)
                                                .setFilterByAuthorizedAccounts(false)
                                                .setAutoSelectEnabled(false)
                                                .build()
                                        )
                                        .build()
                                    val result = credentialManager.getCredential(context = activity, request = request)
                                    val credential = result.credential
                                    if (credential is CustomCredential &&
                                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                    ) {
                                        try {
                                            viewModel.signInWithGoogle(
                                                GoogleIdTokenCredential.createFrom(credential.data).idToken
                                            )
                                        } catch (e: GoogleIdTokenParsingException) {
                                            viewModel.setOAuthError("Invalid Google token response.")
                                        }
                                    } else {
                                        viewModel.setOAuthError("Google sign-in did not return a valid credential.")
                                    }
                                } catch (e: Exception) {
                                    val msg = e.message ?: "Google sign-in failed"
                                    viewModel.setOAuthError(
                                        if (msg.contains("canceled", ignoreCase = true))
                                            "Google sign-in was canceled."
                                        else
                                            "Google sign-in unavailable: $msg"
                                    )
                                }
                            }
                        },
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(13.dp)
                    ) {
                        Text("G", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Continue with Google", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Sign-up link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Don't have an account?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onNavigateToSignUp) {
                        Text(
                            text = "Create one →",
                            color = BrandGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "© 2026 Pulin. All rights reserved.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
internal fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF999999),
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(bottom = 5.dp)
    )
}

@Composable
internal fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color(0xFF111111),
    unfocusedTextColor = Color(0xFF111111),
    focusedBorderColor = Color(0xFF111111),
    unfocusedBorderColor = Color(0xFFEBEBEB),
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color(0xFFFAFAFA),
    focusedPlaceholderColor = Color(0xFFBBBBBB),
    unfocusedPlaceholderColor = Color(0xFFBBBBBB),
    focusedLeadingIconColor = Color(0xFF888888),
    unfocusedLeadingIconColor = Color(0xFFBBBBBB),
    focusedTrailingIconColor = Color(0xFF888888),
    unfocusedTrailingIconColor = Color(0xFFBBBBBB),
)
