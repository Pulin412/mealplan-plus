package com.mealplanplus.api.error

import com.mealplanplus.api.filter.RequestIdFilter
import com.mealplanplus.api.generated.model.ApiError
import io.sentry.Sentry
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

/**
 * Single place every unhandled/thrown exception is turned into the stable, user-safe [ApiError]
 * envelope (see `docs/openapi.yaml#ApiError`). Guarantees:
 *  - **No leak.** 5xx bodies never contain the exception message/stacktrace — only a generic,
 *    code-keyed message. 4xx keep the service-provided `reason` (those are deliberately safe).
 *  - **Traceable.** Every body carries the `requestId` from the MDC (same value the
 *    [RequestIdFilter] returns as `X-Request-Id`), so a user-quoted code maps 1:1 to the logs.
 *  - **Observed.** 5xx are logged at ERROR with the full stacktrace and captured to Sentry
 *    (a no-op when `SENTRY_DSN` is unset — locally and in tests).
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatus(ex: ResponseStatusException): ResponseEntity<ApiError> {
        val status = HttpStatus.valueOf(ex.statusCode.value())
        // A 5xx thrown as a ResponseStatusException is still an unexpected server fault — scrub it.
        return if (status.is5xxServerError) unexpected(ex, status)
        else respond(status, codeFor(status), ex.reason, ex, unexpected = false)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiError> {
        val detail = ex.bindingResult.fieldErrors.firstOrNull()
            ?.let { "${it.field}: ${it.defaultMessage}" }
        return respond(HttpStatus.BAD_REQUEST, "VALIDATION", detail, ex, unexpected = false)
    }

    /** Malformed / unparseable request body or a bad path/query type is the caller's fault → 400, not 500. */
    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class)
    fun handleBadInput(ex: Exception): ResponseEntity<ApiError> =
        respond(HttpStatus.BAD_REQUEST, "BAD_REQUEST", reason = null, ex, unexpected = false)

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ApiError> =
        unexpected(ex, HttpStatus.INTERNAL_SERVER_ERROR)

    private fun unexpected(ex: Exception, status: HttpStatus): ResponseEntity<ApiError> {
        // Tag the Sentry event with the same requestId the user sees, so an incident is searchable
        // by the code quoted in a support ticket. Whole block is a no-op when the DSN is unset.
        Sentry.withScope { scope ->
            MDC.get(RequestIdFilter.MDC_KEY)?.takeIf { it.isNotBlank() }?.let { scope.setTag("request_id", it) }
            Sentry.captureException(ex)
        }
        return respond(status, codeFor(status), reason = null, ex, unexpected = true)
    }

    private fun respond(
        status: HttpStatus,
        code: String,
        reason: String?,
        ex: Exception,
        unexpected: Boolean,
    ): ResponseEntity<ApiError> {
        val requestId = MDC.get(RequestIdFilter.MDC_KEY)?.takeIf { it.isNotBlank() } ?: "unknown"
        val path = currentPath()
        if (unexpected) log.error("[{}] {} {} → {}", requestId, status.value(), path, ex.message, ex)
        else log.warn("[{}] {} {} → {}", requestId, status.value(), path, ex.message)

        // 5xx: never echo the exception message. 4xx: a service-supplied reason is safe to show.
        val message = if (unexpected) genericMessage(status)
        else reason?.takeIf { it.isNotBlank() } ?: genericMessage(status)

        val body = ApiError(
            code = code,
            message = message,
            requestId = requestId,
            timestamp = Instant.now(),
            path = path,
        )
        return ResponseEntity.status(status).body(body)
    }

    private fun currentPath(): String =
        (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)
            ?.request?.requestURI ?: "-"

    private fun codeFor(status: HttpStatusCode): String = when (status.value()) {
        400 -> "BAD_REQUEST"
        401 -> "UNAUTHORIZED"
        403 -> "FORBIDDEN"
        404 -> "NOT_FOUND"
        409 -> "CONFLICT"
        503 -> "SERVICE_UNAVAILABLE"
        else -> if (HttpStatus.valueOf(status.value()).is5xxServerError) "INTERNAL" else "BAD_REQUEST"
    }

    private fun genericMessage(status: HttpStatus): String = when (status.value()) {
        400 -> "That request wasn't valid. Please check your input and try again."
        401 -> "Please sign in and try again."
        403 -> "You don't have access to this."
        404 -> "We couldn't find what you were looking for."
        409 -> "That conflicts with something that already exists."
        503 -> "The service is temporarily unavailable. Please try again shortly."
        else -> "Something went wrong on our end. Please try again."
    }
}
