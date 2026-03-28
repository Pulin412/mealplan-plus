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
import com.mealplanplus.util.AuthPreferences
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
    @ApplicationContext private val context: Context
) {
    private val TAG = "AuthRepository"
    fun isLoggedIn(): Flow<Boolean> = AuthPreferences.isLoggedIn(context)

    fun getCurrentUserId(): Flow<Long?> = AuthPreferences.getUserId(context)

    fun getCurrentUser(userId: Long): Flow<User?> = userDao.getUserById(userId)

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val trimmedEmail = email.lowercase().trim()
            val firebaseAuth = FirebaseAuth.getInstance()

            // Authenticate credentials via Firebase (handles hashing, brute-force
            // protection, account lockout, etc. — no local hash comparison needed).
            val authResult = firebaseAuth.signInWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Sign in failed"))

            // Find the matching local Room user by Firebase UID mapping or email.
            val mappedUserId = AuthPreferences.getUserIdForProviderSubject(
                context, provider = "email", subject = firebaseUser.uid
            )
            val localUser = mappedUserId?.let { userDao.getUserByIdSync(it) }
                ?: userDao.getUserByEmail(trimmedEmail)
                ?: return Result.failure(Exception("Local user record not found. Please sign up again."))

            // Keep the UID → local ID mapping fresh.
            AuthPreferences.setProviderSubjectMapping(context, "email", firebaseUser.uid, localUser.id)
            AuthPreferences.setLoggedIn(context, localUser.id)
            Result.success(localUser)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Invalid email or password"))
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("No account found with this email"))
        } catch (e: Exception) {
            Result.failure(Exception("Sign in failed: ${e.message}"))
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val firebaseAuth = runCatching { com.google.firebase.auth.FirebaseAuth.getInstance() }
                .getOrElse {
                    return Result.failure(
                        Exception("Google OAuth is not configured for this build. Please add Firebase config first.")
                    )
                }

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val firebaseUser = firebaseAuth.signInWithCredential(credential).await().user
                ?: return Result.failure(Exception("Google sign-in failed: no user returned"))

            val normalizedEmail = firebaseUser.email?.lowercase()?.trim()
                ?: return Result.failure(Exception("Google account did not provide an email"))

            val mappedUserId = AuthPreferences.getUserIdForProviderSubject(
                context,
                provider = "google",
                subject = firebaseUser.uid
            )

            val existing = mappedUserId?.let { userDao.getUserByIdSync(it) }
                ?: userDao.getUserByEmail(normalizedEmail)

            val localUser = if (existing != null) {
                existing
            } else {
                val created = User(
                    email = normalizedEmail,
                    // OAuth users do not need local password sign-in unless explicitly added later.
                    passwordHash = "",
                    displayName = firebaseUser.displayName?.trim(),
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                val id = userDao.insertUser(created)
                try {
                    userDataSeeder.seedUserData(context, id)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to seed user data after Google sign-in: ${e.message}")
                }
                created.copy(id = id)
            }

            AuthPreferences.setProviderSubjectMapping(
                context = context,
                provider = "google",
                subject = firebaseUser.uid,
                userId = localUser.id
            )
            AuthPreferences.setLoggedIn(context, localUser.id)
            Result.success(localUser)
        } catch (e: Exception) {
            Result.failure(Exception("Google sign-in failed: ${e.message ?: "unknown error"}"))
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): Result<User> {
        return try {
            val trimmedEmail = email.lowercase().trim()
            val firebaseAuth = FirebaseAuth.getInstance()

            // Create the Firebase account — Firebase handles duplicate-email detection,
            // password strength enforcement, and credential storage.
            val authResult = firebaseAuth.createUserWithEmailAndPassword(trimmedEmail, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Sign up failed"))

            // Persist display name in Firebase profile so it's visible in the console.
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name.trim())
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create the local Room user. passwordHash is empty — Firebase owns the
            // credentials, the app never stores or compares passwords locally again.
            val user = User(
                email = trimmedEmail,
                passwordHash = "",
                displayName = name.trim()
            )
            val userId = userDao.insertUser(user)

            // Map Firebase UID → local userId so sign-in can find the record.
            AuthPreferences.setProviderSubjectMapping(context, "email", firebaseUser.uid, userId)

            try {
                userDataSeeder.seedUserData(context, userId)
                Log.d(TAG, "Seeded user data for userId=$userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed user data: ${e.message}")
            }

            AuthPreferences.setLoggedIn(context, userId)
            Result.success(user.copy(id = userId))
        } catch (e: FirebaseAuthUserCollisionException) {
            Result.failure(Exception("Email already registered"))
        } catch (e: FirebaseAuthWeakPasswordException) {
            Result.failure(Exception("Password is too weak. Use at least 6 characters."))
        } catch (e: SQLiteConstraintException) {
            // Unlikely — Firebase already caught the duplicate — but handle defensively.
            Result.failure(Exception("Email already registered"))
        } catch (e: Exception) {
            Result.failure(Exception("Sign up failed: ${e.message}"))
        }
    }

    suspend fun signOut() {
        runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() }
        AuthPreferences.clearAuth(context)
    }

    /**
     * Sends a Firebase password-reset email to [email].
     * Firebase handles the link generation, expiry, and delivery — no backend needed.
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
            // Also delete the Firebase account so the email can be reused and no
            // orphaned credential record is left in Firebase Auth.
            runCatching { FirebaseAuth.getInstance().currentUser?.delete()?.await() }
            signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
