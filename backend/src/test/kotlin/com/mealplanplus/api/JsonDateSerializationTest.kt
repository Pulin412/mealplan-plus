package com.mealplanplus.api

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.time.LocalDate

/**
 * Guards the API contract: `date-time` and `date` fields must serialize as ISO-8601
 * strings (matching docs/openapi.yaml), NOT epoch millis / [y,m,d] arrays. Enforced by
 * `spring.jackson.serialization.write-dates-as-timestamps: false` in application.yml.
 */
@SpringBootTest
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class JsonDateSerializationTest {

    @Autowired
    private lateinit var mapper: ObjectMapper

    data class DateHolder(val recordedAt: Instant, val date: LocalDate)

    @Test
    fun `dates serialize as ISO-8601 strings, not timestamps`() {
        val json = mapper.writeValueAsString(
            DateHolder(
                recordedAt = Instant.parse("2026-07-22T10:15:30Z"),
                date = LocalDate.of(2026, 7, 22),
            )
        )
        assertThat(json).contains("\"recordedAt\":\"2026-07-22T10:15:30Z\"")
        assertThat(json).contains("\"date\":\"2026-07-22\"")
    }
}
