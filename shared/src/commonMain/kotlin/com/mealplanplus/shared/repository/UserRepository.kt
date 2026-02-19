package com.mealplanplus.shared.repository

import com.mealplanplus.shared.db.MealPlanDatabase
import com.mealplanplus.shared.model.User
import com.mealplanplus.shared.model.currentTimeMillis
import com.mealplanplus.shared.model.sha256
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserRepository(private val database: MealPlanDatabase) {

    private val queries = database.userQueries

    fun getAllUsers(): Flow<List<User>> {
        return queries.selectAll().asFlowList().map { list ->
            list.map { it.toUser() }
        }
    }

    suspend fun getUserById(id: Long): User? {
        return queries.selectById(id).executeAsOneOrNull()?.toUser()
    }

    suspend fun getUserByEmail(email: String): User? {
        return queries.selectByEmail(email).executeAsOneOrNull()?.toUser()
    }

    suspend fun createUser(email: String, password: String, displayName: String? = null): Long {
        val now = currentTimeMillis()
        val passwordHash = sha256(password)
        queries.insert(
            email = email,
            passwordHash = passwordHash,
            displayName = displayName,
            photoUrl = null,
            age = null,
            contact = null,
            createdAt = now,
            updatedAt = now
        )
        return queries.lastInsertRowId().executeAsOne()
    }

    suspend fun updateUser(user: User) {
        queries.update(
            email = user.email,
            passwordHash = user.passwordHash,
            displayName = user.displayName,
            photoUrl = user.photoUrl,
            age = user.age?.toLong(),
            contact = user.contact,
            updatedAt = currentTimeMillis(),
            id = user.id
        )
    }

    suspend fun deleteUser(id: Long) {
        queries.deleteById(id)
    }

    suspend fun verifyPassword(email: String, password: String): User? {
        val user = getUserByEmail(email) ?: return null
        val inputHash = sha256(password)
        return if (user.passwordHash == inputHash) user else null
    }

    private fun com.mealplanplus.shared.db.Users.toUser(): User {
        return User(
            id = id,
            email = email,
            passwordHash = passwordHash,
            displayName = displayName,
            photoUrl = photoUrl,
            age = age?.toInt(),
            contact = contact,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
