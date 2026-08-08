package com.mealplanplus.api.domain.workout

import com.mealplanplus.api.generated.model.ExerciseDto
import com.mealplanplus.api.generated.model.LastSetsDto
import com.mealplanplus.api.generated.model.TagDto
import com.mealplanplus.api.generated.model.TemplateExerciseDto
import com.mealplanplus.api.generated.model.TemplateSetDto
import com.mealplanplus.api.generated.model.WorkoutSessionDto
import com.mealplanplus.api.generated.model.WorkoutSetDto
import com.mealplanplus.api.generated.model.WorkoutTemplateDto
import com.mealplanplus.api.domain.diet.EntityTag
import com.mealplanplus.api.domain.diet.EntityTagRepository
import com.mealplanplus.api.domain.diet.TagEntityType
import com.mealplanplus.api.domain.diet.TagRepository
import com.mealplanplus.api.domain.diet.toDto
import com.mealplanplus.api.domain.sync.TombstoneService
import com.mealplanplus.api.domain.sync.shouldSkipUpdate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class WorkoutService(
    private val exerciseRepo: ExerciseRepository,
    private val sessionRepo: WorkoutSessionRepository,
    private val setRepo: WorkoutSetRepository,
    private val templateRepo: WorkoutTemplateRepository,
    private val templateExerciseRepo: TemplateExerciseRepository,
    private val templateSetRepo: TemplateExerciseSetRepository,
    private val entityTagRepo: EntityTagRepository,
    private val tagRepo: TagRepository,
    private val tombstones: TombstoneService
) {
    // ── Exercise tag helpers ──────────────────────────────────────────────────

    private fun tagsForExercise(exerciseId: Long): List<TagDto> {
        val tagIds = entityTagRepo
            .findByEntityTypeAndEntityId(TagEntityType.EXERCISE, exerciseId)
            .map { it.tagId }
        return if (tagIds.isEmpty()) emptyList()
        else tagRepo.findAllById(tagIds).map { it.toDto() }
    }

    private fun Exercise.toDtoWithTags() = toDto(tagsForExercise(id))

    private fun batchExerciseToDtos(exercises: List<Exercise>): List<ExerciseDto> {
        if (exercises.isEmpty()) return emptyList()
        val ids         = exercises.map { it.id }
        val entityTags  = entityTagRepo.findByEntityTypeAndEntityIdIn(TagEntityType.EXERCISE, ids)
        val tagIds      = entityTags.map { it.tagId }.toSet()
        val tagsById    = if (tagIds.isEmpty()) emptyMap()
                          else tagRepo.findAllById(tagIds).associateBy { it.id }
        val tagsByExId  = entityTags.groupBy { it.entityId }
        return exercises.map { ex ->
            val tags = (tagsByExId[ex.id] ?: emptyList()).mapNotNull { tagsById[it.tagId] }.map { it.toDto() }
            ex.toDto(tags)
        }
    }

    private fun saveExerciseTags(exerciseId: Long, tagIds: List<Long>) {
        entityTagRepo.deleteByEntityTypeAndEntityId(TagEntityType.EXERCISE, exerciseId)
        tagIds.forEach { tagId ->
            entityTagRepo.save(EntityTag(tagId = tagId, entityType = TagEntityType.EXERCISE, entityId = exerciseId))
        }
    }

    // ── Exercises ────────────────────────────────────────────────────────────

    fun listExercises(firebaseUid: String, q: String? = null, tagId: Long? = null): List<ExerciseDto> {
        var exercises = if (!q.isNullOrBlank())
            exerciseRepo.searchByName(firebaseUid, q.trim())
        else
            exerciseRepo.findByFirebaseUidOrIsSystemTrue(firebaseUid)
        if (tagId != null) {
            val ids = entityTagRepo.findByEntityTypeAndTagId(TagEntityType.EXERCISE, tagId)
                .map { it.entityId }.toSet()
            exercises = exercises.filter { it.id in ids }
        }
        return batchExerciseToDtos(exercises)
    }

    fun getExercise(id: Long): ExerciseDto =
        exerciseRepo.findById(id).orElseThrow().toDtoWithTags()

    @Transactional
    fun createExercise(dto: ExerciseDto, firebaseUid: String): ExerciseDto {
        val exercise = Exercise(firebaseUid = firebaseUid, name = dto.name, description = dto.description)
            .also { if (dto.serverId != null) it.serverId = UUID.fromString(dto.serverId.toString()) }
        val saved = exerciseRepo.save(exercise)
        saveExerciseTags(saved.id, dto.tagIds ?: emptyList())
        return saved.toDtoWithTags()
    }

    @Transactional
    fun updateExercise(id: Long, dto: ExerciseDto, firebaseUid: String): ExerciseDto {
        val exercise = exerciseRepo.findById(id).orElseThrow()
        if (exercise.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        val updated = Exercise(id = exercise.id, firebaseUid = exercise.firebaseUid,
            name = dto.name, description = dto.description, isSystem = exercise.isSystem)
            .also { it.serverId = exercise.serverId }
        val saved = exerciseRepo.save(updated)
        saveExerciseTags(saved.id, dto.tagIds ?: emptyList())
        return saved.toDtoWithTags()
    }

    @Transactional
    fun deleteExercise(id: Long, firebaseUid: String) {
        val exercise = exerciseRepo.findById(id).orElseThrow()
        if (exercise.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        entityTagRepo.deleteByEntityTypeAndEntityId(TagEntityType.EXERCISE, id)
        exerciseRepo.delete(exercise)
        tombstones.record(firebaseUid, "exercise", exercise.serverId)
    }

    fun exercisesSince(firebaseUid: String, since: Instant): List<ExerciseDto> =
        batchExerciseToDtos(
            exerciseRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since) +
            exerciseRepo.findByIsSystemTrueAndUpdatedAtAfter(since)
        )

    @Transactional
    fun upsertExercise(dto: ExerciseDto, firebaseUid: String): ExerciseDto {
        val serverId = dto.serverId?.let { UUID.fromString(it.toString()) }
        val existing = serverId?.let { exerciseRepo.findByServerId(it) }
        if (existing == null) return createExercise(dto, firebaseUid)
        if (shouldSkipUpdate(dto.updatedAt, existing.updatedAt)) return existing.toDtoWithTags()
        val updated = Exercise(id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, description = dto.description, isSystem = existing.isSystem)
            .also { it.serverId = existing.serverId }
        val saved = exerciseRepo.save(updated)
        saveExerciseTags(saved.id, dto.tagIds ?: emptyList())
        return saved.toDtoWithTags()
    }

    // ── Workout Templates ─────────────────────────────────────────────────────

    /** Sets grouped by template-exercise id for the given membership rows. */
    private fun setsByTemplateExerciseId(texs: List<TemplateExercise>): Map<Long, List<TemplateExerciseSet>> =
        if (texs.isEmpty()) emptyMap()
        else templateSetRepo.findByTemplateExerciseIdIn(texs.map { it.id }).groupBy { it.templateExerciseId }

    /** Persist a template's exercises and their per-set targets (order = list position). */
    private fun saveTemplateExercises(templateId: Long, dtos: List<TemplateExerciseDto>) {
        dtos.forEachIndexed { idx, te ->
            val savedTe = templateExerciseRepo.save(TemplateExercise(
                templateId = templateId, exerciseId = te.exerciseId ?: 0L, orderIndex = idx, notes = te.notes))
            (te.sets ?: emptyList()).forEachIndexed { setIdx, s ->
                templateSetRepo.save(TemplateExerciseSet(
                    templateExerciseId = savedTe.id, setNumber = setIdx, reps = s.reps, weightKg = s.weightKg))
            }
        }
    }

    /** Remove a template's exercises and their sets (used before re-saving or on delete). */
    private fun deleteTemplateExercises(templateId: Long) {
        val teIds = templateExerciseRepo.findByTemplateIdOrderByOrderIndex(templateId).map { it.id }
        if (teIds.isNotEmpty()) templateSetRepo.deleteByTemplateExerciseIdIn(teIds)
        templateExerciseRepo.deleteByTemplateId(templateId)
    }

    // ── Workout-template tag helpers (mirror exercises; entity_tags with WORKOUT type) ──
    private fun tagsForTemplate(templateId: Long): List<TagDto> {
        val tagIds = entityTagRepo
            .findByEntityTypeAndEntityId(TagEntityType.WORKOUT, templateId)
            .map { it.tagId }
        return if (tagIds.isEmpty()) emptyList()
        else tagRepo.findAllById(tagIds).map { it.toDto() }
    }

    private fun saveTemplateTags(templateId: Long, tagIds: List<Long>) {
        entityTagRepo.deleteByEntityTypeAndEntityId(TagEntityType.WORKOUT, templateId)
        tagIds.forEach { tagId ->
            entityTagRepo.save(EntityTag(tagId = tagId, entityType = TagEntityType.WORKOUT, entityId = templateId))
        }
    }

    private fun WorkoutTemplate.toFullDto(): WorkoutTemplateDto {
        val texs = templateExerciseRepo.findByTemplateIdOrderByOrderIndex(id)
        val exercisesById = exerciseRepo.findAllById(texs.map { it.exerciseId }.toSet()).associateBy { it.id }
        val setsByTeId = setsByTemplateExerciseId(texs)
        return toDto(texs.map { te -> te.toDto(exercisesById[te.exerciseId], setsByTeId[te.id] ?: emptyList()) }, tagsForTemplate(id))
    }

    fun listTemplates(firebaseUid: String, tagId: Long? = null): List<WorkoutTemplateDto> {
        var templates = templateRepo.findByFirebaseUid(firebaseUid)
        if (tagId != null) {
            val ids = entityTagRepo.findByEntityTypeAndTagId(TagEntityType.WORKOUT, tagId).map { it.entityId }.toSet()
            templates = templates.filter { it.id in ids }
        }
        if (templates.isEmpty()) return emptyList()
        val allTexs = templateExerciseRepo.findByTemplateIdIn(templates.map { it.id })
        val texsByTemplateId = allTexs.groupBy { it.templateId }
        val exerciseIds = allTexs.map { it.exerciseId }.toSet()
        val exercisesById = if (exerciseIds.isEmpty()) emptyMap()
                            else exerciseRepo.findAllById(exerciseIds).associateBy { it.id }
        val setsByTeId = setsByTemplateExerciseId(allTexs)
        // Batch-load tags for all templates in one query, mirroring the exercise-list path.
        val entityTags = entityTagRepo.findByEntityTypeAndEntityIdIn(TagEntityType.WORKOUT, templates.map { it.id })
        val tagIdSet   = entityTags.map { it.tagId }.toSet()
        val tagsById   = if (tagIdSet.isEmpty()) emptyMap()
                         else tagRepo.findAllById(tagIdSet).associateBy { it.id }
        val tagsByTemplateId = entityTags.groupBy { it.entityId }
        return templates.map { template ->
            val texs = (texsByTemplateId[template.id] ?: emptyList()).sortedBy { it.orderIndex }
            val tags = (tagsByTemplateId[template.id] ?: emptyList()).mapNotNull { tagsById[it.tagId] }.map { it.toDto() }
            template.toDto(texs.map { te -> te.toDto(exercisesById[te.exerciseId], setsByTeId[te.id] ?: emptyList()) }, tags)
        }
    }

    fun getTemplate(id: Long): WorkoutTemplateDto =
        templateRepo.findById(id).orElseThrow().toFullDto()

    @Transactional
    fun createTemplate(dto: WorkoutTemplateDto, firebaseUid: String): WorkoutTemplateDto {
        val template = WorkoutTemplate(firebaseUid = firebaseUid, name = dto.name, notes = dto.notes)
        val saved = templateRepo.save(template)
        saveTemplateExercises(saved.id, dto.exercises ?: emptyList())
        saveTemplateTags(saved.id, dto.tagIds ?: emptyList())
        return saved.toFullDto()
    }

    @Transactional
    fun updateTemplate(id: Long, dto: WorkoutTemplateDto, firebaseUid: String): WorkoutTemplateDto {
        val existing = templateRepo.findById(id).orElseThrow()
        if (existing.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        val updated = WorkoutTemplate(id = existing.id, firebaseUid = firebaseUid,
            name = dto.name, notes = dto.notes)
            .also { it.serverId = existing.serverId }
        val saved = templateRepo.save(updated)
        deleteTemplateExercises(id)
        saveTemplateExercises(saved.id, dto.exercises ?: emptyList())
        saveTemplateTags(saved.id, dto.tagIds ?: emptyList())
        return saved.toFullDto()
    }

    @Transactional
    fun deleteTemplate(id: Long, firebaseUid: String) {
        val template = templateRepo.findById(id).orElseThrow()
        if (template.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        deleteTemplateExercises(id)
        entityTagRepo.deleteByEntityTypeAndEntityId(TagEntityType.WORKOUT, id)
        templateRepo.delete(template)
    }

    @Transactional
    fun toggleTemplateShare(id: Long, firebaseUid: String): WorkoutTemplateDto {
        val template = templateRepo.findById(id).orElseThrow()
        if (template.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        template.isShared = !template.isShared
        return templateRepo.save(template).toFullDto()
    }

    /** Author-scoped shared reads for the social layer. */
    fun sharedTemplatesOf(authorUid: String): List<WorkoutTemplate> =
        templateRepo.findByFirebaseUid(authorUid).filter { it.isShared }

    /** Shared template + the exercises it references (for read-only render). */
    fun sharedTemplateBundle(authorUid: String, serverId: UUID): Pair<WorkoutTemplateDto, List<ExerciseDto>>? {
        val template = templateRepo.findByServerId(serverId) ?: return null
        if (template.firebaseUid != authorUid || !template.isShared) return null
        val exerciseIds = templateExerciseRepo.findByTemplateIdOrderByOrderIndex(template.id)
            .map { it.exerciseId }.toSet()
        val exercises = if (exerciseIds.isEmpty()) emptyList()
                        else exerciseRepo.findAllById(exerciseIds).map { it.toDto() }
        return template.toFullDto() to exercises
    }

    @Transactional
    fun startFromTemplate(templateId: Long, firebaseUid: String): WorkoutSessionDto {
        val template = templateRepo.findById(templateId).orElseThrow()
        val session = WorkoutSession(firebaseUid = firebaseUid, name = template.name,
            date = LocalDate.now(), isCompleted = false)
        val saved = sessionRepo.save(session)
        val texs = templateExerciseRepo.findByTemplateIdOrderByOrderIndex(templateId)
        val setsByTeId = setsByTemplateExerciseId(texs)
        val sets = mutableListOf<WorkoutSet>()
        texs.forEach { te ->
            (setsByTeId[te.id] ?: emptyList()).sortedBy { it.setNumber }.forEachIndexed { setIdx, ts ->
                sets.add(setRepo.save(WorkoutSet(sessionId = saved.id, exerciseId = te.exerciseId,
                    setNumber = setIdx, reps = ts.reps, weightKg = ts.weightKg)))
            }
        }
        return saved.toDto(sets)
    }

    // ── Workout Sessions ──────────────────────────────────────────────────────

    fun listSessions(firebaseUid: String, from: LocalDate? = null, to: LocalDate? = null): List<WorkoutSessionDto> {
        val sessions = if (from != null && to != null)
            sessionRepo.findByFirebaseUidAndDateBetween(firebaseUid, from, to)
        else
            sessionRepo.findByFirebaseUid(firebaseUid)
        if (sessions.isEmpty()) return emptyList()
        val setsBySessionId = setRepo.findBySessionIdIn(sessions.map { it.id }).groupBy { it.sessionId }
        return sessions.map { it.toDto(setsBySessionId[it.id] ?: emptyList()) }
    }

    fun getSession(id: Long): WorkoutSessionDto {
        val session = sessionRepo.findById(id).orElseThrow()
        return session.toDto(setRepo.findBySessionId(session.id))
    }

    @Transactional
    fun createSession(dto: WorkoutSessionDto, firebaseUid: String): WorkoutSessionDto {
        val session = WorkoutSession(
            firebaseUid = firebaseUid, name = dto.name,
            date = dto.date ?: LocalDate.now(), durationMinutes = dto.durationMinutes,
            notes = dto.notes, isCompleted = dto.isCompleted ?: false
        ).also { if (dto.serverId != null) it.serverId = UUID.fromString(dto.serverId.toString()) }
        val saved = sessionRepo.save(session)
        val sets = (dto.sets ?: emptyList()).map { s ->
            setRepo.save(WorkoutSet(sessionId = saved.id, exerciseId = s.exerciseId ?: 0L,
                setNumber = s.setNumber, reps = s.reps, weightKg = s.weightKg, notes = s.notes))
        }
        return saved.toDto(sets)
    }

    @Transactional
    fun updateSession(id: Long, dto: WorkoutSessionDto, firebaseUid: String): WorkoutSessionDto {
        val session = sessionRepo.findById(id).orElseThrow()
        if (session.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        setRepo.deleteBySessionId(id)
        val updated = WorkoutSession(id = session.id, firebaseUid = session.firebaseUid,
            name = session.name, date = session.date, durationMinutes = dto.durationMinutes,
            notes = dto.notes, isCompleted = session.isCompleted)
            .also { it.serverId = session.serverId }
        val saved = sessionRepo.save(updated)
        val sets = (dto.sets ?: emptyList()).map { s ->
            setRepo.save(WorkoutSet(sessionId = saved.id, exerciseId = s.exerciseId ?: 0L,
                setNumber = s.setNumber, reps = s.reps, weightKg = s.weightKg, notes = s.notes))
        }
        return saved.toDto(sets)
    }

    /** Marks a session complete and upserts — replaces an existing log for the same (uid, date, name). */
    @Transactional
    fun finishSession(id: Long, firebaseUid: String): WorkoutSessionDto {
        val session = sessionRepo.findById(id).orElseThrow()
        if (session.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        val finished = WorkoutSession(id = session.id, firebaseUid = session.firebaseUid,
            name = session.name, date = session.date, durationMinutes = session.durationMinutes,
            notes = session.notes, isCompleted = true)
            .also { it.serverId = session.serverId }
        return sessionRepo.save(finished).toDto(setRepo.findBySessionId(session.id))
    }

    /**
     * Sets for the Session Runner "Last time" / "Copy last" panel. When [workout] is given, anchors on
     * the most recent completed session of that same workout (matched by name) — the last time you did
     * *this* workout — and reads the exercise's sets from it (empty if it wasn't logged there). Without
     * [workout], falls back to the exercise's most recent appearance in any workout. Either way it's a
     * single session's sets, never an aggregate across sessions.
     */
    fun lastSetsForExercise(firebaseUid: String, exerciseId: Long, workout: String? = null): LastSetsDto {
        val sessionId = if (!workout.isNullOrBlank())
            sessionRepo.findCompletedSessionIdsByName(firebaseUid, workout).firstOrNull()
        else
            sessionRepo.findCompletedSessionIdsForExercise(firebaseUid, exerciseId).firstOrNull()
        val sets = sessionId
            ?.let { setRepo.findBySessionIdAndExerciseId(it, exerciseId).sortedBy { s -> s.setNumber } }
            ?: emptyList()
        return LastSetsDto(exerciseId = exerciseId, sets = sets.map { it.toDto() })
    }

    @Transactional
    fun deleteSession(id: Long, firebaseUid: String) {
        val session = sessionRepo.findById(id).orElseThrow()
        if (session.firebaseUid != firebaseUid)
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        setRepo.deleteBySessionId(id)
        sessionRepo.delete(session)
        tombstones.record(firebaseUid, "workout_session", session.serverId)
    }

    fun sessionsSince(firebaseUid: String, since: Instant): List<WorkoutSessionDto> {
        val sessions = sessionRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since)
        if (sessions.isEmpty()) return emptyList()
        val setsBySessionId = setRepo.findBySessionIdIn(sessions.map { it.id }).groupBy { it.sessionId }
        return sessions.map { it.toDto(setsBySessionId[it.id] ?: emptyList()) }
    }

    @Transactional
    fun upsertSession(dto: WorkoutSessionDto, firebaseUid: String): WorkoutSessionDto {
        val serverId = dto.serverId?.let { UUID.fromString(it.toString()) }
        val existing = serverId?.let { sessionRepo.findByServerId(it) }
        if (existing == null) return createSession(dto, firebaseUid)
        if (shouldSkipUpdate(dto.updatedAt, existing.updatedAt))
            return existing.toDto(setRepo.findBySessionId(existing.id))
        setRepo.deleteBySessionId(existing.id)
        val updated = WorkoutSession(id = existing.id, firebaseUid = existing.firebaseUid,
            name = dto.name, date = dto.date ?: existing.date,
            durationMinutes = dto.durationMinutes, notes = dto.notes,
            isCompleted = dto.isCompleted ?: false)
            .also { it.serverId = existing.serverId }
        val saved = sessionRepo.save(updated)
        val sets = (dto.sets ?: emptyList()).map { s ->
            setRepo.save(WorkoutSet(sessionId = saved.id, exerciseId = s.exerciseId ?: 0L,
                setNumber = s.setNumber, reps = s.reps, weightKg = s.weightKg, notes = s.notes))
        }
        return saved.toDto(sets)
    }
}

fun WorkoutSet.toDto() = WorkoutSetDto(
    id         = id,
    sessionId  = sessionId,
    exerciseId = exerciseId,
    setNumber  = setNumber,
    reps       = reps,
    weightKg   = weightKg,
    notes      = notes
)

fun WorkoutSession.toDto(sets: List<WorkoutSet>) = WorkoutSessionDto(
    id              = id,
    serverId        = serverId,
    firebaseUid     = firebaseUid,
    name            = name,
    date            = date,
    durationMinutes = durationMinutes,
    notes           = notes,
    isCompleted     = isCompleted,
    sets            = sets.map { it.toDto() },
    updatedAt       = updatedAt
)

fun TemplateExerciseSet.toDto() = TemplateSetDto(
    setNumber = setNumber,
    reps      = reps,
    weightKg  = weightKg
)

fun TemplateExercise.toDto(exercise: Exercise?, sets: List<TemplateExerciseSet>) = TemplateExerciseDto(
    id           = id,
    templateId   = templateId,
    exerciseId   = exerciseId,
    orderIndex   = orderIndex,
    sets         = sets.sortedBy { it.setNumber }.map { it.toDto() },
    notes        = notes,
    exerciseName = exercise?.name ?: ""
)

fun WorkoutTemplate.toDto(exercises: List<TemplateExerciseDto>, tags: List<TagDto> = emptyList()) = WorkoutTemplateDto(
    id          = id,
    firebaseUid = firebaseUid,
    name        = name,
    notes       = notes,
    exercises   = exercises,
    tagIds      = tags.map { it.id },
    isShared    = isShared
)

fun Exercise.toDto(tags: List<TagDto> = emptyList()) = ExerciseDto(
    id          = id,
    serverId    = serverId,
    name        = name,
    description = description,
    isSystem    = isSystem,
    tagIds      = tags.map { it.id },
    tags        = tags,
    updatedAt   = updatedAt
)
