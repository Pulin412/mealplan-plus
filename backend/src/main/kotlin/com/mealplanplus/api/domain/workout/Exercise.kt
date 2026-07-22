package com.mealplanplus.api.domain.workout

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*

@Entity
@Table(name = "exercises")
class Exercise(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** null = system exercise (shared across all users) */
    val firebaseUid: String? = null,

    val name: String = "",
    val description: String? = null,
    val isSystem: Boolean = false
) : SyncableEntity()
