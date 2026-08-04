package com.mealplanplus.api.domain.log

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.LocalDate

/**
 * A day the user has explicitly marked complete. The *presence* of a row means "done" — un-ticking
 * deletes it. A completed day is the unit the streak counts (a day with logged meals but no row here
 * does not count). One row per (user, date).
 */
@Entity
@Table(
    name = "day_completions",
    uniqueConstraints = [UniqueConstraint(columnNames = ["firebase_uid", "date"])]
)
class DayCompletion(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val date: LocalDate = LocalDate.now(),
    val completedAt: Instant = Instant.now(),
)

interface DayCompletionRepository : JpaRepository<DayCompletion, Long> {
    fun findByFirebaseUidAndDate(firebaseUid: String, date: LocalDate): DayCompletion?
    fun findByFirebaseUidAndDateBetween(firebaseUid: String, from: LocalDate, to: LocalDate): List<DayCompletion>
    fun deleteByFirebaseUidAndDate(firebaseUid: String, date: LocalDate)
}
