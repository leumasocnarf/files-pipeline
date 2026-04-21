package com.demo.ingest.events

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class FileUploadedEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, FileUploadedEvent>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishEvent(event: FileUploadedEvent) {
        val fileId = event.payload.fileId

        kafkaTemplate.send("file.uploaded", event.payload.fileId.toString(), event)
            .whenComplete { _, ex ->
                if (ex != null) {
                    log.error("Failed to publish file.uploaded event for file {}: {}", fileId, ex.message)
                } else {
                    log.info("Published file.uploaded event for file {}", fileId)
                }
            }
    }
}