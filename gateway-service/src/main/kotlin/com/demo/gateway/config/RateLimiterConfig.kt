package com.demo.gateway.config

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Mono
import java.util.Base64

@Configuration
class RateLimiterConfig {

    @Bean
    fun userKeyResolver(): KeyResolver {
        return KeyResolver { exchange ->
            Mono.justOrEmpty(
                exchange.request.headers.getFirst("Authorization")
                    ?.removePrefix("Bearer ")
                    ?.let { token ->
                        // Extract subject from JWT without full decoding
                        try {
                            val payload = token.split(".")[1]
                            val decoded = String(Base64.getUrlDecoder().decode(payload))
                            val sub = Regex(""""sub"\s*:\s*"([^"]+)"""").find(decoded)?.groupValues?.get(1)
                            sub
                        } catch (e: Exception) {
                            null
                        }
                    }
            ).defaultIfEmpty(exchange.request.remoteAddress?.address?.hostAddress ?: "anonymous")
        }
    }
}