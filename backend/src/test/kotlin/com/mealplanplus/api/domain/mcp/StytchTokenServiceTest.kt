package com.mealplanplus.api.domain.mcp

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.Date

/**
 * Verifies [StytchTokenService] against an in-memory JWKS (no network), covering signature, audience,
 * issuer, expiry, scope→scope-authority mapping, and the configurable uid claim (the Firebase-uid bridge).
 */
class StytchTokenServiceTest {

    private val issuer = "https://issuer.test"
    private val audience = "project-test-abc"

    private val signingKey: RSAKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
        .generateKeyPair().let { kp ->
            RSAKey.Builder(kp.public as RSAPublicKey).privateKey(kp.private as RSAPrivateKey).keyID("test-kid").build()
        }
    private val jwkSource = ImmutableJWKSet<SecurityContext>(JWKSet(signingKey.toPublicJWK()))

    private fun service(uidClaim: String = "sub") =
        StytchTokenService.withJwkSource(jwkSource, issuer, audience, writeScope = "mcp:write", uidClaim = uidClaim)

    private fun sign(key: RSAKey = signingKey, build: JWTClaimsSet.Builder.() -> Unit): String {
        val claims = JWTClaimsSet.Builder()
            .subject("uid-123")
            .audience(audience)
            .issuer(issuer)
            .expirationTime(Date(System.currentTimeMillis() + 3_600_000))
            .apply(build)
            .build()
        return SignedJWT(JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.keyID).build(), claims)
            .apply { sign(RSASSASigner(key)) }
            .serialize()
    }

    @Test
    fun `a read-write scoped token yields READ_WRITE for its uid`() {
        val principal = service().verify(sign { claim("scope", "mcp:read mcp:write") })!!
        assertThat(principal.uid).isEqualTo("uid-123")
        assertThat(principal.scope).isEqualTo(McpTokenService.Scope.READ_WRITE)
    }

    @Test
    fun `a token without the write scope is read-only`() {
        assertThat(service().verify(sign { claim("scope", "mcp:read") })!!.scope).isEqualTo(McpTokenService.Scope.READ)
        assertThat(service().verify(sign { })!!.scope).isEqualTo(McpTokenService.Scope.READ)
    }

    @Test
    fun `the uid comes from a configurable claim (the Firebase-uid bridge)`() {
        val token = sign { subject("stytch-user-id"); claim("firebase_uid", "fb-uid-9") }
        assertThat(service(uidClaim = "firebase_uid").verify(token)!!.uid).isEqualTo("fb-uid-9")
    }

    @Test
    fun `a wrong audience is rejected`() {
        assertThat(service().verify(sign { audience("some-other-project") })).isNull()
    }

    @Test
    fun `a wrong issuer is rejected`() {
        assertThat(service().verify(sign { issuer("https://evil.test") })).isNull()
    }

    @Test
    fun `an expired token is rejected`() {
        // Past the default 60s clock-skew tolerance in DefaultJWTClaimsVerifier.
        assertThat(service().verify(sign { expirationTime(Date(System.currentTimeMillis() - 300_000)) })).isNull()
    }

    @Test
    fun `a token signed by an unknown key is rejected`() {
        val otherKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
            .generateKeyPair().let { kp ->
                RSAKey.Builder(kp.public as RSAPublicKey).privateKey(kp.private as RSAPrivateKey).keyID("test-kid").build()
            }
        assertThat(service().verify(sign(key = otherKey) { claim("scope", "mcp:write") })).isNull()
    }

    @Test
    fun `an unconfigured service verifies nothing`() {
        val off = StytchTokenService("", "", "", "mcp:write", "sub")
        assertThat(off.configured).isFalse()
        assertThat(off.verify(sign { })).isNull()
    }
}
