package com.mealplanplus.api.domain.workout

import com.mealplanplus.api.generated.api.ExercisesApi
import com.mealplanplus.api.generated.api.WorkoutSessionsApi
import com.mealplanplus.api.generated.api.WorkoutTemplatesApi
import com.mealplanplus.api.generated.model.ExerciseDto
import com.mealplanplus.api.generated.model.LastSetsDto
import com.mealplanplus.api.generated.model.WorkoutSessionDto
import com.mealplanplus.api.generated.model.WorkoutTemplateDto
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
class ExerciseController(private val service: WorkoutService) : ExercisesApi {

    override fun listExercises(q: String?, tagId: Long?): ResponseEntity<List<ExerciseDto>> =
        ResponseEntity.ok(service.listExercises(currentUid(), q, tagId))

    override fun getExercise(id: Long): ResponseEntity<ExerciseDto> =
        ResponseEntity.ok(service.getExercise(id))

    override fun createExercise(exerciseDto: ExerciseDto): ResponseEntity<ExerciseDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.createExercise(exerciseDto, currentUid()))

    override fun updateExercise(id: Long, exerciseDto: ExerciseDto): ResponseEntity<ExerciseDto> =
        ResponseEntity.ok(service.updateExercise(id, exerciseDto, currentUid()))

    override fun deleteExercise(id: Long): ResponseEntity<Unit> {
        service.deleteExercise(id, currentUid()); return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}

@RestController
class WorkoutTemplateController(private val service: WorkoutService) : WorkoutTemplatesApi {

    override fun listWorkoutTemplates(): ResponseEntity<List<WorkoutTemplateDto>> =
        ResponseEntity.ok(service.listTemplates(currentUid()))

    override fun getWorkoutTemplate(id: Long): ResponseEntity<WorkoutTemplateDto> =
        ResponseEntity.ok(service.getTemplate(id))

    override fun createWorkoutTemplate(workoutTemplateDto: WorkoutTemplateDto): ResponseEntity<WorkoutTemplateDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.createTemplate(workoutTemplateDto, currentUid()))

    override fun updateWorkoutTemplate(id: Long, workoutTemplateDto: WorkoutTemplateDto): ResponseEntity<WorkoutTemplateDto> =
        ResponseEntity.ok(service.updateTemplate(id, workoutTemplateDto, currentUid()))

    override fun deleteWorkoutTemplate(id: Long): ResponseEntity<Unit> {
        service.deleteTemplate(id, currentUid()); return ResponseEntity.noContent().build()
    }

    override fun startWorkoutFromTemplate(id: Long): ResponseEntity<WorkoutSessionDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.startFromTemplate(id, currentUid()))

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}

@RestController
class WorkoutSessionController(private val service: WorkoutService) : WorkoutSessionsApi {

    override fun listWorkoutSessions(from: LocalDate?, to: LocalDate?): ResponseEntity<List<WorkoutSessionDto>> =
        ResponseEntity.ok(service.listSessions(currentUid(), from, to))

    override fun getWorkoutSession(id: Long): ResponseEntity<WorkoutSessionDto> =
        ResponseEntity.ok(service.getSession(id))

    override fun lastSetsForExercise(exerciseId: Long): ResponseEntity<LastSetsDto> =
        ResponseEntity.ok(service.lastSetsForExercise(currentUid(), exerciseId))

    override fun createWorkoutSession(workoutSessionDto: WorkoutSessionDto): ResponseEntity<WorkoutSessionDto> =
        ResponseEntity.status(HttpStatus.CREATED).body(service.createSession(workoutSessionDto, currentUid()))

    override fun updateWorkoutSession(id: Long, workoutSessionDto: WorkoutSessionDto): ResponseEntity<WorkoutSessionDto> =
        ResponseEntity.ok(service.updateSession(id, workoutSessionDto, currentUid()))

    override fun finishWorkoutSession(id: Long): ResponseEntity<WorkoutSessionDto> =
        ResponseEntity.ok(service.finishSession(id, currentUid()))

    override fun deleteWorkoutSession(id: Long): ResponseEntity<Unit> {
        service.deleteSession(id, currentUid()); return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
