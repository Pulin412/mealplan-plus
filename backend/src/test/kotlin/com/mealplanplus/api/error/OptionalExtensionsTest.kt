package com.mealplanplus.api.error

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class OptionalExtensionsTest {

    @Test
    fun `orNotFound returns the value when present`() {
        assertThat(Optional.of("diet").orNotFound("Diet")).isEqualTo("diet")
    }

    @Test
    fun `orNotFound throws a 404 (not a bare NoSuchElementException) when empty`() {
        assertThatThrownBy { Optional.empty<String>().orNotFound("Diet") }
            .isInstanceOfSatisfying(ResponseStatusException::class.java) {
                assertThat(it.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
                assertThat(it.reason).isEqualTo("Diet not found")
            }
    }
}
