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
    val category: String = "STRENGTH",
    val muscleGroup: String? = null,
    val equipment: String? = null,
    val description: String? = null,
    val videoLink: String? = null,
    val isSystem: Boolean = false
) : SyncableEntity()
