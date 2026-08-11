package com.mealplanplus.data.remote

import com.google.gson.Gson
import com.mealplanplus.data.generated.model.ApiError
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * Turns an API failure — a non-successful [Response], a caught [Throwable], or a Retrofit
 * [HttpException] — into a single user-safe message.
 *
 * Mirrors the web client's `errors.ts`: the backend sends a stable `code` in its [ApiError]
 * envelope and each client owns the friendly copy for that code (offline-safe, consistent tone).
 * The server's own `message` is only a last-resort fallback; the raw status/stacktrace is never shown.
 * On a 5xx we append the `requestId` so a support ticket is traceable to the logged incident.
 */
object ApiErrors {

    private val gson: Gson by lazy { apiGson() }

    private val CODE_MESSAGES = mapOf(
        "VALIDATION" to "Please check your input and try again.",
        "BAD_REQUEST" to "That request wasn't valid. Please check your input and try again.",
        "UNAUTHORIZED" to "Your session expired. Please sign in again.",
        "FORBIDDEN" to "You don't have access to this.",
        "NOT_FOUND" to "We couldn't find what you were looking for.",
        "CONFLICT" to "That conflicts with something that already exists.",
        "INTERNAL" to "Something went wrong on our end. Please try again.",
        "SERVICE_UNAVAILABLE" to "The service is busy right now. Please try again in a moment.",
    )
    private const val OFFLINE = "You appear to be offline. Check your connection and try again."
    private const val UNKNOWN = "Something went wrong. Please try again."

    /** Message for a non-successful Retrofit [Response] (the common shape in this app's repos). */
    fun messageFor(response: Response<*>): String {
        val parsed = parse(runCatching { response.errorBody()?.string() }.getOrNull())
        return render(parsed, response.code())
    }

    /** Message for a caught throwable (IOException = offline; HttpException = parse its body). */
    fun messageFor(t: Throwable): String = when (t) {
        is IOException -> OFFLINE
        is HttpException -> {
            val parsed = parse(runCatching { t.response()?.errorBody()?.string() }.getOrNull())
            render(parsed, t.code())
        }
        else -> t.message?.takeIf { it.isNotBlank() } ?: UNKNOWN
    }

    private fun parse(body: String?): ApiError? =
        body?.takeIf { it.isNotBlank() }
            ?.let { runCatching { gson.fromJson(it, ApiError::class.java) }.getOrNull() }
            ?.takeIf { it.code.isNotBlank() }

    private fun render(parsed: ApiError?, status: Int): String {
        val code = parsed?.code ?: codeForStatus(status)
        val base = CODE_MESSAGES[code] ?: UNKNOWN
        val ref = parsed?.requestId?.takeIf { it.isNotBlank() }
        return if (status >= 500 && ref != null) "$base (Ref: $ref)" else base
    }

    private fun codeForStatus(status: Int): String = when (status) {
        400 -> "BAD_REQUEST"
        401 -> "UNAUTHORIZED"
        403 -> "FORBIDDEN"
        404 -> "NOT_FOUND"
        409 -> "CONFLICT"
        503 -> "SERVICE_UNAVAILABLE"
        else -> if (status >= 500) "INTERNAL" else "UNKNOWN"
    }
}
