package com.mealplanplus.api.domain.featureflag

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import java.util.Optional

class FeatureFlagServiceTest {

    private val repo = Mockito.mock(FeatureFlagRepository::class.java)
    private val service = FeatureFlagService(repo)

    @Test
    fun `isEnabled reads the flag from the repo and caches it`() {
        Mockito.`when`(repo.findById("mcp_server"))
            .thenReturn(Optional.of(FeatureFlag(flagKey = "mcp_server", enabled = true)))

        assertThat(service.isEnabled("mcp_server")).isTrue()
        assertThat(service.isEnabled("mcp_server")).isTrue()

        // Second read is served from cache — only one DB hit.
        Mockito.verify(repo, Mockito.times(1)).findById("mcp_server")
    }

    @Test
    fun `isEnabled defaults to false when the flag row is absent`() {
        Mockito.`when`(repo.findById("mcp_server")).thenReturn(Optional.empty())

        assertThat(service.isEnabled("mcp_server")).isFalse()
    }

    @Test
    fun `setEnabled persists the value plus updater and refreshes the cache immediately`() {
        Mockito.`when`(repo.findById("mcp_server")).thenReturn(Optional.empty())
        Mockito.`when`(repo.save(anyNonNull<FeatureFlag>())).thenAnswer { it.getArgument<FeatureFlag>(0) }

        service.setEnabled("mcp_server", enabled = true, updatedBy = "admin-uid")

        val captor = ArgumentCaptor.forClass(FeatureFlag::class.java)
        Mockito.verify(repo).save(captor.capture())
        assertThat(captor.value.enabled).isTrue()
        assertThat(captor.value.updatedBy).isEqualTo("admin-uid")

        // The new value is live from cache — no extra findById beyond the one setEnabled did.
        assertThat(service.isEnabled("mcp_server")).isTrue()
        Mockito.verify(repo, Mockito.times(1)).findById("mcp_server")
    }

    // Mockito's any() returns null; a generic return type lets Kotlin pass it to a non-null parameter.
    private fun <T> anyNonNull(): T = Mockito.any()
}
