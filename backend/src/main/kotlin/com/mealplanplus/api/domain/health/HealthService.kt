package com.mealplanplus.api.domain.health

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class HealthMetricService(
    private val metricRepo: HealthMetricRepository,
    private val customTypeRepo: CustomMetricTypeRepository
) {
    fun list(firebaseUid: String): List<HealthMetricDto> =
        metricRepo.findByFirebaseUid(firebaseUid).map { it.toDto() }

    fun listCustomTypes(firebaseUid: String): List<CustomMetricTypeDto> =
        customTypeRepo.findByFirebaseUid(firebaseUid).map { it.toDto() }

    @Transactional
    fun create(dto: HealthMetricDto, firebaseUid: String): HealthMetricDto {
        val metric = HealthMetric(
            firebaseUid = firebaseUid, type = dto.type, subType = dto.subType,
            value = dto.value, secondaryValue = dto.secondaryValue, unit = dto.unit,
            recordedAt = dto.recordedAt ?: Instant.now()
        ).also { if (dto.serverId != null) it.serverId = dto.serverId }
        return metricRepo.save(metric).toDto()
    }

    @Transactional
    fun createCustomType(dto: CustomMetricTypeDto, firebaseUid: String): CustomMetricTypeDto {
        val type = CustomMetricType(firebaseUid = firebaseUid, name = dto.name, unit = dto.unit, icon = dto.icon)
            .also { if (dto.serverId != null) it.serverId = dto.serverId }
        return customTypeRepo.save(type).toDto()
    }

    @Transactional
    fun delete(id: Long, firebaseUid: String) {
        val metric = metricRepo.findById(id).orElseThrow()
        require(metric.firebaseUid == firebaseUid) { "Forbidden" }
        metricRepo.delete(metric)
    }

    fun since(firebaseUid: String, since: Instant): List<HealthMetricDto> =
        metricRepo.findByFirebaseUidAndUpdatedAtAfter(firebaseUid, since).map { it.toDto() }

    @Transactional
    fun upsert(dto: HealthMetricDto, firebaseUid: String): HealthMetricDto {
        val existing = dto.serverId?.let { metricRepo.findByServerId(it) }
        if (existing == null) return create(dto, firebaseUid)
        if ((dto.updatedAt ?: Instant.EPOCH) <= existing.updatedAt) return existing.toDto()
        val updated = HealthMetric(
            id = existing.id, firebaseUid = existing.firebaseUid, type = dto.type, subType = dto.subType,
            value = dto.value, secondaryValue = dto.secondaryValue, unit = dto.unit,
            recordedAt = dto.recordedAt ?: existing.recordedAt
        ).also { it.serverId = existing.serverId }
        return metricRepo.save(updated).toDto()
    }
}
