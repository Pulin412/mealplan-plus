package com.mealplanplus.data.cache

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mealplanplus.data.local.dao.CachedResponseDao
import com.mealplanplus.data.model.CachedResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.lang.reflect.Type
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read-through cache for server-computed read-models. [stream] emits the cached value first (so a
 * screen paints instantly on open, even during a Cloud Run cold start), then hits the network in
 * the background and emits the fresh value — persisting it for next time. On failure it re-emits
 * the cache, so the screen keeps showing the last-known data offline / while the server wakes.
 *
 * Writes still go server-first through the existing APIs; this only accelerates reads. When a
 * domain later needs offline editing it graduates to a normalized table + sync (path B) — this
 * helper is a strict subset of that, so nothing here is throwaway.
 */
@Singleton
class ResponseCache @Inject constructor(
    private val dao: CachedResponseDao,
    private val gson: Gson,
    private val auth: FirebaseAuth,
) {
    /**
     * Stream a cached read for [screen] (+ optional [params], e.g. a date) refreshed by [fetch].
     * [fetch] should throw on a non-success response so the cache is preserved rather than
     * overwritten with an error body.
     */
    inline fun <reified T> stream(
        screen: String,
        params: String = "",
        noinline fetch: suspend () -> T,
    ): Flow<Resource<T>> = streamOf(screen, params, object : TypeToken<T>() {}.type, fetch)

    /** Non-reified core (kept public so the inline [stream] can delegate to it). */
    fun <T> streamOf(
        screen: String,
        params: String,
        type: Type,
        fetch: suspend () -> T,
    ): Flow<Resource<T>> = flow {
        val key = key(screen, params)
        val cached: T? = runCatching { dao.get(key)?.let { gson.fromJson<T>(it.payload, type) } }.getOrNull()
        emit(Resource.Loading(cached))
        runCatching { fetch() }
            .onSuccess { fresh ->
                runCatching { dao.put(CachedResponse(key, gson.toJson(fresh, type), System.currentTimeMillis())) }
                emit(Resource.Success(fresh))
            }
            .onFailure { e -> emit(Resource.Error(cached, e)) }
    }.flowOn(Dispatchers.IO)

    /** Drop the whole cache — call on logout so the next account never reads stale data. */
    suspend fun clear() = dao.clearAll()

    /** "<uid>|<screen>|<params>" — scopes every entry to the signed-in account. */
    fun key(screen: String, params: String): String {
        val uid = auth.currentUser?.uid ?: "anon"
        return "$uid|$screen|$params"
    }
}
