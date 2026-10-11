package com.mealplanplus.api.domain.journal

import com.mealplanplus.api.generated.api.JournalApi
import com.mealplanplus.api.generated.model.JournalEntryDto
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class JournalController(private val service: JournalService) : JournalApi {

    override fun listJournal(from: LocalDate?, to: LocalDate?): ResponseEntity<List<JournalEntryDto>> =
        ResponseEntity.ok(service.list(currentUid(), from, to))

    override fun upsertJournal(date: LocalDate, journalEntryDto: JournalEntryDto): ResponseEntity<JournalEntryDto> =
        service.upsert(currentUid(), date, journalEntryDto.note)
            ?.let { ResponseEntity.ok(it) } ?: ResponseEntity.noContent().build()

    override fun deleteJournal(date: LocalDate): ResponseEntity<Unit> {
        service.delete(currentUid(), date); return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
