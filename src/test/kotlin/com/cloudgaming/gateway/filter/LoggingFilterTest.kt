package com.cloudgaming.gateway.filter

import com.cloudgaming.gateway.config.TestSecurityConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig::class)
@ExtendWith(OutputCaptureExtension::class)
class LoggingFilterTest {

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
    fun `should log incoming request with arrow`(output: CapturedOutput) {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()

        Thread.sleep(200)
        assert(output.all.contains("GET /actuator/health")) {
            "Log should contain GET /actuator/health for incoming request. Actual output: ${output.all}"
        }
    }

    @Test
    fun `should log outgoing response with arrow`(output: CapturedOutput) {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()

        Thread.sleep(200)
        assert(output.all.contains("status=")) {
            "Log should contain status= for outgoing response. Actual output: ${output.all}"
        }
    }

    @Test
    fun `should log anonymous for unauthenticated request`(output: CapturedOutput) {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()

        Thread.sleep(200)
        assert(output.all.contains("user=anonymous")) {
            "Log should contain user=anonymous. Actual output: ${output.all}"
        }
    }

    @Test
    fun `should log username from JWT subject for authenticated request`(output: CapturedOutput) {
        webTestClient
            .get()
            .uri("/actuator/health")
            .header("Authorization", "Bearer test-token-PLAYER")
            .exchange()

        Thread.sleep(200)
        assert(output.all.contains("user=")) { "Log should contain user= field. Actual output: ${output.all}" }
    }
}