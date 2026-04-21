package com.demo.processing.events

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class FileProcessedEventProducer(
    private val kafkaTemplate: KafkaTemplate<String, FileProcessedEvent>
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishEvent(event: FileProcessedEvent) {
        val fileId = event.payload.fileId

        kafkaTemplate.send("file.processed", fileId.toString(), event)
            .whenComplete { _, ex ->
                if (ex != null) {
                    log.error("Failed to publish file.processed event for file {}", fileId, ex)
                } else {
                    log.info("Published file.processed event for file {} with status {}", fileId, event.payload.status)
                }
            }
    }
}