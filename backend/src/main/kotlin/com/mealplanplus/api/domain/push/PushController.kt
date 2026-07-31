package com.mealplanplus.api.domain.push

import com.mealplanplus.api.generated.api.PushApi
import com.mealplanplus.api.generated.model.PushSubscriptionRequest
import com.mealplanplus.api.generated.model.PushUnsubscribeRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.RestController

@RestController
class PushController(private val service: PushNotificationService) : PushApi {

    override fun subscribePush(pushSubscriptionRequest: PushSubscriptionRequest): ResponseEntity<Unit> {
        service.subscribe(
            uid = currentUid(),
            endpoint = pushSubscriptionRequest.endpoint,
            p256dh = pushSubscriptionRequest.propertyKeys.p256dh,
            auth = pushSubscriptionRequest.propertyKeys.auth,
            userAgent = pushSubscriptionRequest.userAgent,
        )
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    override fun unsubscribePush(pushUnsubscribeRequest: PushUnsubscribeRequest): ResponseEntity<Unit> {
        service.unsubscribe(pushUnsubscribeRequest.endpoint)
        return ResponseEntity.noContent().build()
    }

    private fun currentUid() = SecurityContextHolder.getContext().authentication.name
}
