package com.cloudgaming.gateway.filter

import org.slf4j.LoggerFactory
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class UserIdHeaderFilter : GlobalFilter, Ordered {

    private val log = LoggerFactory.getLogger(UserIdHeaderFilter::class.java)

    companion object {
        const val USER_ID_HEADER = "X-User-Id"
    }

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        return ReactiveSecurityContextHolder.getContext()
            .flatMap { securityContext ->
                val authentication = securityContext.authentication
                if (authentication is JwtAuthenticationToken) {
                    val userId = authentication.token.subject ?: return@flatMap chain.filter(exchange)
                    log.debug("Propagating X-User-Id: {} to downstream service", userId)
                    val mutatedRequest = exchange.request.mutate()
                        .header(USER_ID_HEADER, userId)
                        .build()
                    chain.filter(exchange.mutate().request(mutatedRequest).build())
                } else {
                    chain.filter(exchange)
                }
            }
            .switchIfEmpty(Mono.defer {
                chain.filter(exchange)
            })
    }

    override fun getOrder(): Int = -100
}

