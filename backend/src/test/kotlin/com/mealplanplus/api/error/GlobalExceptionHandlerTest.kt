package com.mealplanplus.api.error

import com.mealplanplus.api.filter.RequestIdFilter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()

    @BeforeEach
    fun setUp() {
        MDC.put(RequestIdFilter.MDC_KEY, "req-123")
        val request = MockHttpServletRequest("GET", "/api/v1/diets")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @AfterEach
    fun tearDown() {
        MDC.clear()
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun `unexpected exception maps to 500 INTERNAL and never leaks the message`() {
        val ex = IllegalStateException("Sensitive: DB password = hunter2")

        val response = handler.handleUnexpected(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val body = response.body!!
        assertThat(body.code).isEqualTo("INTERNAL")
        assertThat(body.requestId).isEqualTo("req-123")
        assertThat(body.path).isEqualTo("/api/v1/diets")
        // The internal message must NOT reach the client.
        assertThat(body.message).doesNotContain("hunter2").doesNotContain("Sensitive")
        assertThat(body.message).isNotBlank()
    }

    @Test
    fun `4xx ResponseStatusException keeps its status, code and reason`() {
        val ex = ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")

        val response = handler.handleResponseStatus(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        val body = response.body!!
        assertThat(body.code).isEqualTo("FORBIDDEN")
        assertThat(body.message).isEqualTo("Not your resource")
        assertThat(body.requestId).isEqualTo("req-123")
    }

    @Test
    fun `404 maps to NOT_FOUND code`() {
        val response = handler.handleResponseStatus(ResponseStatusException(HttpStatus.NOT_FOUND, "gone"))
        assertThat(response.body!!.code).isEqualTo("NOT_FOUND")
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
    }

    @Test
    fun `409 maps to CONFLICT code`() {
        val response = handler.handleResponseStatus(ResponseStatusException(HttpStatus.CONFLICT, "dup"))
        assertThat(response.body!!.code).isEqualTo("CONFLICT")
    }

    @Test
    fun `a 5xx ResponseStatusException is scrubbed like an unexpected error`() {
        val ex = ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "leaky internal reason")

        val response = handler.handleResponseStatus(ex)

        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        val body = response.body!!
        assertThat(body.code).isEqualTo("INTERNAL")
        assertThat(body.message).doesNotContain("leaky")
    }

    @Test
    fun `missing MDC request id falls back to a placeholder rather than throwing`() {
        MDC.clear()
        val response = handler.handleUnexpected(RuntimeException("boom"))
        assertThat(response.body!!.requestId).isNotBlank()
    }
}
