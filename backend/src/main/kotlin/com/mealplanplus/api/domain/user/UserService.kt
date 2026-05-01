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

    @Transactional
    fun update(firebaseUid: String, req: UpdateUserRequest): UserResponse {
        val user = userRepository.findByFirebaseUid(firebaseUid)
            ?: userRepository.save(User(firebaseUid = firebaseUid))
        req.displayName?.let { user.displayName = it }
        req.age?.let { user.age = it }
        req.weightKg?.let { user.weightKg = it }
        req.heightCm?.let { user.heightCm = it }
        req.gender?.let { user.gender = it }
        req.activityLevel?.let { user.activityLevel = it }
        req.targetCalories?.let { user.targetCalories = it }
        req.goalType?.let { user.goalType = it }
        return userRepository.save(user).toResponse()
    }
}

fun User.toResponse() = UserResponse(
    id = id,
    firebaseUid = firebaseUid,
    email = email,
    displayName = displayName,
    age = age,
    weightKg = weightKg,
    heightCm = heightCm,
    gender = gender,
    activityLevel = activityLevel,
    targetCalories = targetCalories,
    goalType = goalType,
    createdAt = createdAt
)
