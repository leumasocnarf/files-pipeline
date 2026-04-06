package com.demo.ingest.auth

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
            ?: emptyList()).toSet() + extractResourceRoles(jwt)

        return JwtAuthenticationToken(jwt, authorities, getPrincipalClaimName(jwt))
    }

    private fun getPrincipalClaimName(jwt: Jwt): String? {
        val claimName = principalAttribute ?: JwtClaimNames.SUB
        return jwt.getClaim(claimName)
    }

    private fun extractResourceRoles(jwt: Jwt): Set<GrantedAuthority> {
        val resourceAccess = jwt.getClaim<Map<String, Any>>("resource_access") ?: return emptySet()
        val resource = resourceId?.let { resourceAccess[it] as? Map<String, Any> } ?: return emptySet()
        val roles = resource["roles"] as? Collection<String> ?: return emptySet()

        return roles.map { role -> SimpleGrantedAuthority("ROLE_$role") }.toSet()
    }
}