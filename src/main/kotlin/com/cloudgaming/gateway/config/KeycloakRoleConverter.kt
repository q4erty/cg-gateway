package com.cloudgaming.gateway.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

@Component
class KeycloakRoleConverter : Converter<Jwt, Collection<GrantedAuthority>> {

    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
        @Suppress("UNCHECKED_CAST")
        val realmAccess = jwt.claims["realm_access"] as? Map<String, Any>
            ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        val roles = realmAccess["roles"] as? List<String>
            ?: return emptyList()

        return roles.map { roleName ->
            SimpleGrantedAuthority("ROLE_${roleName.uppercase()}")
        }
    }
}