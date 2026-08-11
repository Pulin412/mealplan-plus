package com.mealplanplus.data.remote

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

class ApiErrorsTest {

    private fun errorResponse(status: Int, json: String): Response<Unit> =
        Response.error(status, json.toResponseBody("application/json".toMediaType()))

    @Test
    fun `maps a structured server code to friendly copy, not the raw code`() {
        val body = """{"code":"FORBIDDEN","message":"Not your resource","requestId":"abc","timestamp":"2026-08-11T10:00:00Z","path":"/x"}"""
        val msg = ApiErrors.messageFor(errorResponse(403, body))
        assertEquals("You don't have access to this.", msg)
    }

    @Test
    fun `5xx appends the request id so support can trace the incident`() {
        val body = """{"code":"INTERNAL","message":"boom","requestId":"req-42","timestamp":"2026-08-11T10:00:00Z","path":"/x"}"""
        val msg = ApiErrors.messageFor(errorResponse(500, body))
        assertTrue(msg.contains("Ref: req-42"))
        // Never leak the server's raw message on a 5xx.
        assertFalse(msg.contains("boom"))
    }

    @Test
    fun `falls back to the status when the body is not an ApiError envelope`() {
        val msg = ApiErrors.messageFor(errorResponse(404, "<html>Not Found</html>"))
        assertEquals("We couldn't find what you were looking for.", msg)
    }

    @Test
    fun `an IOException is reported as an offline message`() {
        val msg = ApiErrors.messageFor(IOException("connection reset"))
        assertTrue(msg.contains("offline"))
    }

    @Test
    fun `an unknown 5xx status without a body still gives a safe generic message`() {
        val msg = ApiErrors.messageFor(errorResponse(502, ""))
        assertEquals("Something went wrong on our end. Please try again.", msg)
    }
}
