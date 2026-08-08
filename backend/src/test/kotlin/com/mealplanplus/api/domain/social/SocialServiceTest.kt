package com.mealplanplus.api.domain.social

import com.mealplanplus.api.domain.user.User
import com.mealplanplus.api.domain.user.UserRepository
import com.mealplanplus.api.generated.model.ProfileUpdateRequest
import com.mealplanplus.api.generated.model.ReportRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.web.server.ResponseStatusException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertThrows

/**
 * Service-level integration test for the social foundation (P0) on the H2 dev schema.
 * Exercises handle claiming/validation, the follow graph, and block/report semantics
 * without needing Docker, so it runs in CI.
 */
@SpringBootTest
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class SocialServiceTest {

    @Autowired lateinit var service: SocialService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var follows: FollowRepository
    @Autowired lateinit var blocks: BlockRepository
    @Autowired lateinit var reports: ContentReportRepository

    private val alice = "uid-alice"
    private val bob = "uid-bob"

    @BeforeEach
    fun setUp() {
        reports.deleteAll(); follows.deleteAll(); blocks.deleteAll(); users.deleteAll()
        users.save(User(firebaseUid = alice, displayName = "Alice"))
        users.save(User(firebaseUid = bob, displayName = "Bob"))
    }

    private fun claim(uid: String, handle: String) =
        service.updateMyProfile(uid, ProfileUpdateRequest(handle = handle))

    // ── Handle claiming ────────────────────────────────────────────────────────

    @Test
    fun `claiming a handle normalises to lowercase`() {
        val res = claim(alice, "Alice_01")
        assertEquals("alice_01", res.handle)
        assertEquals("alice_01", users.findByFirebaseUid(alice)!!.handle)
    }

    @Test
    fun `invalid handle is rejected with 400`() {
        val ex = assertThrows<ResponseStatusException> { claim(alice, "ab") }   // too short
        assertEquals(HttpStatus.BAD_REQUEST, ex.statusCode)
        assertThrows<ResponseStatusException> { claim(alice, "bad handle!") }   // illegal chars
    }

    @Test
    fun `duplicate handle is rejected with 409, case-insensitively`() {
        claim(alice, "shared")
        val ex = assertThrows<ResponseStatusException> { claim(bob, "SHARED") }
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
    }

    @Test
    fun `re-claiming your own handle is a no-op, not a conflict`() {
        claim(alice, "alice")
        val res = claim(alice, "alice")   // same handle again
        assertEquals("alice", res.handle)
    }

    @Test
    fun `handle availability reflects format and uniqueness`() {
        claim(alice, "taken")
        assertFalse(service.checkHandleAvailable(bob, "taken").available)
        assertTrue(service.checkHandleAvailable(bob, "free_one").available)
        val bad = service.checkHandleAvailable(bob, "no")
        assertFalse(bad.valid); assertFalse(bad.available)
        // Your own handle reads as available to you.
        assertTrue(service.checkHandleAvailable(alice, "taken").available)
    }

    // ── Follow graph ───────────────────────────────────────────────────────────

    @Test
    fun `follow then unfollow updates counts and flags`() {
        claim(alice, "alice"); claim(bob, "bob")

        service.followUser(alice, "bob")
        service.followUser(alice, "bob")   // idempotent

        val bobProfile = service.getPublicProfile(alice, "bob")
        assertEquals(1L, bobProfile.followerCount)
        assertTrue(bobProfile.isFollowedByMe)
        // Alice follows exactly one person (bob).
        assertEquals(1L, service.getPublicProfile(alice, "alice").followingCount)

        service.unfollowUser(alice, "bob")
        assertFalse(service.getPublicProfile(alice, "bob").isFollowedByMe)
    }

    @Test
    fun `cannot follow yourself`() {
        claim(alice, "alice")
        val ex = assertThrows<ResponseStatusException> { service.followUser(alice, "alice") }
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
    }

    @Test
    fun `following an unknown handle 404s`() {
        val ex = assertThrows<ResponseStatusException> { service.followUser(alice, "ghost") }
        assertEquals(HttpStatus.NOT_FOUND, ex.statusCode)
    }

    // ── Block / report ───────────────────────────────────────────────────────

    @Test
    fun `blocking severs follows both ways and prevents re-follow`() {
        claim(alice, "alice"); claim(bob, "bob")
        service.followUser(alice, "bob")
        service.followUser(bob, "alice")

        service.blockUser(alice, "bob")

        assertFalse(follows.existsByFollowerUidAndFolloweeUid(alice, bob))
        assertFalse(follows.existsByFollowerUidAndFolloweeUid(bob, alice))
        val ex = assertThrows<ResponseStatusException> { service.followUser(alice, "bob") }
        assertEquals(HttpStatus.CONFLICT, ex.statusCode)
    }

    @Test
    fun `blocked user is hidden from search and profile`() {
        claim(alice, "alice"); claim(bob, "bob")
        service.blockUser(alice, "bob")

        assertTrue(service.searchUsers(alice, "bob").isEmpty())
        assertTrue(service.searchUsers(bob, "alice").isEmpty())
        val ex = assertThrows<ResponseStatusException> { service.getPublicProfile(alice, "bob") }
        assertEquals(HttpStatus.FORBIDDEN, ex.statusCode)
    }

    @Test
    fun `search excludes self and non-searchable users`() {
        claim(alice, "alice")
        service.updateMyProfile(bob, ProfileUpdateRequest(handle = "bob", isSearchable = false))

        assertTrue(service.searchUsers(alice, "alice").isEmpty())  // excludes self
        assertTrue(service.searchUsers(alice, "bob").isEmpty())    // bob opted out
    }

    @Test
    fun `report resolves the reported handle to a uid and persists`() {
        claim(bob, "bob")
        service.report(alice, ReportRequest(entityType = ReportRequest.EntityType.USER, reportedHandle = "bob", reason = "spam"))
        val saved = reports.findAll().single()
        assertEquals(alice, saved.reporterUid)
        assertEquals(bob, saved.reportedUid)
        assertEquals("USER", saved.entityType)
    }
}
