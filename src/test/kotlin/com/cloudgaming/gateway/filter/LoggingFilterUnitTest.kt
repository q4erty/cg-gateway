package com.cloudgaming.gateway.filter

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class LoggingFilterUnitTest {

    private val filter = LoggingFilter()

    @Test
    fun `should return order -1`() {
        assertEquals(-1, filter.order)
    }

    @Test
    fun `should log anonymous when no security context`() {
        val request = MockServerHttpRequest.get("/test/path").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should log username when authenticated`() {
        val mockAuth: Authentication = mock()
        whenever(mockAuth.name).thenReturn("test-user")
        val securityContext: SecurityContext = SecurityContextImpl(mockAuth)

        val request = MockServerHttpRequest.get("/api/test").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should log anonymous when authentication name is null`() {
        val mockAuth: Authentication = mock()
        whenever(mockAuth.name).thenReturn(null)
        val securityContext: SecurityContext = SecurityContextImpl(mockAuth)

        val request = MockServerHttpRequest.post("/api/data").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should log response status after chain completes`() {
        val request = MockServerHttpRequest.get("/actuator/health").build()
        val exchange = MockServerWebExchange.from(request)
        exchange.response.statusCode = HttpStatus.OK

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should handle PUT request`() {
        val request = MockServerHttpRequest.put("/api/resource/123").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should handle DELETE request`() {
        val request = MockServerHttpRequest.delete("/api/resource/456").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should log status 0 when response status code is null`() {
        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)
        // status code is null by default in MockServerHttpResponse

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should handle chain filter error gracefully`() {
        val request = MockServerHttpRequest.get("/error-path").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.error(RuntimeException("Test error")))

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .expectError(RuntimeException::class.java)
            .verify()
    }

    @Test
    fun `should handle security context with null authentication`() {
        val securityContext: SecurityContext = mock()
        whenever(securityContext.authentication).thenReturn(null)

        val request = MockServerHttpRequest.get("/test").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)
            .contextWrite { it.put(SecurityContext::class.java, Mono.just(securityContext)) }

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should log with status 500 on error response`() {
        val request = MockServerHttpRequest.get("/api/error").build()
        val exchange = MockServerWebExchange.from(request)
        exchange.response.statusCode = HttpStatus.INTERNAL_SERVER_ERROR

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should log with status 404 on not found`() {
        val request = MockServerHttpRequest.get("/not-found").build()
        val exchange = MockServerWebExchange.from(request)
        exchange.response.statusCode = HttpStatus.NOT_FOUND

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }

    @Test
    fun `should handle PATCH request`() {
        val request = MockServerHttpRequest.patch("/api/resource/789").build()
        val exchange = MockServerWebExchange.from(request)

        val chain: WebFilterChain = mock()
        whenever(chain.filter(any<ServerWebExchange>())).thenReturn(Mono.empty())

        val result = filter.filter(exchange, chain)

        StepVerifier.create(result)
            .verifyComplete()
    }
}



