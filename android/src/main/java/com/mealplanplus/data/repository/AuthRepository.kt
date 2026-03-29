package com.mealplanplus.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.mealplanplus.data.local.DailyLogDao
import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.local.PlanDao
import com.mealplanplus.data.local.UserDao
import com.mealplanplus.data.local.UserDataSeeder
import com.mealplanplus.data.model.User
import com.mealplanplus.util.AnalyticsManager
import com.mealplanplus.util.AuthPreferences
import com.mealplanplus.util.CrashlyticsReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val userDataSeeder: UserDataSeeder,
    private val healthMetricDao: HealthMetricDao,
    private val dailyLogDao: DailyLogDao,
    private val planDao: PlanDao,
    private val groceryDao: GroceryDao,
    private val dietDao: DietDao,
    private val mealDao: MealDao,
    private val crashlytics: CrashlyticsReporter,
    private val analytics: AnalyticsManager,
    @ApplicationContext private val context: Context
) {
    private val TAG = "AuthRepository"

    fun isLoggedIn(): Flow<Boolean> = AuthPreferences.isLoggedIn(context)

    fun getCurrentUserId(): Flow<Long?> = AuthPreferences.getUserId(context)

    fun getCurrentUser(userId: Long): Flow<User?> = userDao.getUserById(userId)

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        val trimmedEmail = email.lowercase().trim()
        return try {
            val firebaseUser = FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(trimmedEmail, password).await().user
                ?: return Result.failure(Exception("Sign in failed"))

            val mappedUserId = AuthPreferences.getUserIdForProviderSubject(
                context, provider = "email", subject = firebaseUser.uid
            )
            val localUser = mappedUserId?.let { userDao.getUserByIdSync(it) }
                ?: userDao.getUserByEmail(trimmedEmail)
                ?: return Result.failure(Exception("Local user record not found. Please sign up again."))

            AuthPreferences.setProviderSubjectMapping(context, "email", firebaseUser.uid, localUser.id)
            AuthPreferences.setLoggedIn(context, localUser.id)
            crashlytics.setUserId(localUser.id.toString())
            crashlytics.log("sign_in", "provider=email")
            analytics.setUserId(localUser.id.toString())
            analytics.logSignIn("email")
            Result.success(localUser)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email or password"))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found with this email"))
        } catch (e: Exception) {
            crashlytics.recordNonFatal(e, context = "sign_in_email")
            Result.failure(Exception("Sign in failed: ${e.message}"))
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val firebaseUser = FirebaseAuth.getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .await().user
                ?: return Result.failure(Exception("Google sign-in failed: no user returned"))

            val normalizedEmail = firebaseUser.email?.lowercase()?.trim()
                ?: return Result.failure(Exception("Google account did not provide an email"))

            val mappedUserId = AuthPreferences.getUserIdForProviderSubject(
                context, provider = "google", subject = firebaseUser.uid
            )
            val existing = mappedUserId?.let { userDao.getUserByIdSync(it) }
                ?: userDao.getUserByEmail(normalizedEmail)

            val localUser = existing ?: run {
                val newUser = User(
                    email = normalizedEmail,
                    passwordHash = "",
                    displayName = firebaseUser.displayName?.trim(),
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                val id = userDao.insertUser(newUser)
                try {
                    userDataSeeder.seedUserData(context, id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to seed user data after Google sign-in: ${e.message}")
                    crashlytics.recordNonFatal(e, context = "google_sign_in_seed")
                }
                newUser.copy(id = id)
            }

            AuthPreferences.setProviderSubjectMapping(context, "google", firebaseUser.uid, localUser.id)
            AuthPreferences.setLoggedIn(context, localUser.id)
            crashlytics.setUserId(localUser.id.toString())
            crashlytics.log("sign_in", "provider=google")
            analytics.setUserId(localUser.id.toString())
            analytics.logSignIn("google")
            Result.success(localUser)
        } catch (e: Exception) {
            crashlytics.recordNonFatal(e, context = "sign_in_google")
            Result.failure(Exception("Google sign-in failed: ${e.message ?: "unknown error"}"))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> {
        return try {
            val trimmedEmail = email.lowercase().trim()
            val firebaseAuth = FirebaseAuth.getInstance()

            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Sign up failed"))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name.trim())
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val user = User(
                email = trimmedEmail,
                passwordHash = "",
                displayName = name.trim()
            )
            val userId = userDao.insertUser(user)

            AuthPreferences.setProviderSubjectMapping(context, "email", firebaseUser.uid, userId)

            try {
                userDataSeeder.seedUserData(context, userId)
                Log.d(TAG, "Seeded user data for userId=$userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed user data: ${e.message}")
                crashlytics.recordNonFatal(e, context = "sign_up_seed", extras = mapOf("userId" to userId.toString()))
            }

            AuthPreferences.setLoggedIn(context, userId)
            crashlytics.setUserId(userId.toString())
            crashlytics.log("sign_up", "provider=email")
            analytics.setUserId(userId.toString())
            analytics.logSignUp("email")
            Result.success(user.copy(id = userId))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email already registered"))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Password is too weak. Use at least 6 characters."))
        } catch (e: SQLiteConstraintException) {
            Result.failure(Exception("Email already registered"))
        } catch (e: Exception) {
            crashlytics.recordNonFatal(e, context = "sign_up_email")
            Result.failure(Exception("Sign up failed: ${e.message}"))
        }
    }

    suspend fun signOut() {
        runCatching { FirebaseAuth.getInstance().signOut() }
        crashlytics.clearUserId()
        crashlytics.log("sign_out")
        analytics.logSignOut()
        analytics.clearUserId()
        AuthPreferences.clearAuth(context)
    }

    /**
     * Sends a Firebase password-reset email to [email].
     */
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email.lowercase().trim())
                .await()
            Result.success(Unit)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found with this email"))
        } catch (e: Exception) {
            crashlytics.recordNonFatal(e, context = "password_reset")
            Result.failure(Exception("Could not send reset email: ${e.message}"))
        }
    }

    suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)
    }

    suspend fun updateProfile(user: User): Result<User> {
        return try {
            val updatedUser = user.copy(updatedAt = System.currentTimeMillis())
            userDao.updateUser(updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            crashlytics.recordNonFatal(e, context = "update_profile")
            Result.failure(e)
        }
    }

    suspend fun clearAllUserData() {
        healthMetricDao.deleteAllHealthMetrics()
        healthMetricDao.deleteAllCustomTypes()
        dailyLogDao.deleteAllLoggedFoods()
        dailyLogDao.deleteAllDailyLogs()
        planDao.deleteAllPlans()
        groceryDao.deleteAllGroceryItems()
        groceryDao.deleteAllGroceryLists()
        dietDao.deleteAllDietMeals()
        dietDao.deleteAllDiets()
        mealDao.deleteAllMealFoodItems()
        mealDao.deleteAllMeals()
    }

    suspend fun deleteAccount(userId: Long): Result<Unit> {
        return try {
            clearAllUserData()
            userDao.deleteUser(userId)
            runCatching { FirebaseAuth.getInstance().currentUser?.delete()?.await() }
            crashlytics.log("account_deleted")
            crashlytics.clearUserId()
            signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            crashlytics.recordNonFatal(e, context = "delete_account")
            Result.failure(e)
        }
    }
}
