package com.mealplanplus.api.domain.feedback

import com.mealplanplus.api.generated.model.FeedbackRequestDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito

class FeedbackServiceTest {

    private val repo = Mockito.mock(FeedbackRepository::class.java)
    private val service = FeedbackService(repo)

    @Test
    fun `submit persists the message with the server-side uid and echoes a dto`() {
        Mockito.`when`(repo.save(anyNonNull<Feedback>())).thenAnswer { it.getArgument<Feedback>(0) }

        val dto = service.submit(
            FeedbackRequestDto(message = "love the app", appVersion = "2.2.7", platform = "android"),
            firebaseUid = "uid-123"
        )

        // The dto echoes what was submitted…
        assertThat(dto.message).isEqualTo("love the app")
        assertThat(dto.appVersion).isEqualTo("2.2.7")
        assertThat(dto.platform).isEqualTo("android")

        // …and the persisted row is stamped with the server-side uid, not anything client-supplied.
        val captor = ArgumentCaptor.forClass(Feedback::class.java)
        Mockito.verify(repo).save(captor.capture())
        assertThat(captor.value.firebaseUid).isEqualTo("uid-123")
        assertThat(captor.value.message).isEqualTo("love the app")
    }

    // Mockito's any() returns null; a generic return type lets Kotlin pass it to a non-null parameter
    // without inserting a null-check (the standard mockito-kotlin shim).
    private fun <T> anyNonNull(): T = Mockito.any()
}
