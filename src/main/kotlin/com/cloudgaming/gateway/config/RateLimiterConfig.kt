package com.cloudgaming.gateway.config

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono

@Configuration
class RateLimiterConfig {

    private val log = LoggerFactory.getLogger(RateLimiterConfig::class.java)

    @Bean
    fun userKeyResolver(): KeyResolver = KeyResolver { exchange ->
        ReactiveSecurityContextHolder.getContext()
            .flatMap { securityContext ->
                val authentication = securityContext.authentication
                if (authentication is JwtAuthenticationToken) {
                    val userId = authentication.token.subject ?: "unknown"
                    log.debug("Rate limit key resolved from JWT subject: {}", userId)
                    Mono.just(userId)
                } else {
                    val clientIp = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
                    log.debug("Rate limit key resolved from IP: {}", clientIp)
                    Mono.just(clientIp)
                }
            }
            .switchIfEmpty(Mono.defer {
                val clientIp = exchange.request.remoteAddress?.address?.hostAddress ?: "unknown"
                log.debug("Rate limit key resolved from IP (no context): {}", clientIp)
                Mono.just(clientIp)
            })
    }
}

