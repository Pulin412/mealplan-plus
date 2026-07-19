package com.mealplanplus.data.sync

import com.mealplanplus.data.generated.api.SyncApi
import com.mealplanplus.data.generated.model.SyncPushRequest
import com.mealplanplus.data.generated.model.TombstoneDto
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.repository.toDto
import com.mealplanplus.data.repository.toEntity
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The sole network boundary for domain data. Repositories only ever touch Room; this
 * pushes local changes up and pulls server changes down via the backend's delta-sync
 * endpoints. Covers Foods today; more entity types plug into the same push/pull as their
 * screens are built.
 */
@Singleton
class SyncManager @Inject constructor(
    private val syncApi: SyncApi,
    private val foodDao: FoodDao,
    private val cursor: SyncCursorStore,
) {
    /** Push dirty rows, then pull changes since the cursor. Best-effort; safe to call often. */
    suspend fun sync(): Result<Unit> = runCatching {
        push()
        pull()
    }

    private suspend fun push() {
        val dirty = foodDao.getDirty()
        if (dirty.isEmpty()) return
        val (deleted, live) = dirty.partition { it.deletedAt != null }
        val req = SyncPushRequest(
            foods = live.map { it.toDto() },
            tombstones = deleted.map {
                TombstoneDto(
                    entityType = "food",
                    serverId = UUID.fromString(it.id),
                    deletedAt = Instant.ofEpochMilli(it.deletedAt!!),
                )
            },
        )
        val resp = syncApi.syncPush(req)
        if (!resp.isSuccessful) return
        foodDao.clearDirty(live.map { it.id })      // pushed edits are now clean
        foodDao.hardDelete(deleted.map { it.id })   // synced tombstones can be removed
    }

    private suspend fun pull() {
        val resp = syncApi.syncPull(cursor.get()).body() ?: return
        // Never clobber un-pushed local edits: skip rows still dirty locally.
        val dirty = foodDao.dirtyIds().toSet()
        val incoming = resp.foods
            .filter { it.serverId?.toString() !in dirty }
            .map { it.toEntity() }
        if (incoming.isNotEmpty()) foodDao.upsertAll(incoming)
        // Apply server-side deletes.
        val tombstoned = resp.tombstones
            .filter { it.entityType == "food" }
            .map { it.serverId.toString() }
        if (tombstoned.isNotEmpty()) foodDao.hardDelete(tombstoned)
        cursor.set(resp.serverTime)
    }
}
