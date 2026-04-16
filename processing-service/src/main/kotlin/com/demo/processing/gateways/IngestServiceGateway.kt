package com.demo.processing.gateways

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import java.util.*

@Component
class IngestServiceGateway(
    private val restClient: RestClient,
    @Value($$"${services.ingest.base-url}") private val baseUrl: String
) {

    fun getFileContent(fileId: UUID): ByteArray {
        return restClient.get()
            .uri("$baseUrl/api/v1/uploads/$fileId/data")
            .retrieve()
            .body(ByteArray::class.java)
            ?: throw RuntimeException("Empty response when downloading file $fileId")
    }
}