package com.mealplanplus.data.repository

import android.content.Context
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.mealplanplus.data.local.UserDao
import com.mealplanplus.data.model.User
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val userDao: UserDao,
    @ApplicationContext private val context: Context
) {
    val currentFirebaseUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    fun isLoggedIn(): Flow<Boolean> = AuthPreferences.isLoggedIn(context)

    fun getCurrentUserId(): Flow<String?> = AuthPreferences.getUserId(context)

    fun getCurrentUser(userId: String): Flow<User?> = userDao.getUserById(userId)

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign in failed")
            val user = saveUserLocally(firebaseUser)
            AuthPreferences.setLoggedIn(context, user.id)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign up failed")

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val user = saveUserLocally(firebaseUser, displayName = name)
            AuthPreferences.setLoggedIn(context, user.id)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(credential: AuthCredential): Result<User> {
        return try {
            val result = firebaseAuth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Google sign in failed")
            val user = saveUserLocally(firebaseUser)
            AuthPreferences.setLoggedIn(context, user.id)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        AuthPreferences.clearAuth(context)
    }

    suspend fun updateProfile(user: User): Result<User> {
        return try {
            val updatedUser = user.copy(updatedAt = System.currentTimeMillis())
            userDao.updateUser(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun saveUserLocally(
        firebaseUser: FirebaseUser,
        displayName: String? = null
    ): User {
        val existingUser = userDao.getUserByIdSync(firebaseUser.uid)
        val user = User(
            id = firebaseUser.uid,
            email = firebaseUser.email ?: "",
            displayName = displayName ?: firebaseUser.displayName ?: existingUser?.displayName,
            photoUrl = firebaseUser.photoUrl?.toString() ?: existingUser?.photoUrl,
            age = existingUser?.age,
            contact = existingUser?.contact,
            createdAt = existingUser?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        userDao.insertUser(user)
        return user
    }
}
