package com.demo.processing.gateways

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.util.*

@Component
class IngestServiceGateway(
    private val restTemplate: RestTemplate,
    @Value($$"${services.ingest.base-url}") private val baseUrl: String
) {

    fun getFileContent(fileId: UUID): ByteArray {
        val url = "$baseUrl/api/v1/uploads/$fileId/data"

        return restTemplate.getForObject(url, ByteArray::class.java)
            ?: throw RuntimeException("Empty response when downloading file $fileId")
    }
}
