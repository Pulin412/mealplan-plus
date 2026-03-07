package com.mealplanplus.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("MealPlan+ API")
                .description("REST API for MealPlan+ — nutrition tracking & meal planning")
                .version("v1")
        )
        .addSecurityItem(SecurityRequirement().addList("Firebase JWT"))
        .components(
            Components().addSecuritySchemes(
                "Firebase JWT",
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Firebase ID token from client sign-in")
            )
        )
}
