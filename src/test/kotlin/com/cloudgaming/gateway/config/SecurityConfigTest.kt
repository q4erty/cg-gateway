package com.cloudgaming.gateway.config

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig::class)
@ActiveProfiles("test")
class SecurityConfigTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    private fun bearerToken(vararg roles: String): String {
        return "test-token-${roles.joinToString("-")}"
    }

    private fun WebTestClient.RequestHeadersSpec<*>.withToken(token: String) =
        this.header("Authorization", "Bearer $token")

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
            .get()
            .uri("/api/sessions/test")
            .withToken(bearerToken("PLAYER"))
            .exchange()
            .expectStatus().value { status -> status >= 400 }
    }

    @Test
    fun `sessions with token but without PLAYER role should return 403`() {
        webTestClient
            .get()
            .uri("/api/sessions/test")
            .withToken(bearerToken("SOME_OTHER_ROLE"))
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
            .get()
            .uri("/api/billing/test")
            .withToken(bearerToken("PLAYER"))
            .exchange()
            .expectStatus().value { status -> status >= 400 }
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
            .get()
            .uri("/actuator/metrics")
            .withToken(bearerToken("PLAYER"))
            .exchange()
            .expectStatus().isForbidden
    }

    @Test
    fun `actuator metrics with ADMIN role should be accessible`() {
        webTestClient
            .get()
            .uri("/actuator/metrics")
            .withToken(bearerToken("ADMIN"))
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
            .get()
            .uri("/some/unknown/path")
            .withToken(bearerToken("PLAYER"))
            .exchange()
            .expectStatus().isNotFound
    }
}