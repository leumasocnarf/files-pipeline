package com.demo.processing.events

import com.demo.processing.services.CreateProcessingJobUseCase
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class FileUploadedEventConsumer(
    private val createProcessingJobUseCase: CreateProcessingJobUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["file.uploaded"], groupId = "processing-service")
    fun handle(event: FileUploadedEvent, ack: Acknowledgment) {
        log.info("Received file.uploaded event for file {}: {}", event.payload.fileId, event.payload.filename)

        try {
            // Single write
            createProcessingJobUseCase.execute(event.payload)
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Failed to process event for file {}", event.payload.fileId, e)
            // Don't ack — let Kafka retry based on your consumer config,
            // or explicitly send to a dead-letter topic here
        }
    }
}