package com.mealplanplus.api.domain.health

import com.mealplanplus.api.generated.model.CustomMetricTypeDto
import com.mealplanplus.api.generated.model.HealthMetricDto
import com.mealplanplus.api.domain.sync.TombstoneService
import com.mealplanplus.api.domain.sync.shouldSkipUpdate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@Service
class HealthMetricService(
    private val metricRepo: HealthMetricRepository,
    private val customTypeRepo: CustomMetricTypeRepository,
    private val tombstones: TombstoneService
) {
    fun list(firebaseUid: String, type: String? = null, from: LocalDate? = null, to: LocalDate? = null): List<HealthMetricDto> {
        if (type == null) return metricRepo.findByFirebaseUid(firebaseUid).map { it.toDto() }
        if (from != null && to != null) {
            val fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant()
            val toInstant   = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant()
            return metricRepo.findByFirebaseUidAndTypeAndRecordedAtBetween(firebaseUid, type, fromInstant, toInstant)
                .map { it.toDto() }
        }
        return metricRepo.findByFirebaseUidAndType(firebaseUid, type).map { it.toDto() }
    }

    fun listCustomTypes(firebaseUid: String): List<CustomMetricTypeDto> =
        customTypeRepo.findByFirebaseUid(firebaseUid).map { it.toDto() }

    @Transactional
    fun create(dto: HealthMetricDto, firebaseUid: String): HealthMetricDto {
        val metric = HealthMetric(
            firebaseUid = firebaseUid, type = dto.type, subType = dto.subType,
            value = dto.value, secondaryValue = dto.secondaryValue, unit = dto.unit,
            recordedAt = dto.recordedAt ?: Instant.now()
        ).also { if (dto.serverId != null) it.serverId = UUID.fromString(dto.serverId.toString()) }
        return metricRepo.save(metric).toDto()
    }

    @Transactional
    fun createCustomType(dto: CustomMetricTypeDto, firebaseUid: String): CustomMetricTypeDto {
        val type = CustomMetricType(firebaseUid = firebaseUid, name = dto.name, unit = dto.unit, icon = dto.icon)
            .also { if (dto.serverId != null) it.serverId = UUID.fromString(dto.serverId.toString()) }
        return customTypeRepo.save(type).toDto()
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val metric = metricRepo.findById(id).orElseThrow()
        if (metric.firebaseUid != firebaseUid) throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
        metricRepo.delete(metric)
        tombstones.record(firebaseUid, "health_metric", metric.serverId)
    }

    fun since(firebaseUid: String, since: Instant): List<HealthMetricDto> =
        metricRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since).map { it.toDto() }

    @Transactional
    fun upsert(dto: HealthMetricDto, firebaseUid: String): HealthMetricDto {
        val serverId = dto.serverId?.let { UUID.fromString(it.toString()) }
        val existing = serverId?.let { metricRepo.findByServerId(it) }
        if (existing == null) return create(dto, firebaseUid)
        if (shouldSkipUpdate(dto.updatedAt, existing.updatedAt)) return existing.toDto()
        val updated = HealthMetric(
            id = existing.id, firebaseUid = existing.firebaseUid, type = dto.type, subType = dto.subType,
            value = dto.value, secondaryValue = dto.secondaryValue, unit = dto.unit,
            recordedAt = dto.recordedAt ?: existing.recordedAt
        ).also { it.serverId = existing.serverId }
        return metricRepo.save(updated).toDto()
    }
}

fun HealthMetric.toDto() = HealthMetricDto(
    id             = id,
    serverId       = serverId?.toString(),
    firebaseUid    = firebaseUid,
    type           = type,
    subType        = subType,
    value          = value,
    secondaryValue = secondaryValue,
    unit           = unit,
    recordedAt     = recordedAt,
    updatedAt      = updatedAt
)

fun CustomMetricType.toDto() = CustomMetricTypeDto(
    id          = id,
    serverId    = serverId?.toString(),
    firebaseUid = firebaseUid,
    name        = name,
    unit        = unit,
    icon        = icon,
    updatedAt   = updatedAt
)
