package com.mealplanplus.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import com.mealplanplus.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps Firebase Auth. The backend accepts any valid Firebase ID token as a Bearer
 * credential, so this repo only needs to establish a signed-in FirebaseUser — the
 * token injection already lives in NetworkModule's OkHttp interceptor.
 */
@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val appContext: Context,
) {
    /** Emits the current user (or null) and every subsequent sign-in/out change. */
    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    val currentUser: FirebaseUser? get() = auth.currentUser

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    /** Create the account, then set the Firebase display name if a [name] was provided. */
    suspend fun register(email: String, password: String, name: String = "") {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            result.user?.updateProfile(userProfileChangeRequest { displayName = trimmed })?.await()
        }
    }

    /** Send a Firebase password-reset email. Succeeds silently even if the address is unknown. */
    suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email.trim()).await()
    }

    /**
     * Google sign-in via Credential Manager. Needs an Activity context to show the
     * account picker; [serverClientId] is the web client from google-services.json.
     */
    suspend fun signInWithGoogle(activityContext: Context) {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(appContext.getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = CredentialManager.create(activityContext).getCredential(activityContext, request)
        val googleCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleCredential.idToken, null)
        auth.signInWithCredential(firebaseCredential).await()
    }

    fun signOut() = auth.signOut()

    /**
     * Delete this device's Firebase auth account (account-deletion option A). May throw
     * FirebaseAuthRecentLoginRequiredException if the sign-in is stale — callers treat that as
     * non-fatal since the server-side data is already erased.
     */
    suspend fun deleteCurrentUser() {
        auth.currentUser?.delete()?.await()
    }
}
