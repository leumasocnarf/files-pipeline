package com.demo.processing.gateways

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.UUID

@Component
class IngestServiceGateway(
    @Value($$"${services.ingest.base-url}") private val baseUrl: String
) {
    private val restClient = RestClient.builder()
        .baseUrl(baseUrl)
        .build()

    fun getFileContent(fileId: UUID): ByteArray = restClient
        .get()
        .uri("/api/v1/uploads/{id}/data", fileId)
        .retrieve()
        .body(ByteArray::class.java)
        ?: throw RuntimeException("Empty response when downloading file $fileId")
}
