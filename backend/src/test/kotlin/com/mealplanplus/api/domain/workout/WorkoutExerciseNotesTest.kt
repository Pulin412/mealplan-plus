package com.mealplanplus.api.domain.workout

import com.mealplanplus.api.generated.model.ExerciseNoteDto
import com.mealplanplus.api.generated.model.WorkoutSessionDto
import com.mealplanplus.api.generated.model.WorkoutSetDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Per-exercise notes-to-self (V10): persisted per (session, exercise), returned with the session,
 * and surfaced by "copy last" (lastSetsForExercise) — but kept independent of the sets, so copying
 * sets never copies the note.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class WorkoutExerciseNotesTest {

    @Autowired lateinit var service: WorkoutService

    private val uid = "uid-notes"
    private val benchId = 100L
    private val squatId = 200L

    private fun session(name: String, completed: Boolean, notes: List<ExerciseNoteDto>) = WorkoutSessionDto(
        name = name,
        date = LocalDate.now(),
        isCompleted = completed,
        sets = listOf(
            WorkoutSetDto(exerciseId = benchId, setNumber = 0, reps = 8, weightKg = 60.0),
            WorkoutSetDto(exerciseId = benchId, setNumber = 1, reps = 8, weightKg = 60.0),
        ),
        exerciseNotes = notes,
    )

    @Test
    fun `exercise notes round-trip through create and get, blanks dropped`() {
        val created = service.createSession(
            session("Push", completed = false, notes = listOf(
                ExerciseNoteDto(exerciseId = benchId, note = "elbows tucked"),
                ExerciseNoteDto(exerciseId = squatId, note = "  "),   // blank → dropped
            )),
            uid,
        )
        assertEquals(listOf(benchId to "elbows tucked"), created.exerciseNotes!!.map { it.exerciseId to it.note })

        val fetched = service.getSession(created.id!!)
        assertEquals(listOf(benchId to "elbows tucked"), fetched.exerciseNotes!!.map { it.exerciseId to it.note })
    }

    @Test
    fun `update replaces the session's exercise notes`() {
        val created = service.createSession(
            session("Push", completed = false, notes = listOf(ExerciseNoteDto(exerciseId = benchId, note = "old"))),
            uid,
        )
        val updated = service.updateSession(
            created.id!!,
            session("Push", completed = false, notes = listOf(ExerciseNoteDto(exerciseId = benchId, note = "new cue"))),
            uid,
        )
        assertEquals(listOf("new cue"), updated.exerciseNotes!!.map { it.note })
    }

    @Test
    fun `re-saving a session with the same exercise note does not violate the unique constraint`() {
        // Repro for the prod 500 (uq_wsen_session_exercise): finishing/re-saving a session that
        // already has a note for an exercise must replace it, not collide with the existing row.
        val created = service.createSession(
            session("Push", completed = false, notes = listOf(ExerciseNoteDto(exerciseId = benchId, note = "cue v1"))),
            uid,
        )
        val updated = service.updateSession(
            created.id!!,
            session("Push", completed = true, notes = listOf(ExerciseNoteDto(exerciseId = benchId, note = "cue v2"))),
            uid,
        )
        assertEquals(listOf(benchId to "cue v2"), updated.exerciseNotes!!.map { it.exerciseId to it.note })
        // Exactly one note persisted for the exercise (re-read from the DB).
        assertEquals(listOf(benchId to "cue v2"), service.getSession(created.id!!).exerciseNotes!!.map { it.exerciseId to it.note })
    }

    @Test
    fun `copy last surfaces the note from the last completed session, separate from the sets`() {
        service.createSession(
            session("Push", completed = true, notes = listOf(ExerciseNoteDto(exerciseId = benchId, note = "pause at chest"))),
            uid,
        )
        val last = service.lastSetsForExercise(uid, benchId, "Push")
        assertEquals("pause at chest", last.note)
        assertEquals(2, last.sets.size)                 // sets come across
        assertTrue(last.sets.all { it.notes == null })  // the exercise note is NOT stamped onto sets
    }

    @Test
    fun `copy last note is null when the last session had no note for that exercise`() {
        service.createSession(session("Pull", completed = true, notes = emptyList()), uid)
        assertNull(service.lastSetsForExercise(uid, benchId, "Pull").note)
    }
}
