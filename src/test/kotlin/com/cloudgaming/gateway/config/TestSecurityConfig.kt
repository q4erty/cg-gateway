package com.cloudgaming.gateway.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import reactor.core.publisher.Mono
import java.time.Instant

@TestConfiguration(proxyBeanMethods = false)
class TestSecurityConfig {

    @Bean
    fun reactiveJwtDecoder(): ReactiveJwtDecoder = ReactiveJwtDecoder { token ->
        Mono.just(
            Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()
        )
    }
}