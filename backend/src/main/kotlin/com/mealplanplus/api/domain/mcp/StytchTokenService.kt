package com.mealplanplus.api.domain.mcp

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.jwk.source.RemoteJWKSet
import com.nimbusds.jose.util.DefaultResourceRetriever
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URL

/**
 * Verifies OAuth 2.1 access tokens issued by our Stytch Connected-Apps authorization server (Phase 2),
 * so a user's own Claude can connect to the MCP server through the standard add-connector / OAuth flow
 * — the header-bearer connector token ([McpTokenService]) only works for Inspector / the Messages API.
 *
 * Same shape as [com.mealplanplus.api.filter.FirebaseTokenFilter]: RS256 JWTs validated against the
 * remote JWKS (cached + auto-rotated), with the audience pinned to our Stytch project id and (once set)
 * the issuer. The caller's app identity is read from [uidClaim] — with a Stytch Trusted Auth Token Profile
 * we map the Firebase uid onto a custom claim (Phase 2b), so the MCP tools stay keyed by Firebase uid with
 * no second identity store. Returns a [McpTokenService.Principal] so [McpAuthFilter] treats both token
 * kinds uniformly. Disabled (verifies nothing) until `stytch.jwk-set-uri` + `stytch.audience` are set.
 */
@Service
class StytchTokenService private constructor(
    private val jwkSource: JWKSource<SecurityContext>?,
    private val issuer: String,
    private val audience: String,
    private val writeScope: String,
    private val uidClaim: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Autowired
    constructor(
        @Value("\${stytch.jwk-set-uri:}") jwkSetUri: String,
        @Value("\${stytch.issuer:}") issuer: String,
        @Value("\${stytch.audience:}") audience: String,
        @Value("\${stytch.write-scope:mcp:write}") writeScope: String,
        @Value("\${stytch.uid-claim:sub}") uidClaim: String,
    ) : this(
        // Explicit timeouts: nimbus's default RemoteJWKSet retriever times out before Stytch's
        // Cloudflare-fronted JWKS answers a cold request (~1s), unlike Google's fast Firebase JWKS.
        jwkSource = jwkSetUri.takeIf { it.isNotBlank() }
            ?.let { RemoteJWKSet(URL(it), DefaultResourceRetriever(3000, 5000)) },
        issuer = issuer,
        audience = audience,
        writeScope = writeScope,
        uidClaim = uidClaim,
    )

    val configured: Boolean get() = jwkSource != null && audience.isNotBlank()

    /** The principal iff the token is a valid Stytch access token for this project, else null. */
    fun verify(token: String): McpTokenService.Principal? {
        val source = jwkSource ?: return null
        if (audience.isBlank()) return null
        return try {
            val processor = DefaultJWTProcessor<SecurityContext>().apply {
                jwsKeySelector = JWSVerificationKeySelector(JWSAlgorithm.RS256, source)
                val exact = JWTClaimsSet.Builder().audience(audience)
                val required = mutableSetOf("sub", "exp", "aud")
                if (issuer.isNotBlank()) { exact.issuer(issuer); required.add("iss") }
                jwtClaimsSetVerifier = DefaultJWTClaimsVerifier(exact.build(), required)
            }
            val claims = processor.process(token, null)
            val uid = (if (uidClaim == "sub") claims.subject else claims.getStringClaim(uidClaim))
                ?.takeIf { it.isNotBlank() } ?: return null
            val scopes = (claims.getStringClaim("scope") ?: "").split(" ").filter { it.isNotBlank() }.toSet()
            val scope = if (writeScope.isNotBlank() && writeScope in scopes) {
                McpTokenService.Scope.READ_WRITE
            } else {
                McpTokenService.Scope.READ
            }
            McpTokenService.Principal(uid, scope)
        } catch (e: Exception) {
            log.debug("Stytch access-token validation failed: ${e.message}")
            null
        }
    }

    companion object {
        /** Test seam: build the service against an in-memory JWKS instead of the remote URL. */
        fun withJwkSource(
            jwkSource: JWKSource<SecurityContext>,
            issuer: String,
            audience: String,
            writeScope: String = "mcp:write",
            uidClaim: String = "sub",
        ) = StytchTokenService(jwkSource, issuer, audience, writeScope, uidClaim)
    }
}
