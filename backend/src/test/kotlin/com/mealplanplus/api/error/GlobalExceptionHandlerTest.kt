package com.mealplanplus.api.error

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.mealplanplus.api.filter.RequestIdFilter
import net.logstash.logback.argument.StructuredArgument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException

class GlobalExceptionHandlerTest {

    private val handler = GlobalExceptionHandler()
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java) as Logger
    private val appender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setUp() {
        MDC.put(RequestIdFilter.MDC_KEY, "req-123")
        val request = MockHttpServletRequest("GET", "/api/v1/diets")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(appender)
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
    fun `a 5xx logs the exception class, stacktrace, and structured exception fields for alerting`() {
        // A null-message exception (like an NPE) is exactly the case where the class must carry the signal.
        handler.handleUnexpected(NullPointerException())

        val event = appender.list.single { it.level == Level.ERROR }
        // The exception CLASS is in the message even though the message is null.
        assertThat(event.formattedMessage).contains("java.lang.NullPointerException")
        // The full stacktrace rides along as the throwable.
        assertThat(event.throwableProxy).isNotNull
        assertThat(event.throwableProxy.className).isEqualTo("java.lang.NullPointerException")
        // Structured fields exception_class / exception_message are attached for the log-based alert to surface.
        val structured = event.argumentArray.orEmpty().filterIsInstance<StructuredArgument>().map { it.toString() }
        assertThat(structured).anySatisfy { assertThat(it).contains("exception_class").contains("NullPointerException") }
        assertThat(structured).anySatisfy { assertThat(it).contains("exception_message") }
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
