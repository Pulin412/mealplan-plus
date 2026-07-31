package com.mealplanplus.api.domain.push

import com.mealplanplus.api.generated.api.InternalApi
import com.mealplanplus.api.generated.model.ReminderRunResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

/**
 * Scheduler-facing endpoint (GitHub Actions cron), NOT a user route. Guarded by a shared secret in
 * the `X-Reminder-Token` header — the path is `permitAll` in SecurityConfig so no Firebase JWT is
 * required, and we authenticate the caller here.
 */
@RestController
class ReminderController(
    private val service: PushNotificationService,
    @Value("\${push.reminder-token:}") private val reminderToken: String,
) : InternalApi {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun runReminders(xReminderToken: String): ResponseEntity<ReminderRunResponse> {
        if (reminderToken.isBlank() || xReminderToken != reminderToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val (checked, sent, pruned) = service.runReminders()
        log.info("Reminder run: checked=$checked sent=$sent pruned=$pruned")
        return ResponseEntity.ok(ReminderRunResponse(checked = checked, sent = sent, pruned = pruned))
    }
}
