package com.mealplanplus.api.domain.journal

import com.mealplanplus.api.generated.model.JournalEntryDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class JournalService(private val repo: JournalRepository) {

    fun list(firebaseUid: String, from: LocalDate?, to: LocalDate?): List<JournalEntryDto> {
        val entries = if (from != null && to != null)
            repo.findByFirebaseUidAndDateBetween(firebaseUid, from, to)
        else repo.findByFirebaseUid(firebaseUid)
        return entries.sortedByDescending { it.date }.map { it.toDto() }
    }

    /** Create or replace the note for a date. A blank note deletes the entry. */
    @Transactional
    fun upsert(firebaseUid: String, date: LocalDate, note: String?): JournalEntryDto? {
        val text = note?.trim().orEmpty()
        if (text.isEmpty()) { repo.deleteByFirebaseUidAndDate(firebaseUid, date); return null }
        val existing = repo.findByFirebaseUidAndDate(firebaseUid, date)
        val entry = existing?.apply { this.note = text; this.updatedAt = Instant.now() }
            ?: JournalEntry(firebaseUid = firebaseUid, date = date, note = text, updatedAt = Instant.now())
        return repo.save(entry).toDto()
    }

    @Transactional
    fun delete(firebaseUid: String, date: LocalDate) = repo.deleteByFirebaseUidAndDate(firebaseUid, date)
}

private fun JournalEntry.toDto() = JournalEntryDto(date = date, note = note, id = id, updatedAt = updatedAt)
