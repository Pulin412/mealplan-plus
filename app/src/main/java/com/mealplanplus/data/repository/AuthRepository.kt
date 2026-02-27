package com.mealplanplus.data.repository

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import android.util.Log
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
