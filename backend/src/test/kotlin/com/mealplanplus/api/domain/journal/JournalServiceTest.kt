package com.mealplanplus.api.domain.journal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest
@Transactional
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class JournalServiceTest {

    @Autowired lateinit var service: JournalService
    private val uid = "uid-journal-test"
    private val date = LocalDate.of(2026, 1, 15)

    @Test
    fun `upsert creates then replaces the note for a date`() {
        service.upsert(uid, date, "cheated a bit")
        assertEquals("cheated a bit", service.list(uid, null, null).single().note)

        service.upsert(uid, date, "back on track")
        val all = service.list(uid, null, null)
        assertEquals(1, all.size, "same date should update, not duplicate")
        assertEquals("back on track", all.single().note)
    }

    @Test
    fun `blank note deletes the entry and delete is idempotent`() {
        service.upsert(uid, date, "temp")
        assertNull(service.upsert(uid, date, "   "), "blank note removes the entry")
        assertTrue(service.list(uid, null, null).isEmpty())
        service.delete(uid, date) // no throw on a missing entry
    }

    @Test
    fun `list filters by date range and is scoped to the user`() {
        service.upsert(uid, LocalDate.of(2026, 3, 1), "march")
        service.upsert(uid, LocalDate.of(2026, 4, 1), "april")
        service.upsert("someone-else", LocalDate.of(2026, 3, 1), "not mine")

        val march = service.list(uid, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))
        assertEquals(listOf("march"), march.map { it.note })
    }
}
