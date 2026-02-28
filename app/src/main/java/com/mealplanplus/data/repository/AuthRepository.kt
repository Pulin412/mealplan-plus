package com.mealplanplus.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import com.google.firebase.auth.GoogleAuthProvider
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
            val user = userDao.getUserByEmail(email.lowercase().trim())
                ?: return Result.failure(Exception("User not found"))

            if (!User.verifyPassword(password, user.passwordHash)) {
                return Result.failure(Exception("Invalid password"))
            }

            AuthPreferences.setLoggedIn(context, user.id)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
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

            val existingUser = userDao.getUserByEmail(trimmedEmail)
            if (existingUser != null) {
                return Result.failure(Exception("Email already registered"))
            }

            val user = User(
                email = trimmedEmail,
                passwordHash = User.hashPassword(password),
                displayName = name.trim()
            )

            val userId = userDao.insertUser(user)
            val savedUser = user.copy(id = userId)

            try {
                userDataSeeder.seedUserData(context, userId)
                Log.d(TAG, "Seeded user data for userId=$userId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to seed user data: ${e.message}")
            }

            AuthPreferences.setLoggedIn(context, userId)
            Result.success(savedUser)
        } catch (e: SQLiteConstraintException) {
            Result.failure(Exception("Email already registered"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        runCatching { com.google.firebase.auth.FirebaseAuth.getInstance().signOut() }
        AuthPreferences.clearAuth(context)
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
            signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
