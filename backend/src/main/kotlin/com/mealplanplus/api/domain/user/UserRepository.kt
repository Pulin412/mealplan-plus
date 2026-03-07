package com.mealplanplus.api.domain.user

import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByFirebaseUid(firebaseUid: String): User?
}
