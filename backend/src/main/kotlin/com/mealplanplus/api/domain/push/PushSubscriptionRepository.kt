package com.mealplanplus.api.domain.push

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional

interface PushSubscriptionRepository : JpaRepository<PushSubscription, Long> {
    fun findByEndpoint(endpoint: String): PushSubscription?

    @Transactional
    fun deleteByEndpoint(endpoint: String)
}
