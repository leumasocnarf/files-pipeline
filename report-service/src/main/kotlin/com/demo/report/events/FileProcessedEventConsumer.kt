package com.demo.report.events

import com.demo.report.services.SaveFileSummaryUseCase
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class FileProcessedEventConsumer(
    private val saveFileSummaryUseCase: SaveFileSummaryUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["file.processed"], groupId = "report-service")
    fun handle(event: FileProcessedEvent, ack: Acknowledgment) {
        log.info("Received file.processed for file {} with status {}", event.payload.fileId, event.payload.status)

        try {
            // Single write
            saveFileSummaryUseCase.execute(event.payload)
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Failed to process event for file {}", event.payload.fileId, e)
            // Don't ack — let Kafka retry based on your consumer config,
            // or explicitly send to a dead-letter topic here
        }
    }
}