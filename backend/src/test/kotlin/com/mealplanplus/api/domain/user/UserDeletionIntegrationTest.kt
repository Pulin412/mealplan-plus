package com.mealplanplus.api.domain.user

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Real-Postgres integration test for account deletion (right-to-erasure).
 *
 * Runs the actual Flyway migrations (V1..V4) on a throwaway pgvector Postgres so the FK
 * `ON DELETE CASCADE` rules — which the H2 dev schema does NOT reproduce — are exercised for real.
 * Asserts that [UserService.deleteMe] removes the user and their owned rows, cascades child rows,
 * and leaves shared system data (the V3-seeded system foods) untouched.
 *
 * Needs a Docker daemon (Testcontainers), so it is skipped in CI (GitHub Actions sets CI=true) and
 * meant to be run locally / on demand: `./gradlew :backend:test`.
 */
@SpringBootTest
@Testcontainers
@DisabledIfEnvironmentVariable(named = "CI", matches = "true")
class UserDeletionIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"),
        ).withDatabaseName("mealplanplus").withUsername("mealplan").withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            registry.add("spring.datasource.hikari.data-source-properties.stringtype") { "unspecified" }
            registry.add("spring.jpa.hibernate.ddl-auto") { "validate" }
            registry.add("spring.jpa.database-platform") { "org.hibernate.dialect.PostgreSQLDialect" }
            registry.add("spring.flyway.enabled") { "true" }
            registry.add("spring.flyway.locations") { "classpath:db/migration" }
        }
    }

    @Autowired lateinit var userService: UserService
    @Autowired lateinit var jdbc: JdbcTemplate

    private fun count(sql: String, vararg args: Any): Int =
        jdbc.queryForObject(sql, Int::class.java, *args) ?: 0

    @Test
    fun `deleteMe erases the user and cascades children, leaving system data intact`() {
        val uid = "IT_DELETE_UID"
        val systemFoodsBefore = count("SELECT count(*) FROM foods WHERE firebase_uid IS NULL OR firebase_uid = ''")

        // Seed: a user + an owned food + an owned diet + a child diet_food_item (marked quantity=999).
        jdbc.update(
            "INSERT INTO users(firebase_uid, created_at, updated_at, consented_at, privacy_policy_version, onboarding_completed_at) " +
                "VALUES (?, now(), now(), now(), '2026-07-29', now())",
            uid,
        )
        jdbc.update(
            "INSERT INTO foods(name, created_at, updated_at, server_id, firebase_uid) " +
                "VALUES ('IT Food', now(), now(), gen_random_uuid(), ?)",
            uid,
        )
        jdbc.update(
            "INSERT INTO diets(firebase_uid, name, created_at, updated_at, server_id) " +
                "VALUES (?, 'IT Diet', now(), now(), gen_random_uuid())",
            uid,
        )
        jdbc.update(
            "INSERT INTO diet_food_items(diet_id, food_id, slot, quantity, unit) " +
                "SELECT d.id, f.id, 'BREAKFAST', 999, 'GRAM' FROM diets d, foods f " +
                "WHERE d.firebase_uid = ? AND f.firebase_uid = ?",
            uid, uid,
        )

        assertEquals(1, count("SELECT count(*) FROM users WHERE firebase_uid = ?", uid))
        assertEquals(1, count("SELECT count(*) FROM diet_food_items WHERE quantity = 999"))

        // Act
        userService.deleteMe(uid)

        // Owned rows gone
        assertEquals(0, count("SELECT count(*) FROM users WHERE firebase_uid = ?", uid), "user row remains")
        assertEquals(0, count("SELECT count(*) FROM foods WHERE firebase_uid = ?", uid), "owned food remains")
        assertEquals(0, count("SELECT count(*) FROM diets WHERE firebase_uid = ?", uid), "owned diet remains")
        // Child row cascaded away with its diet
        assertEquals(0, count("SELECT count(*) FROM diet_food_items WHERE quantity = 999"), "child diet_food_item not cascaded")
        // Shared system data untouched
        assertEquals(
            systemFoodsBefore,
            count("SELECT count(*) FROM foods WHERE firebase_uid IS NULL OR firebase_uid = ''"),
            "system foods were affected",
        )
    }
}
