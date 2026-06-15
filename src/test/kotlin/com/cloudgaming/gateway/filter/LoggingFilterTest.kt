package com.cloudgaming.gateway.filter

import com.cloudgaming.gateway.config.TestSecurityConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.annotation.Import
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestSecurityConfig::class)
@ExtendWith(OutputCaptureExtension::class)
class LoggingFilterTest {

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `should log incoming request with arrow →`(output: CapturedOutput) {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()

        assert(output.all.contains("→")) { "Log should contain → for incoming request" }
    }

    @Test
    fun `should log outgoing response with arrow ←`(output: CapturedOutput) {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()

        assert(output.all.contains("←")) { "Log should contain ← for outgoing response" }
    }

    @Test
    fun `should log anonymous for unauthenticated request`(output: CapturedOutput) {
        webTestClient.get()
            .uri("/actuator/health")
            .exchange()

        assert(output.all.contains("user=anonymous")) { "Log should contain user=anonymous" }
    }

    @Test
    fun `should log username from JWT subject for authenticated request`(output: CapturedOutput) {
        webTestClient
            .mutateWith(
                mockJwt()
                    .jwt { it.subject("testplayer") }
                    .authorities(listOf(SimpleGrantedAuthority("ROLE_PLAYER")))
            )
            .get()
            .uri("/actuator/health")
            .exchange()

        assert(output.all.contains("user=testplayer")) { "Log should contain user=testplayer" }
    }
}