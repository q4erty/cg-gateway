package com.cloudgaming.gateway.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val keycloakRoleConverter: KeycloakRoleConverter,
    @Value("\${app.cors.allowed-origins}") private val allowedOriginsList: List<String>,
    @Value("\${app.cors.allowed-methods}") private val allowedMethodsList: List<String>,
    @Value("\${app.cors.allowed-headers}") private val allowedHeadersList: List<String>,
    @Value("\${app.cors.allow-credentials}") private val allowCredentialsProp: Boolean,
    @Value("\${app.cors.max-age}") private val maxAgeProp: Long
) {

    @Bean
    fun securityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .authorizeExchange { auth ->
                auth
                    // Public endpoints
                    .pathMatchers("/actuator/health").permitAll()
                    .pathMatchers("/actuator/info").permitAll()
                    // Webhook
                    .pathMatchers("/api/webhooks/**").permitAll()
                    // Admin endpoints
                    .pathMatchers("/actuator/**").hasRole("ADMIN")
                    // Protected endpoints
                    .pathMatchers("/api/users/**").authenticated()
                    .pathMatchers("/api/sessions/**").hasRole("PLAYER")
                    .pathMatchers("/api/payments/**").hasRole("PLAYER")
                    .pathMatchers("/api/v1/signaling/**").hasRole("PLAYER")
                    .pathMatchers("/api/notifications/**").authenticated()
                    // Default
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(
                        ReactiveJwtAuthenticationConverterAdapter(
                            org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter()
                                .apply {
                                    setJwtGrantedAuthoritiesConverter(keycloakRoleConverter)
                                }
                        )
                    )
                }
            }
            .build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = allowedOriginsList
            allowedMethods = allowedMethodsList
            allowedHeaders = allowedHeadersList
            allowCredentials = allowCredentialsProp
            maxAge = maxAgeProp
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }
}