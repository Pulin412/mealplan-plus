package com.mealplanplus.api.domain.plan

import com.mealplanplus.api.domain.SyncableEntity
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "day_plans",
    uniqueConstraints = [UniqueConstraint(columnNames = ["firebase_uid", "date"])]
)
class DayPlan(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val date: LocalDate = LocalDate.now(),
    val dietId: Long = 0
) : SyncableEntity()

// ── DTO ──────────────────────────────────────────────────────────────────────

data class DayPlanDto(
    val id: Long = 0,
    val serverId: UUID? = null,
    val firebaseUid: String = "",
    val date: String = "",          // ISO-8601 yyyy-MM-dd
    val dietId: Long = 0,
    val updatedAt: Instant? = null
)

fun DayPlan.toDto() = DayPlanDto(
    id          = id,
    serverId    = serverId,
    firebaseUid = firebaseUid,
    date        = date.toString(),
    dietId      = dietId,
    updatedAt   = updatedAt
)
