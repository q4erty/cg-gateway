package com.cloudgaming.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class LoggingFilter : WebFilter, Ordered {

    private val log = LoggerFactory.getLogger(LoggingFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val startTime = System.currentTimeMillis()

        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication?.name ?: "anonymous" }
            .defaultIfEmpty("anonymous")
            .flatMap { username ->
                log.info(
                    "→ {} {} | user={}",
                    request.method,
                    request.uri.path,
                    username
                )
                chain.filter(exchange).doFinally {
                    val duration = System.currentTimeMillis() - startTime
                    val status = exchange.response.statusCode?.value() ?: 0
                    log.info(
                        "← {} {} | status={} | {}ms",
                        request.method,
                        request.uri.path,
                        status,
                        duration
                    )
                }
            }
    }

    override fun getOrder(): Int = -1
}