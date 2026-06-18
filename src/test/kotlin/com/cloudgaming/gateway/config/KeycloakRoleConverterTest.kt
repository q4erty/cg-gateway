package com.cloudgaming.gateway.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class KeycloakRoleConverterTest {

    private val converter = KeycloakRoleConverter()

    private fun buildJwt(claims: Map<String, Any>): Jwt {
        return Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .claims { it.putAll(claims) }
            .build()
    }

    @Test
    fun `should extract PLAYER role and add ROLE_ prefix`() {
        val jwt = buildJwt(
            mapOf("realm_access" to mapOf("roles" to listOf("PLAYER")))
        )

        val authorities = converter.convert(jwt)

        assertEquals(1, authorities!!.size)
        assertTrue(authorities.any { it.authority == "ROLE_PLAYER" })
    }

    @Test
    fun `should extract multiple roles at once`() {
        val jwt = buildJwt(
            mapOf("realm_access" to mapOf("roles" to listOf("PLAYER", "ADMIN", "PREMIUM")))
        )

        val authorities = converter.convert(jwt)!!

        assertEquals(3, authorities.size)
        assertTrue(authorities.any { it.authority == "ROLE_PLAYER" })
        assertTrue(authorities.any { it.authority == "ROLE_ADMIN" })
        assertTrue(authorities.any { it.authority == "ROLE_PREMIUM" })
    }

    @Test
    fun `should convert lowercase roles to uppercase`() {
        val jwt = buildJwt(
            mapOf("realm_access" to mapOf("roles" to listOf("player", "admin")))
        )

        val authorities = converter.convert(jwt)!!

        assertTrue(authorities.any { it.authority == "ROLE_PLAYER" })
        assertTrue(authorities.any { it.authority == "ROLE_ADMIN" })
    }

    @Test
    fun `should return empty list when realm_access claim is missing`() {
        val jwt = buildJwt(mapOf("sub" to "user-123"))

        val authorities = converter.convert(jwt)

        assertTrue(authorities!!.isEmpty())
    }

    @Test
    fun `should return empty list when roles field is missing inside realm_access`() {
        val jwt = buildJwt(
            mapOf("realm_access" to mapOf("other_field" to "value"))
        )

        val authorities = converter.convert(jwt)

        assertTrue(authorities!!.isEmpty())
    }

    @Test
    fun `should return empty list when roles array is empty`() {
        val jwt = buildJwt(
            mapOf("realm_access" to mapOf("roles" to emptyList<String>()))
        )

        val authorities = converter.convert(jwt)

        assertTrue(authorities!!.isEmpty())
    }
}