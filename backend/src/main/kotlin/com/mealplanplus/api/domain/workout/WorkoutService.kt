package com.mealplanplus.api.domain.workout

import com.mealplanplus.api.domain.sync.TombstoneService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class WorkoutService(
    private val exerciseRepo: ExerciseRepository,
    private val sessionRepo: WorkoutSessionRepository,
    private val setRepo: WorkoutSetRepository,
    private val templateRepo: WorkoutTemplateRepository,
    private val templateExerciseRepo: TemplateExerciseRepository,
    private val tombstones: TombstoneService
) {

    // ── Exercises ────────────────────────────────────────────────────────────

    fun listExercises(firebaseUid: String): List<ExerciseDto> =
        exerciseRepo.findByFirebaseUidOrIsSystemTrue(firebaseUid).map { it.toDto() }

    @Transactional
    fun createExercise(dto: ExerciseDto, firebaseUid: String): ExerciseDto {
        val exercise = Exercise(
            firebaseUid = firebaseUid, name = dto.name, category = dto.category,
            muscleGroup = dto.muscleGroup, equipment = dto.equipment,
            description = dto.description, videoLink = dto.videoLink
        ).also { if (dto.serverId != null) it.serverId = dto.serverId }
        return exerciseRepo.save(exercise).toDto()
    }

    @Transactional
    fun deleteExercise(id: Long, firebaseUid: String) {
        val exercise = exerciseRepo.findById(id).orElseThrow()
        require(exercise.firebaseUid == firebaseUid) { "Forbidden" }
        exerciseRepo.delete(exercise)
        tombstones.record(firebaseUid, "exercise", exercise.serverId)
    }

    // ── Templates ─────────────────────────────────────────────────────────────

    private fun WorkoutTemplate.toFullDto(): WorkoutTemplateDto {
        val texs = templateExerciseRepo.findByTemplateIdOrderByOrderIndex(id)
        return toDto(texs.map { te ->
            val ex = exerciseRepo.findById(te.exerciseId).orElse(null)
            te.toDto(ex)
        })
    }

    fun listTemplates(firebaseUid: String): List<WorkoutTemplateDto> =
        templateRepo.findByFirebaseUid(firebaseUid).map { it.toFullDto() }

    fun getTemplate(id: Long): WorkoutTemplateDto =
        templateRepo.findById(id).orElseThrow().toFullDto()

    @Transactional
    fun createTemplate(dto: WorkoutTemplateDto, firebaseUid: String): WorkoutTemplateDto {
        val template = WorkoutTemplate(firebaseUid = firebaseUid, name = dto.name,
            category = dto.category, notes = dto.notes)
        val saved = templateRepo.save(template)
        dto.exercises.forEachIndexed { idx, te ->
            templateExerciseRepo.save(TemplateExercise(templateId = saved.id,
                exerciseId = te.exerciseId, orderIndex = idx,
                targetSets = te.targetSets, targetReps = te.targetReps,
                targetWeightKg = te.targetWeightKg, notes = te.notes))
        }
        return saved.toFullDto()
    }

    @Transactional
    fun updateTemplate(id: Long, dto: WorkoutTemplateDto, firebaseUid: String): WorkoutTemplateDto {
        val existing = templateRepo.findById(id).orElseThrow()
        require(existing.firebaseUid == firebaseUid) { "Forbidden" }
        val updated = WorkoutTemplate(id = existing.id, firebaseUid = firebaseUid,
            name = dto.name, category = dto.category, notes = dto.notes)
            .also { it.serverId = existing.serverId }
        val saved = templateRepo.save(updated)
        templateExerciseRepo.deleteByTemplateId(id)
        dto.exercises.forEachIndexed { idx, te ->
            templateExerciseRepo.save(TemplateExercise(templateId = saved.id,
                exerciseId = te.exerciseId, orderIndex = idx,
                targetSets = te.targetSets, targetReps = te.targetReps,
                targetWeightKg = te.targetWeightKg, notes = te.notes))
        }
        return saved.toFullDto()
    }

    @Transactional
    fun deleteTemplate(id: Long, firebaseUid: String) {
        val template = templateRepo.findById(id).orElseThrow()
        require(template.firebaseUid == firebaseUid) { "Forbidden" }
        templateExerciseRepo.deleteByTemplateId(id)
        templateRepo.delete(template)
    }

    /**
     * Create a new WorkoutSession pre-populated from a template.
     * Each template exercise gets [targetSets] WorkoutSets with the target weight/reps.
     */
    @Transactional
    fun startFromTemplate(templateId: Long, firebaseUid: String): WorkoutSessionDto {
        val template = templateRepo.findById(templateId).orElseThrow()
        val session = WorkoutSession(firebaseUid = firebaseUid, name = template.name,
            date = LocalDate.now(), isCompleted = false)
        val saved = sessionRepo.save(session)
        val texs = templateExerciseRepo.findByTemplateIdOrderByOrderIndex(templateId)
        val sets = mutableListOf<WorkoutSet>()
        texs.forEach { te ->
            repeat(te.targetSets) { setIdx ->
                sets.add(setRepo.save(WorkoutSet(sessionId = saved.id, exerciseId = te.exerciseId,
                    setNumber = setIdx, reps = te.targetReps, weightKg = te.targetWeightKg)))
            }
        }
        return saved.toDto(sets)
    }

    fun exercisesSince(firebaseUid: String, since: Instant): List<ExerciseDto> =
        (exerciseRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since) +
         exerciseRepo.findByIsSystemTrueAndUpdatedAtAfter(since)).map { it.toDto() }

    @Transactional
    fun upsertExercise(dto: ExerciseDto, firebaseUid: String): ExerciseDto {
        val existing = dto.serverId?.let { exerciseRepo.findByServerId(it) }
        if (existing == null) return createExercise(dto, firebaseUid)
        if ((dto.updatedAt ?: Instant.EPOCH) <= existing.updatedAt) return existing.toDto()
        val updated = Exercise(
            id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, category = dto.category, muscleGroup = dto.muscleGroup,
            equipment = dto.equipment, description = dto.description,
            videoLink = dto.videoLink, isSystem = existing.isSystem
        ).also { it.serverId = existing.serverId }
        return exerciseRepo.save(updated).toDto()
    }

    // ── Sessions ─────────────────────────────────────────────────────────────

    fun listSessions(firebaseUid: String): List<WorkoutSessionDto> =
        sessionRepo.findByFirebaseUid(firebaseUid)
            .map { it.toDto(setRepo.findBySessionId(it.id)) }

    fun getSession(id: Long): WorkoutSessionDto {
        val session = sessionRepo.findById(id).orElseThrow()
        return session.toDto(setRepo.findBySessionId(session.id))
    }

    @Transactional
    fun createSession(dto: WorkoutSessionDto, firebaseUid: String): WorkoutSessionDto {
        val session = WorkoutSession(
            firebaseUid = firebaseUid, name = dto.name,
            date = dto.date ?: LocalDate.now(), durationMinutes = dto.durationMinutes,
            notes = dto.notes, isCompleted = dto.isCompleted
        ).also { if (dto.serverId != null) it.serverId = dto.serverId }
        val saved = sessionRepo.save(session)
        val sets = dto.sets.map { s ->
            setRepo.save(WorkoutSet(sessionId = saved.id, exerciseId = s.exerciseId,
                setNumber = s.setNumber, reps = s.reps, weightKg = s.weightKg,
                durationSeconds = s.durationSeconds, distanceMeters = s.distanceMeters,
                notes = s.notes))
        }
        return saved.toDto(sets)
    }

    @Transactional
    fun deleteSession(id: Long, firebaseUid: String) {
        val session = sessionRepo.findById(id).orElseThrow()
        require(session.firebaseUid == firebaseUid) { "Forbidden" }
        setRepo.deleteBySessionId(id)
        sessionRepo.delete(session)
        tombstones.record(firebaseUid, "workout_session", session.serverId)
    }

    fun sessionsSince(firebaseUid: String, since: Instant): List<WorkoutSessionDto> =
        sessionRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
            .map { it.toDto(setRepo.findBySessionId(it.id)) }

    @Transactional
    fun upsertSession(dto: WorkoutSessionDto, firebaseUid: String): WorkoutSessionDto {
        val existing = dto.serverId?.let { sessionRepo.findByServerId(it) }
        if (existing == null) return createSession(dto, firebaseUid)
        if ((dto.updatedAt ?: Instant.EPOCH) <= existing.updatedAt) return existing.toDto(setRepo.findBySessionId(existing.id))
        setRepo.deleteBySessionId(existing.id)
        val updated = WorkoutSession(
            id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, date = dto.date ?: existing.date,
            durationMinutes = dto.durationMinutes, notes = dto.notes,
            isCompleted = dto.isCompleted
        ).also { it.serverId = existing.serverId }
        val saved = sessionRepo.save(updated)
        val sets = dto.sets.map { s ->
            setRepo.save(WorkoutSet(sessionId = saved.id, exerciseId = s.exerciseId,
                setNumber = s.setNumber, reps = s.reps, weightKg = s.weightKg,
                durationSeconds = s.durationSeconds, distanceMeters = s.distanceMeters,
                notes = s.notes))
        }
        return saved.toDto(sets)
    }
}
