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
    /** How this exercise is logged: STRENGTH (reps + weight), CARDIO (duration + distance),
     *  or TIMED (duration only). Drives which fields the runner/builder show. */
    val type: String = "STRENGTH",
    val isSystem: Boolean = false,
    // Social (V9): copy provenance (leaf dedupe stamps these in P1).
    var copiedFromUid: String? = null,
    var copiedFromServerId: java.util.UUID? = null
) : SyncableEntity()
