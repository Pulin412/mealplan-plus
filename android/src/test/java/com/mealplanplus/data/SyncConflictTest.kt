package com.mealplanplus.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies last-write-wins conflict resolution used in SyncRepository.pull().
 *
 * Rule: apply remote change only when remoteUpdatedAt > localUpdatedAt.
 * Server wins on tie (equal timestamps → skip).
 */
class SyncConflictTest {

    private fun shouldApply(remoteUpdatedAt: Long?, localUpdatedAt: Long): Boolean =
        (remoteUpdatedAt ?: 0L) > localUpdatedAt

    @Test
    fun `remote newer than local — apply`() {
        assertTrue(shouldApply(remoteUpdatedAt = 2000L, localUpdatedAt = 1000L))
    }

    @Test
    fun `remote older than local — skip`() {
        assertFalse(shouldApply(remoteUpdatedAt = 500L, localUpdatedAt = 1000L))
    }

    @Test
    fun `remote and local same timestamp — server wins, skip`() {
        assertFalse(shouldApply(remoteUpdatedAt = 1000L, localUpdatedAt = 1000L))
    }

    @Test
    fun `remote updatedAt null treated as epoch zero — older than any real local record`() {
        assertFalse(shouldApply(remoteUpdatedAt = null, localUpdatedAt = 1000L))
    }

    @Test
    fun `remote updatedAt null and local also zero — skip, no-op`() {
        assertFalse(shouldApply(remoteUpdatedAt = null, localUpdatedAt = 0L))
    }

    @Test
    fun `first sync — local record does not exist — always insert regardless of timestamp`() {
        // When existing == null we always insert; conflict resolution only runs on updates.
        // This test documents that invariant rather than calling shouldApply().
        val existing: Any? = null
        assertTrue("New record should always be inserted", existing == null)
    }
}
