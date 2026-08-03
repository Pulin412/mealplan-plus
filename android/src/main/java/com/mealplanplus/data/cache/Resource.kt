package com.mealplanplus.data.cache

/**
 * The state of a read-through cached read: [data] holds the best value known at that moment —
 * the cached payload during [Loading], the fresh payload on [Success], and the last-known cache
 * (if any) on [Error] so a screen keeps showing something usable when the refresh fails.
 */
sealed interface Resource<out T> {
    val data: T?

    data class Loading<out T>(override val data: T?) : Resource<T>
    data class Success<out T>(override val data: T) : Resource<T>
    data class Error<out T>(override val data: T?, val error: Throwable) : Resource<T>
}
