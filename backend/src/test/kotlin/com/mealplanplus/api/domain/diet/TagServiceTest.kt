package com.mealplanplus.api.domain.diet

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional

/**
 * Tag creation is per-entity-type. Regression guard for the bug where creating a tag whose name already
 * existed under a *different* entity type reused that tag, so it never appeared in the target type's list
 * ("created a tag but didn't see it").
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class TagServiceTest {

    @Autowired lateinit var service: DietService
    private val uid = "uid-tag-test"

    @Test
    fun `same name under a different entity type creates a distinct tag, visible in its own list`() {
        val dietTag = service.createTag("Legs", null, uid, TagEntityType.DIET)
        val exerciseTag = service.createTag("Legs", null, uid, TagEntityType.EXERCISE)

        // Distinct rows — the exercise tag is NOT the diet tag reused.
        assertNotEquals(dietTag.id, exerciseTag.id)

        // Each is listed only under its own entity type.
        val exerciseTags = service.listTags(uid, TagEntityType.EXERCISE)
        val dietTags = service.listTags(uid, TagEntityType.DIET)
        assertTrue(exerciseTags.any { it.id == exerciseTag.id }, "new EXERCISE tag must appear in the exercise list")
        assertTrue(dietTags.none { it.id == exerciseTag.id }, "EXERCISE tag must not leak into the diet list")
        assertTrue(dietTags.any { it.id == dietTag.id })
    }

    @Test
    fun `same name and same entity type reuses the existing tag`() {
        val first = service.createTag("Push", null, uid, TagEntityType.WORKOUT)
        val second = service.createTag("Push", null, uid, TagEntityType.WORKOUT)
        assertEquals(first.id, second.id, "same name + type should reuse, not duplicate")
    }
}
