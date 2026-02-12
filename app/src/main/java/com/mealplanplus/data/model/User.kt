package com.mealplanplus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,  // Firebase UID
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val age: Int? = null,
    val contact: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
