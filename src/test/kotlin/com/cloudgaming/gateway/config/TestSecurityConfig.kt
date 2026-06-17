package com.cloudgaming.gateway.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono
import java.time.Instant

@TestConfiguration(proxyBeanMethods = false)
@EnableWebFluxSecurity
class TestSecurityConfig {

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder = ReactiveJwtDecoder { token ->
        val roles = if (token.startsWith("test-token-")) {
            token.substring("test-token-".length).split("-")
        } else {
            emptyList()
        }

        val scopes = roles.ifEmpty { listOf("PLAYER") }

        Mono.just(
            Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("test-user")
                .claim("scope", scopes.joinToString(" "))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        )
    }

    @Bean
    @Primary
    fun testSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .authorizeExchange { auth ->
                auth
                    .pathMatchers("/actuator/health").permitAll()
                    .pathMatchers("/actuator/info").permitAll()
                    .pathMatchers("/actuator/**").hasAuthority("SCOPE_ADMIN")
                    .pathMatchers("/api/sessions/**").hasAuthority("SCOPE_PLAYER")
                    .pathMatchers("/api/billing/**").hasAuthority("SCOPE_PLAYER")
                    .pathMatchers("/signaling/**").hasAuthority("SCOPE_PLAYER")
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { }
            }
            .build()
    }
}