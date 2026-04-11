package com.demo.processing.events

import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class FileUploadedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val eventType: String = "",
    val timestamp: Instant = Instant.now(),
    val payload: FileUploadedPayload = FileUploadedPayload()
)

data class FileUploadedPayload(
    val fileId: UUID = UUID(0, 0),
    val filename: String = "",
    val contentType: String = "",
    val fileSize: Long = 0
)

@Component
class FileUploadedEventConsumer(
    private val jdbc: JdbcTemplate
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["file.uploaded"], groupId = "processing-service")
    fun handle(event: FileUploadedEvent, ack: Acknowledgment) {
        log.info("Received file.uploaded event for file {}: {}", event.payload.fileId, event.payload.filename)

        try {
            jdbc.update(
                "INSERT INTO processing_jobs (id, file_id, filename, status, created_at) VALUES (?, ?, ?, 'QUEUED', ?)",
                UUID.randomUUID(), event.payload.fileId, event.payload.filename, Timestamp.from(Instant.now())
            )
            ack.acknowledge()
            log.info("Inserted processing job for file {}", event.payload.fileId)

        } catch (e: DuplicateKeyException) {
            log.warn("Duplicate event for file {}, skipping", event.payload.fileId)
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Failed to process event for file {}: {}", event.payload.fileId, e.message)
        }
    }
}