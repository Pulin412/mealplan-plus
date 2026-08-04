package com.mealplanplus.api.config

import com.mealplanplus.api.filter.RequestIdFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

/**
 * Registers [RequestIdFilter] as a plain servlet filter at the very front of the chain so a
 * correlation id is in the MDC before Spring Security (or anything else) runs. The filter is NOT a
 * @Component — registering it here is the single source of truth for its order and avoids the
 * double-registration foot-gun (cf. FirebaseTokenFilter in SecurityConfig).
 */
@Configuration
class LoggingConfig {

    @Bean
    fun requestIdFilterRegistration(): FilterRegistrationBean<RequestIdFilter> =
        FilterRegistrationBean(RequestIdFilter()).apply {
            order = Ordered.HIGHEST_PRECEDENCE
            addUrlPatterns("/*")
        }
}
