package com.mealplanplus.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class MealPlanApiApplication

fun main(args: Array<String>) {
    runApplication<MealPlanApiApplication>(*args)
}
