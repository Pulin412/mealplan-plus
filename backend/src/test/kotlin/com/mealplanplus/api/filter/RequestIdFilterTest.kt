package com.mealplanplus.api.filter

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class RequestIdFilterTest {

    private val filter = RequestIdFilter()

    @Test
    fun `generates an id, exposes it during the chain and via the header, then clears the MDC`() {
        val req = MockHttpServletRequest()
        val res = MockHttpServletResponse()
        var midChain: String? = null

        filter.doFilter(req, res) { _, _ -> midChain = MDC.get(RequestIdFilter.MDC_KEY) }

        assertThat(midChain).isNotBlank()
        assertThat(res.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo(midChain)
        // Must not leak onto the thread once the request is done.
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull()
    }

    @Test
    fun `prefers a caller-supplied X-Request-Id`() {
        val req = MockHttpServletRequest().apply { addHeader(RequestIdFilter.REQUEST_ID_HEADER, "caller-123") }
        val res = MockHttpServletResponse()

        filter.doFilter(req, res) { _, _ -> }

        assertThat(res.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("caller-123")
    }

    @Test
    fun `falls back to the Cloud Run trace id (part before the slash)`() {
        val req = MockHttpServletRequest().apply {
            addHeader(RequestIdFilter.CLOUD_TRACE_HEADER, "abc123trace/9876543210;o=1")
        }
        val res = MockHttpServletResponse()

        filter.doFilter(req, res) { _, _ -> }

        assertThat(res.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("abc123trace")
    }
}
