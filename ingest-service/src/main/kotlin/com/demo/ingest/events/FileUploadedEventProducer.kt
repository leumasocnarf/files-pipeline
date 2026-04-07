package com.demo.ingest.events

import com.demo.ingest.domain.IngestedFile
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

data class FileUploadedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val eventType: String = "FILE_UPLOADED",
    val timestamp: Instant = Instant.now(),
    val payload: FileUploadedPayload
)

data class FileUploadedPayload(
    val fileId: UUID?,
    val filename: String,
    val contentType: String,
    val fileSize: Long
)

@Component
class FileUploadedEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, FileUploadedEvent>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishEvent(file: IngestedFile) {
        val event = FileUploadedEvent(
            payload = FileUploadedPayload(
                fileId = file.id,
                filename = file.filename,
                contentType = file.contentType,
                fileSize = file.fileSize
            )
        )

        kafkaTemplate.send("file.uploaded", file.id.toString(), event)
            .whenComplete { result, ex ->
                if (ex != null) log.error("Failed to publish event for file {}: {}", file.id, ex.message)
                else log.info(
                    "Published file.uploaded for file {} at offset {}",
                    file.id,
                    result.recordMetadata.offset()
                )
            }
    }
}