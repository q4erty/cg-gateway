package com.cloudgaming.gateway.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig::class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    private fun mockJwtWithRoles(vararg roles: String) = mockJwt()
        .jwt { it.subject("user-123") }
        .authorities(roles.map { SimpleGrantedAuthority("ROLE_${it.uppercase()}") })

    @Test
    fun `actuator health should be accessible without a token`() {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `sessions without token should return 401`() {
        webTestClient.get()
            .uri("/api/sessions/test")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `sessions with PLAYER role should be forwarded (502 — no upstream running)`() {
        webTestClient
            .mutateWith(mockJwtWithRoles("PLAYER"))
            .get()
            .uri("/api/sessions/test")
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    fun `sessions with token but without PLAYER role should return 403`() {
        webTestClient
            .mutateWith(mockJwtWithRoles("SOME_OTHER_ROLE"))
            .get()
            .uri("/api/sessions/test")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `billing without token should return 401`() {
        webTestClient.get()
            .uri("/api/billing/test")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `billing with PLAYER role should be forwarded (502 — no upstream running)`() {
        webTestClient
            .mutateWith(mockJwtWithRoles("PLAYER"))
            .get()
            .uri("/api/billing/test")
            .exchange()
            .expectStatus().is5xxServerError
    }

    @Test
    fun `actuator metrics without token should return 401`() {
        webTestClient.get()
            .uri("/actuator/metrics")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `actuator metrics with PLAYER role should return 403`() {
        webTestClient
            .mutateWith(mockJwtWithRoles("PLAYER"))
            .get()
            .uri("/actuator/metrics")
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `actuator metrics with ADMIN role should be accessible`() {
        webTestClient
            .mutateWith(mockJwtWithRoles("ADMIN"))
            .get()
            .uri("/actuator/metrics")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `unknown path without token should return 401`() {
        webTestClient.get()
            .uri("/some/unknown/path")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `unknown path with any valid token should be forwarded`() {
        webTestClient
            .mutateWith(mockJwtWithRoles("PLAYER"))
            .get()
            .uri("/some/unknown/path")
            .exchange()
            .expectStatus().isNotFound
    }
}