package com.demo.report.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimNames
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.stereotype.Component

@Suppress("UNCHECKED_CAST")
@Component
class JwtConverter(
    @Value($$"${jwt.auth.converter.resource-id}")
    private val resourceId: String?,
    @Value($$"${jwt.auth.converter.principal-attribute}")
    private val principalAttribute: String?
) : Converter<Jwt, AbstractAuthenticationToken> {


    private val jwtGrantedAuthoritiesConverter = JwtGrantedAuthoritiesConverter()

    override fun convert(jwt: Jwt): AbstractAuthenticationToken {
        val authorities = (jwtGrantedAuthoritiesConverter.convert(jwt)
            ?: emptyList()).toSet() + extractRoles(jwt)

        return JwtAuthenticationToken(jwt, authorities, getPrincipalClaimName(jwt))
    }

    private fun getPrincipalClaimName(jwt: Jwt): String? {
        val claimName = principalAttribute ?: JwtClaimNames.SUB
        return jwt.getClaim(claimName)
    }

    private fun extractRoles(jwt: Jwt): Set<GrantedAuthority> {
        val roles = mutableSetOf<String>()

        // Realm roles
        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
        val realmRoles = realmAccess?.get("roles") as? Collection<String>
        if (realmRoles != null) roles.addAll(realmRoles)

        // Client roles
        val resourceAccess = jwt.getClaim<Map<String, Any>>("resource_access")
        val resource = resourceId?.let { resourceAccess?.get(it) as? Map<String, Any> }
        val clientRoles = resource?.get("roles") as? Collection<String>
        if (clientRoles != null) roles.addAll(clientRoles)

        return roles.map { SimpleGrantedAuthority("ROLE_$it") }.toSet()
    }
}