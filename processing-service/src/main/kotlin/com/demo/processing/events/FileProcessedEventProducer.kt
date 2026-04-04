package com.demo.processing.events

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

data class FileProcessedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val eventType: String = "FILE_PROCESSED",
    val timestamp: Instant = Instant.now(),
    val payload: FileProcessedPayload
)

data class FileProcessedPayload(
    val fileId: UUID,
    val jobId: UUID?,
    val filename: String,
    val status: String,
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val summaryData: Map<String, Any>?,
    val errorMessage: String?,
    val processedAt: Instant
)

@Component
class FileProcessedEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, FileProcessedEvent>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishEvent(event: FileProcessedEvent) {
        kafkaTemplate.send(
            "file.processed",
            event.payload.fileId.toString(),
            event
        ).whenComplete { result, ex ->
            if (ex != null) {
                log.error("Failed to publish file.processed event for file {}: {}", event.payload.fileId, ex.message)
            } else {
                log.info(
                    "Published file.processed event for file {} with status {}",
                    event.payload.fileId,
                    event.payload.status
                )
            }
        }
    }
}