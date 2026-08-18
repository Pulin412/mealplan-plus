package com.mealplanplus.api.config

import com.mealplanplus.api.domain.mcp.McpAuthFilter
import com.mealplanplus.api.filter.FirebaseTokenFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val firebaseTokenFilter: FirebaseTokenFilter,
    private val mcpAuthFilter: McpAuthFilter,
) {

    /**
     * Allowed CORS origins come from `app.cors.allowed-origins` (comma-separated) so they differ per
     * profile: dev keeps localhost + *.vercel.app previews; prod is locked to the prod webapp origin.
     * CORS only restricts browser JS from other web origins — it does not affect the Android app.
     */
    @Bean
    fun corsConfigurationSource(
        @Value("\${app.cors.allowed-origins}") allowedOrigins: List<String>,
    ): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOriginPatterns = allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }

    /**
     * Prod-only: protect Swagger UI + OpenAPI docs behind HTTP Basic auth so the API surface isn't
     * publicly browsable. Credentials come from `app.swagger.user` / `app.swagger.password` (secrets).
     * This chain has higher precedence, so in prod it intercepts these paths before the main chain;
     * in dev the profile is absent and the main chain leaves them open.
     */
    @Bean
    @Order(1)
    @Profile("prod")
    fun swaggerSecurityChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .httpBasic { it.authenticationEntryPoint(basicEntryPoint()) }
            // Keep anonymous enabled: it makes unauthenticated requests resolve to the entry point
            // (401 + WWW-Authenticate → browser prompt) rather than the access-denied handler (403).
            .exceptionHandling { it.authenticationEntryPoint(basicEntryPoint()) }
            .csrf { it.disable() }
            .cors { it.disable() }
            .build()

    private fun basicEntryPoint(): AuthenticationEntryPoint =
        BasicAuthenticationEntryPoint().apply { realmName = "MealPlan+ API docs" }

    /**
     * FirebaseTokenFilter is a @Component, so Spring Boot would auto-register it as a servlet filter
     * on EVERY request — running it a second time outside the security chain (where it's already
     * added via addFilterBefore). Disable that auto-registration so it runs only in the chain.
     */
    @Bean
    fun disableFirebaseFilterAutoRegistration(
        filter: FirebaseTokenFilter,
    ): FilterRegistrationBean<FirebaseTokenFilter> =
        FilterRegistrationBean(filter).apply { isEnabled = false }

    /** Same reasoning as [disableFirebaseFilterAutoRegistration] — McpAuthFilter runs only inside the chain. */
    @Bean
    fun disableMcpFilterAutoRegistration(
        filter: McpAuthFilter,
    ): FilterRegistrationBean<McpAuthFilter> =
        FilterRegistrationBean(filter).apply { isEnabled = false }

    @Bean
    @Profile("prod")
    fun swaggerUserDetailsService(
        @Value("\${app.swagger.user}") user: String,
        @Value("\${app.swagger.password}") password: String,
    ): UserDetailsService = InMemoryUserDetailsManager(
        User.withUsername(user).password("{noop}$password").roles("SWAGGER").build(),
    )

    @Bean
    @Order(2)
    fun filterChain(
        http: HttpSecurity,
        @Qualifier("corsConfigurationSource") corsSource: CorsConfigurationSource,
    ): SecurityFilterChain =
        http
            .cors { it.configurationSource(corsSource) }
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(
                        "/actuator/health",
                        "/actuator/health/**",  // health groups (e.g. /health/ping for the DB-free uptime check)
                        "/error",            // servlet error dispatch — must pass through or it masks the real status
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/h2-console/**",
                        "/api/v1/internal/reminders/run", // scheduler-triggered; authed by X-Reminder-Token in-controller
                        "/mcp/**",           // MCP server — authed by McpAuthFilter (bearer connector token), not Firebase
                        "/.well-known/oauth-protected-resource", // OAuth Protected Resource Metadata (RFC 9728) — public discovery
                    ).permitAll()
                    .anyRequest().authenticated()
            }
            // Allow H2 console iframes in dev
            .headers { it.frameOptions { fo -> fo.disable() } }
            // MCP auth (bearer connector token) runs before Firebase so /mcp/** requests never fall through it.
            .addFilterBefore(mcpAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(firebaseTokenFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
}
