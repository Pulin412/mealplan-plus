package com.mealplanplus.api.domain.user

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional
    fun getOrCreate(firebaseUid: String, email: String? = null, displayName: String? = null): UserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            ?: userRepository.save(User(firebaseUid = firebaseUid, email = email, displayName = displayName))
        return user.toResponse()
    }
}

private fun User.toResponse() = UserResponse(
    id = id,
    firebaseUid = firebaseUid,
    email = email,
    displayName = displayName,
    createdAt = createdAt
)
