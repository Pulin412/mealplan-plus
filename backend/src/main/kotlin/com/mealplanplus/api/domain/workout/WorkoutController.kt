package com.mealplanplus.api.domain.workout

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/exercises")
@Tag(name = "Exercises")
class ExerciseController(private val service: WorkoutService) {

    @GetMapping
    fun list(auth: Authentication) = service.listExercises(auth.name)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody dto: ExerciseDto, auth: Authentication) =
        service.createExercise(dto, auth.name)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, auth: Authentication) =
        service.deleteExercise(id, auth.name)
}

@RestController
@RequestMapping("/api/v1/workout-sessions")
@Tag(name = "Workout Sessions")
class WorkoutSessionController(private val service: WorkoutService) {

    @GetMapping
    fun list(auth: Authentication) = service.listSessions(auth.name)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, auth: Authentication) = service.getSession(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody dto: WorkoutSessionDto, auth: Authentication) =
        service.createSession(dto, auth.name)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, auth: Authentication) =
        service.deleteSession(id, auth.name)
}
