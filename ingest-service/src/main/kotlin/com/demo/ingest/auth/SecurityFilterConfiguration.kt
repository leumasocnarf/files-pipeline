package com.demo.ingest.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtDecoders
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityFilterConfiguration(
    @Value($$"${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private val jwtIssuerUri: String,
    private var jwtConverter: JwtConverter,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return JwtDecoders.fromIssuerLocation(jwtIssuerUri)
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