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
        kafkaTemplate.send("file.processed", event.payload.fileId.toString(), event)
            .whenComplete { _, ex ->
                if (ex != null) {
                    log.error(
                        "Failed to publish file.processed event for file {}: {}",
                        event.payload.fileId,
                        ex.message
                    )
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