package com.mealplanplus.api

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(properties = ["firebase.project-id=test-project"])
class MealPlanApiApplicationTests {

    @Test
    fun contextLoads() { }
}
