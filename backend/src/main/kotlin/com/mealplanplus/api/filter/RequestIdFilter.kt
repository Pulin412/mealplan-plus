package com.mealplanplus.api.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Assigns every request a correlation id and exposes it two ways:
 *  - MDC key `requestId` — picked up by the JSON log encoder (prod) and the console pattern (dev),
 *    so all log lines for one request share an id.
 *  - `X-Request-Id` response header — so a client/support can quote it in a bug report.
 *
 * Precedence: a caller-supplied `X-Request-Id`, else Cloud Run's `X-Cloud-Trace-Context` trace id
 * (the part before the first `/`), else a random UUID. Never reads the Authorization header.
 *
 * Registered at HIGHEST_PRECEDENCE (see [com.mealplanplus.api.config.LoggingConfig]) so the id is
 * in the MDC before any other filter — including auth — can log.
 */
class RequestIdFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain,
    ) {
        val id = request.getHeader(REQUEST_ID_HEADER)?.takeIf { it.isNotBlank() }
            ?: request.getHeader(CLOUD_TRACE_HEADER)?.substringBefore('/')?.takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()

        MDC.put(MDC_KEY, id)
        response.setHeader(REQUEST_ID_HEADER, id)
        try {
            chain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_KEY)
        }
    }

    companion object {
        const val MDC_KEY = "requestId"
        const val REQUEST_ID_HEADER = "X-Request-Id"
        const val CLOUD_TRACE_HEADER = "X-Cloud-Trace-Context"
    }
}
