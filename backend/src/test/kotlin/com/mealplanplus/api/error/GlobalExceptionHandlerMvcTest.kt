package com.mealplanplus.api.error

import com.mealplanplus.api.filter.RequestIdFilter
import org.hamcrest.Matchers.matchesPattern
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

/**
 * Proves the [GlobalExceptionHandler] is wired as advice over a real request path: it serializes the
 * [com.mealplanplus.api.generated.model.ApiError] envelope as JSON, the [RequestIdFilter] stamps a
 * matching `X-Request-Id`, and a 5xx body never leaks the exception detail.
 */
class GlobalExceptionHandlerMvcTest {

    @RestController
    class BoomController {
        @GetMapping("/boom") fun boom(): Nothing = throw IllegalStateException("secret stacktrace detail")
        @GetMapping("/forbidden") fun forbidden(): Nothing =
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Not your resource")
    }

    private val mvc: MockMvc = MockMvcBuilders
        .standaloneSetup(BoomController())
        .setControllerAdvice(GlobalExceptionHandler())
        .addFilters<org.springframework.test.web.servlet.setup.StandaloneMockMvcBuilder>(RequestIdFilter())
        .build()

    @Test
    fun `unhandled exception returns a clean ApiError 500 with a request id and no leak`() {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/boom"))
            .andExpect(status().isInternalServerError)
            .andExpect(header().string(RequestIdFilter.REQUEST_ID_HEADER, matchesPattern(".+")))
            .andExpect(jsonPath("$.code").value("INTERNAL"))
            .andExpect(jsonPath("$.requestId").value(matchesPattern(".+")))
            .andExpect(jsonPath("$.path").value("/boom"))
            .andExpect(jsonPath("$.message").value(not("")))
            .andExpect(jsonPath("$.message").value(not(matchesPattern(".*secret.*"))))
    }

    @Test
    fun `a thrown 4xx keeps its status, code and safe reason`() {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/forbidden"))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("Not your resource"))
    }
}
