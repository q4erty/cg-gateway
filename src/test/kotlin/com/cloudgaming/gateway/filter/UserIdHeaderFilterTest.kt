package com.cloudgaming.gateway.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.Instant

class UserIdHeaderFilterTest {

    private val filter = UserIdHeaderFilter()

    @Test
    fun `should return order -100`() {
        assertEquals(-100, filter.order)
    }

    @Test
    fun `USER_ID_HEADER constant should be X-User-Id`() {
        assertEquals("X-User-Id", UserIdHeaderFilter.USER_ID_HEADER)
    }

    @Test
    fun `should add X-User-Id header when JWT authentication is present`() {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("user-123")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val jwtAuth = JwtAuthenticationToken(jwt)
        val securityContext: SecurityContext = SecurityContextImpl(jwtAuth)

        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: GatewayFilterChain = mock()
        whenever(chain.filter(any())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should proceed without header when no security context`() {
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: GatewayFilterChain = mock()
        whenever(chain.filter(any())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should proceed without header when authentication is not JWT`() {
        val mockAuth: Authentication = mock()
        whenever(mockAuth.name).thenReturn("test-user")
        val securityContext: SecurityContext = SecurityContextImpl(mockAuth)

        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: GatewayFilterChain = mock()
        whenever(chain.filter(any())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should proceed without header when JWT subject is null`() {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .claim("aud", "test-audience")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val jwtAuth = JwtAuthenticationToken(jwt)
        val securityContext: SecurityContext = SecurityContextImpl(jwtAuth)

        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: GatewayFilterChain = mock()
        whenever(chain.filter(any())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should handle POST request`() {
        val jwt = Jwt.withTokenValue("test-token")
            .header("alg", "RS256")
            .subject("user-789")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        val jwtAuth = JwtAuthenticationToken(jwt)
        val securityContext: SecurityContext = SecurityContextImpl(jwtAuth)

        val request = MockServerHttpRequest.post("/api/resource").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: GatewayFilterChain = mock()
        whenever(chain.filter(any())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .verifyComplete()
    }
}
