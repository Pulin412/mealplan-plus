package com.mealplanplus.api.domain.journal

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant
import java.time.LocalDate

/**
 * A free-text daily diary note, one per (user, date). Online-only — NOT part of the offline
 * delta-sync (so it doesn't extend SyncableEntity); both clients read/write it directly over REST.
 */
@Entity
@Table(
    name = "journal_entries",
    uniqueConstraints = [UniqueConstraint(columnNames = ["firebase_uid", "date"])]
)
class JournalEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    val firebaseUid: String = "",
    val date: LocalDate = LocalDate.now(),
    @Column(columnDefinition = "text")
    var note: String = "",
    var updatedAt: Instant = Instant.now(),
)

interface JournalRepository : JpaRepository<JournalEntry, Long> {
    fun findByFirebaseUidAndDateBetween(firebaseUid: String, from: LocalDate, to: LocalDate): List<JournalEntry>
    fun findByFirebaseUid(firebaseUid: String): List<JournalEntry>
    fun findByFirebaseUidAndDate(firebaseUid: String, date: LocalDate): JournalEntry?
    fun deleteByFirebaseUidAndDate(firebaseUid: String, date: LocalDate)
}
