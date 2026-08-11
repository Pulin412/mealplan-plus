package com.mealplanplus.api.domain.social

import com.mealplanplus.api.domain.user.User
import com.mealplanplus.api.domain.user.UserRepository
import com.mealplanplus.api.generated.model.ProfileUpdateRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.util.UUID

/**
 * Service-level test for the social notification feed on the H2 dev schema (no Docker).
 * Covers follow generation, share fan-out to followers, the pref/block gates, and read state.
 */
@SpringBootTest
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class NotificationServiceTest {

    @Autowired lateinit var social: SocialService
    @Autowired lateinit var notifications: NotificationService
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var follows: FollowRepository
    @Autowired lateinit var blocks: BlockRepository
    @Autowired lateinit var notificationRepo: NotificationRepository

    private val alice = "uid-alice"
    private val bob = "uid-bob"
    private val carol = "uid-carol"

    @BeforeEach
    fun setUp() {
        notificationRepo.deleteAll(); follows.deleteAll(); blocks.deleteAll(); users.deleteAll()
        users.save(User(firebaseUid = alice, displayName = "Alice"))
        users.save(User(firebaseUid = bob, displayName = "Bob"))
        users.save(User(firebaseUid = carol, displayName = "Carol"))
        social.updateMyProfile(alice, ProfileUpdateRequest(handle = "alice"))
        social.updateMyProfile(bob, ProfileUpdateRequest(handle = "bob"))
        social.updateMyProfile(carol, ProfileUpdateRequest(handle = "carol"))
    }

    @Test
    fun `following generates a FOLLOW notification for the followee`() {
        social.followUser(alice, "bob")

        val feed = notifications.list(bob, 50)
        assertEquals(1, feed.items.size)
        assertEquals(1, feed.unreadCount)
        val n = feed.items.single()
        assertEquals("FOLLOW", n.type.value)
        assertEquals("alice", n.actorHandle)
        assertFalse(n.read)
    }

    @Test
    fun `re-following is idempotent and does not duplicate the notification`() {
        social.followUser(alice, "bob")
        social.followUser(alice, "bob")
        assertEquals(1, notifications.list(bob, 50).items.size)
    }

    @Test
    fun `sharing fans out to followers only, not to strangers`() {
        social.followUser(bob, "alice")   // bob follows alice
        // carol does NOT follow alice

        notifications.notifyShare(alice, NotificationSubjectKind.DIET, UUID.randomUUID(), "Cut Plan")

        val bobFeed = notifications.list(bob, 50)
        assertEquals(1, bobFeed.items.size)
        val n = bobFeed.items.single()
        assertEquals("SHARE", n.type.value)
        assertEquals("DIET", n.subjectKind!!.value)
        assertEquals("Cut Plan", n.subjectName)
        // carol received nothing.
        assertTrue(notifications.list(carol, 50).items.isEmpty())
    }

    @Test
    fun `recipient with notifications disabled receives nothing`() {
        notifications.setPrefs(bob, enabled = false)
        social.followUser(alice, "bob")
        assertTrue(notifications.list(bob, 50).items.isEmpty())
    }

    @Test
    fun `a mutual block suppresses follow notifications`() {
        social.blockUser(bob, "alice")   // bob blocks alice; follow is then rejected anyway,
        // but notifyFollow is independently gated — assert directly.
        notifications.notifyFollow(actorUid = alice, followeeUid = bob)
        assertTrue(notifications.list(bob, 50).items.isEmpty())
    }

    @Test
    fun `marking read clears the unread count`() {
        social.followUser(alice, "bob")
        assertEquals(1, notifications.list(bob, 50).unreadCount)

        notifications.markAllRead(bob)
        val feed = notifications.list(bob, 50)
        assertEquals(0, feed.unreadCount)
        assertTrue(feed.items.single().read)
    }

    @Test
    fun `prefs default to enabled and round-trip`() {
        assertTrue(notifications.getPrefs(alice).enabled)
        notifications.setPrefs(alice, enabled = false)
        assertFalse(notifications.getPrefs(alice).enabled)
    }
}
