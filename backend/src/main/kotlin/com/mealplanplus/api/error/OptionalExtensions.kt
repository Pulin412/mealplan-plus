package com.mealplanplus.api.error

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

/**
 * Unwrap a repository `findById` result, turning a missing row into a clean **404 NOT_FOUND**
 * (mapped to a user-safe message by [GlobalExceptionHandler]) instead of a bare
 * `NoSuchElementException`, which would fall through as a generic 500.
 *
 * [what] names the resource for the log/reason, e.g. `"Diet"` → `"Diet not found"`.
 */
fun <T> Optional<T>.orNotFound(what: String): T =
    orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "$what not found") }
