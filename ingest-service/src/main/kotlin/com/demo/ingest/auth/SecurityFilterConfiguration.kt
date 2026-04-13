package com.demo.ingest.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.client.RestOperations
import org.springframework.web.client.RestTemplate

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityFilterConfiguration(
    @Value($$"${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val jwtIssuerUri: String,
    @Value($$"${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private val jwkSetUri: String,
    private var jwtConverter: JwtConverter,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun jwtRestOperations(): RestOperations {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(10_000)
            setReadTimeout(30_000)
        }
        return RestTemplate(factory)
    }

    @Bean
    fun jwtDecoder(jwtRestOperations: RestOperations): JwtDecoder {
        return NimbusJwtDecoder
            .withJwkSetUri(jwkSetUri)
            .restOperations(jwtRestOperations)
            .build().apply {
                setJwtValidator(JwtValidators.createDefaultWithIssuer(jwtIssuerUri))
            }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity, jwtDecoder: JwtDecoder): SecurityFilterChain {
        return http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/v1/**").authenticated()
                    .requestMatchers("/actuator/**").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2
                    .jwt { jwt ->
                        jwt.decoder(jwtDecoder)
                        jwt.jwtAuthenticationConverter(jwtConverter)
                    }
            }
            .build()
    }
}