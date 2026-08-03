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

/**
 * How a screen that paints cache-first should fold a [Resource] into its state. [keep] = leave the
 * currently-shown data untouched (so a silent reload never flickers, and a failed refresh keeps the
 * last-known data); otherwise show [value].
 *
 * - fresh [Resource.Success] → always replace with the fresh value
 * - [Resource.Loading]/[Resource.Error] while the screen already has content → keep it
 * - cold open (no content yet): paint the cache if present, else spin ([loading]); surface an
 *   [error] message only when there is genuinely nothing to show.
 */
class Rendered<out T>(val value: T?, val keep: Boolean, val loading: Boolean, val error: String?)

fun <T> Resource<T>.render(hasContent: Boolean): Rendered<T> = when (this) {
    is Resource.Success -> Rendered(data, keep = false, loading = false, error = null)
    is Resource.Loading ->
        if (hasContent) Rendered(null, keep = true, loading = false, error = null)
        else Rendered(data, keep = data == null, loading = data == null, error = null)
    is Resource.Error ->
        if (hasContent || data != null) Rendered(data, keep = hasContent, loading = false, error = null)
        else Rendered(null, keep = true, loading = false, error = error.message)
}
