package com.mealplanplus.api.filter

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.net.URL

/**
 * Validates Firebase ID tokens (RS256 JWTs) using Google's JWKS endpoint.
 * No Firebase Admin SDK required — zero billing, keys cached + auto-rotated.
 *
 * JWKS source: https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com
 */
@Component
class FirebaseTokenFilter(
    @Value("\${firebase.project-id}") private val projectId: String
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    // RemoteJWKSet caches and auto-rotates Google's public keys
    private val jwkSource = RemoteJWKSet<SecurityContext>(
        URL("https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com")
    )

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        chain: FilterChain
    ) {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()
        try {
            val processor = DefaultJWTProcessor<SecurityContext>().apply {
                jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, jwkSource)
                jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(
                    JWTClaimsSet.Builder()
                        .issuer("https://securetoken.google.com/$projectId")
                        .audience(projectId)
                        .build(),
                    setOf("sub", "iat", "exp", "iss", "aud")
                )
            }
            val claims = processor.process(token, null)
            val auth = UsernamePasswordAuthenticationToken(
                claims.subject, null, emptyList()
            )
            SecurityContextHolder.getContext().authentication = auth
        } catch (e: Exception) {
            log.debug("Firebase JWT validation failed: ${e.message}")
        }

        chain.doFilter(request, response)
    }
}
