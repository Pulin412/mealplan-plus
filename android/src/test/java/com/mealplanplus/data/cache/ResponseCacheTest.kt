package com.mealplanplus.data.cache

import app.cash.turbine.test
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.mealplanplus.data.generated.model.HealthMetricDto
import com.mealplanplus.data.local.dao.CachedResponseDao
import com.mealplanplus.data.model.CachedResponse
import com.mealplanplus.data.remote.apiGson
import java.time.Instant
import java.util.UUID
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** A DTO stand-in — apiGson serializes it just like a real generated model. */
private data class SampleDto(val n: Int, val label: String)

/** In-memory [CachedResponseDao] so the cache logic is tested without Room. */
private class FakeCachedResponseDao : CachedResponseDao {
    val rows = mutableMapOf<String, CachedResponse>()
    override suspend fun get(key: String): CachedResponse? = rows[key]
    override fun observe(key: String): Flow<CachedResponse?> = flowOf(rows[key])
    override suspend fun put(row: CachedResponse) { rows[row.key] = row }
    override suspend fun clearAll() { rows.clear() }
}

class ResponseCacheTest {

    private val dao = FakeCachedResponseDao()
    private val auth = mockk<FirebaseAuth> {
        every { currentUser } returns mockk<FirebaseUser> { every { uid } returns "u1" }
    }
    private val cache = ResponseCache(dao, apiGson(), auth)

    @Test
    fun key_isScopedToUidScreenAndParams() {
        assertEquals("u1|dashboard|2026-08-03", cache.key("dashboard", "2026-08-03"))
    }

    @Test
    fun cacheMiss_emitsLoadingNull_thenSuccess_andPersists() = runTest {
        val fresh = SampleDto(1, "fresh")
        cache.stream("dashboard", "d", fetch = { fresh }).test {
            assertNull((awaitItem() as Resource.Loading).data)      // ① nothing cached yet
            assertEquals(fresh, (awaitItem() as Resource.Success).data)  // ② network result
            awaitComplete()
        }
        // ③ persisted for next time, scoped to the uid.
        assertTrue(dao.rows.containsKey("u1|dashboard|d"))
    }

    @Test
    fun cacheHit_emitsCachedInstantly_thenFreshOnSuccess() = runTest {
        dao.rows["u1|dashboard|d"] =
            CachedResponse("u1|dashboard|d", apiGson().toJson(SampleDto(1, "old")), 0L)

        val fresh = SampleDto(2, "new")
        cache.stream("dashboard", "d", fetch = { fresh }).test {
            assertEquals(SampleDto(1, "old"), (awaitItem() as Resource.Loading).data)   // ① instant paint
            assertEquals(fresh, (awaitItem() as Resource.Success).data)                 // ② fresh swap
            awaitComplete()
        }
    }

    @Test
    fun networkFailure_keepsShowingCache_viaError() = runTest {
        dao.rows["u1|dashboard|d"] =
            CachedResponse("u1|dashboard|d", apiGson().toJson(SampleDto(1, "old")), 0L)

        val boom = IllegalStateException("cold start 503")
        cache.stream<SampleDto>("dashboard", "d", fetch = { throw boom }).test {
            assertEquals(SampleDto(1, "old"), (awaitItem() as Resource.Loading).data)
            val err = awaitItem() as Resource.Error
            assertEquals(SampleDto(1, "old"), err.data)   // still usable offline
            assertSame(boom, err.error)
            awaitComplete()
        }
    }

    @Test
    fun roundTrips_realDtoWithInstantAndUuid_throughCache() = runTest {
        // The exact payload shape Health caches: type -> readings, DTOs carry java.time + UUID.
        val reading = HealthMetricDto(
            type = "WEIGHT", value = 72.5, unit = "kg",
            recordedAt = Instant.parse("2026-08-03T10:15:30Z"),
            serverId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
            updatedAt = Instant.parse("2026-08-03T10:15:31Z"),
        )
        val payload = mapOf("WEIGHT" to listOf(reading))

        cache.stream("health.readings", fetch = { payload }).test {
            awaitItem(); awaitItem(); awaitComplete()   // miss → success persists it
        }
        // Next open: the cache-hit payload must deserialize back to an equal object (adapters intact).
        cache.stream("health.readings", fetch = { emptyMap<String, List<HealthMetricDto>>() }).test {
            assertEquals(payload, (awaitItem() as Resource.Loading).data)
            awaitItem(); awaitComplete()
        }
    }

    @Test
    fun clear_dropsEverything() = runTest {
        dao.rows["u1|x|"] = CachedResponse("u1|x|", "{}", 0L)
        cache.clear()
        assertTrue(dao.rows.isEmpty())
    }
}
