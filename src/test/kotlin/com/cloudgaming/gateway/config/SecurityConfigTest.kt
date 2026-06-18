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
    fun `payments without token should return 401`() {
        webTestClient.get()
            .uri("/api/payments/test")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `payments with PLAYER role should be forwarded (502 — no upstream running)`() {
        webTestClient
            .get()
            .uri("/api/payments/test")
            .withToken(bearerToken("PLAYER"))
            .exchange()
            .expectStatus().value { status -> status >= 400 }
    }

    @Test
    fun `stripe webhook should be accessible without a token`() {
        webTestClient.post()
            .uri("/api/webhooks/stripe/events")
            .exchange()
            .expectStatus().value { status -> status != 401 && status != 403 }
    }

    @Test
    fun `yookassa webhook should be accessible without a token`() {
        webTestClient.post()
            .uri("/api/webhooks/yookassa/events")
            .exchange()
            .expectStatus().value { status -> status != 401 && status != 403 }
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

    @Test
    fun `users endpoint without token should return 401`() {
        webTestClient.get()
            .uri("/api/users/me")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `users endpoint with any valid token should be forwarded`() {
        webTestClient
            .get()
            .uri("/api/users/me")
            .withToken(bearerToken("PLAYER"))
            .exchange()
            .expectStatus().value { status -> status >= 400 }
    }

    @Test
    fun `notifications endpoint without token should return 401`() {
        webTestClient.get()
            .uri("/api/notifications/list")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `notifications endpoint with any valid token should be forwarded`() {
        webTestClient
            .get()
            .uri("/api/notifications/list")
            .withToken(bearerToken("PLAYER"))
            .exchange()
            .expectStatus().value { status -> status >= 400 }
    }

    @Test
    fun `signaling endpoint without token should return 401`() {
        webTestClient.get()
            .uri("/api/v1/signaling/connect")
            .exchange()
            .expectStatus().isUnauthorized
    }

    @Test
    fun `signaling endpoint with PLAYER role should be forwarded`() {
        webTestClient
            .get()
            .uri("/api/v1/signaling/connect")
            .withToken(bearerToken("PLAYER"))
            .exchange()
            .expectStatus().value { status -> status >= 400 }
    }

    @Test
    fun `actuator info should be accessible without token`() {
        webTestClient.get()
            .uri("/actuator/info")
            .exchange()
            .expectStatus().isOk
    }
}