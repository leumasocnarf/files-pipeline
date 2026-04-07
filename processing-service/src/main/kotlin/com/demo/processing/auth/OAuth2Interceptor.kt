package com.demo.processing.auth

import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.web.client.RestTemplate

class OAuth2Interceptor(
    private val authorizedClientManager: OAuth2AuthorizedClientManager
) : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val clientRegistrationId = "processing-service"

        // Use a simple UsernamePasswordAuthenticationToken as a dummy principal
        val principal: Authentication = UsernamePasswordAuthenticationToken(clientRegistrationId, null, emptyList())

        val client = authorizedClientManager.authorize(
            OAuth2AuthorizeRequest.withClientRegistrationId(clientRegistrationId)
                .principal(principal)
                .build()
        ) ?: throw RuntimeException("Cannot obtain access token")

        request.headers.setBearerAuth(client.accessToken.tokenValue)

        return execution.execute(request, body)
    }
}


@Configuration
class OAuth2ClientConfig(
    private val clientRegistrationRepository: ClientRegistrationRepository,
    private val authorizedClientService: OAuth2AuthorizedClientService
) {

    @Bean
    fun authorizedClientManager(): OAuth2AuthorizedClientManager {
        val manager = AuthorizedClientServiceOAuth2AuthorizedClientManager(
            clientRegistrationRepository,
            authorizedClientService
        )
        manager.setAuthorizedClientProvider(
            org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build()
        )
        return manager
    }

    @Bean
    fun restTemplate(authorizedClientManager: OAuth2AuthorizedClientManager): RestTemplate {
        return RestTemplateBuilder()
            .interceptors(OAuth2Interceptor(authorizedClientManager))
            .build()
    }
}