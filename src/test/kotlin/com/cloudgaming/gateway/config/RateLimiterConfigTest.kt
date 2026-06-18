package com.cloudgaming.gateway.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.net.InetSocketAddress
import java.time.Instant

class RateLimiterConfigTest {

    private val config = RateLimiterConfig()
    private val keyResolver = config.userKeyResolver()

    @Test
    fun `userKeyResolver bean should not be null`() {
        assertNotNull(keyResolver)
    }

    @Test
    fun `should resolve key from JWT subject when authenticated`() {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("user-456")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val jwtAuth = JwtAuthenticationToken(jwt)
        val securityContext: SecurityContext = SecurityContextImpl(jwtAuth)

        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val result = keyResolver.resolve(exchange)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .expectNext("user-456")
            .verifyComplete()
    }

    @Test
    fun `should resolve key from IP when authentication is not JWT`() {
        val mockAuth: Authentication = mock()
        whenever(mockAuth.name).thenReturn("test-user")
        val securityContext: SecurityContext = SecurityContextImpl(mockAuth)

        val request = MockServerHttpRequest.get("/test")
            .remoteAddress(InetSocketAddress("192.168.1.100", 12345))
            .build()
        val exchange = MockServerWebExchange.from(request)

        val result = keyResolver.resolve(exchange)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .expectNext("192.168.1.100")
            .verifyComplete()
    }

    @Test
    fun `should resolve key from IP when no security context`() {
        val request = MockServerHttpRequest.get("/test")
            .remoteAddress(InetSocketAddress("10.0.0.1", 8080))
            .build()
        val exchange = MockServerWebExchange.from(request)

        val result = keyResolver.resolve(exchange)

        StepVerifier.create(result)
            .expectNext("10.0.0.1")
            .verifyComplete()
    }

    @Test
    fun `should return unknown when no IP address available and no security context`() {
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val result = keyResolver.resolve(exchange)

        StepVerifier.create(result)
            .expectNext("unknown")
            .verifyComplete()
    }

    @Test
    fun `should return unknown when JWT subject is null`() {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("aud", "audience")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val jwtAuth = JwtAuthenticationToken(jwt)
        val securityContext: SecurityContext = SecurityContextImpl(jwtAuth)

        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val result = keyResolver.resolve(exchange)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .expectNext("unknown")
            .verifyComplete()
    }

    @Test
    fun `should return unknown when no IP and non-JWT authentication`() {
        val mockAuth: Authentication = mock()
        whenever(mockAuth.name).thenReturn("test-user")
        val securityContext: SecurityContext = SecurityContextImpl(mockAuth)

        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val result = keyResolver.resolve(exchange)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .expectNext("unknown")
            .verifyComplete()
    }
}

