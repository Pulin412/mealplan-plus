package com.mealplanplus.data.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The cache-first fold used by every read-through screen. */
class RenderTest {

    @Test
    fun success_alwaysReplacesWithFreshValue() {
        val r = Resource.Success("fresh").render(hasContent = true)
        assertFalse(r.keep)
        assertEquals("fresh", r.value)
        assertFalse(r.loading)
        assertNull(r.error)
    }

    @Test
    fun loading_withContentShown_keepsIt() {
        val r = Resource.Loading("cached").render(hasContent = true)
        assertTrue("must not disturb data already on screen", r.keep)
        assertFalse(r.loading)
        assertNull(r.error)
    }

    @Test
    fun loading_coldOpenWithCache_paintsCacheNoSpinner() {
        val r = Resource.Loading("cached").render(hasContent = false)
        assertFalse(r.keep)
        assertEquals("cached", r.value)
        assertFalse("cache present → no spinner", r.loading)
    }

    @Test
    fun loading_coldOpenNoCache_spins() {
        val r = Resource.Loading<String>(null).render(hasContent = false)
        assertTrue(r.keep)
        assertTrue(r.loading)
    }

    @Test
    fun error_withContentShown_staysSilent() {
        val r = Resource.Error("cached", RuntimeException("boom")).render(hasContent = true)
        assertTrue(r.keep)
        assertNull("a failed refresh must not nag while data is shown", r.error)
    }

    @Test
    fun error_coldOpenWithCache_showsStaleData_noError() {
        val r = Resource.Error("cached", RuntimeException("boom")).render(hasContent = false)
        assertFalse(r.keep)
        assertEquals("cached", r.value)
        assertNull(r.error)
    }

    @Test
    fun error_coldOpenNoCache_surfacesMessage() {
        val r = Resource.Error<String>(null, RuntimeException("cold start 503")).render(hasContent = false)
        assertTrue(r.keep)
        assertEquals("cold start 503", r.error)
    }
}
