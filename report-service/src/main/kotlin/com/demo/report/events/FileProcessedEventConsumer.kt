package com.demo.report.events

import com.demo.report.domain.FileSummary
import com.demo.report.domain.FileSummaryRepository
import com.demo.report.domain.SummaryStatus
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

data class FileProcessedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val eventType: String = "",
    val timestamp: Instant = Instant.now(),
    val payload: FileProcessedPayload = FileProcessedPayload()
)

data class FileProcessedPayload(
    val fileId: UUID = UUID(0, 0),
    val jobId: UUID = UUID(0, 0),
    val filename: String = "",
    val status: String = "",
    val totalRows: Int = 0,
    val validRows: Int = 0,
    val invalidRows: Int = 0,
    val summaryData: Map<String, Any>? = null,
    val errorMessage: String? = null,
    val processedAt: Instant = Instant.now()
)

@Component
class FileProcessedEventConsumer(
    private val fileSummaryRepository: FileSummaryRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["file.processed"], groupId = "report-service")
    @Transactional
    fun handle(event: FileProcessedEvent, ack: Acknowledgment) {
        val p = event.payload
        log.info("Received file.processed for file {} with status {}", p.fileId, p.status)

        try {
            if (fileSummaryRepository.existsByFileId(p.fileId)) {
                log.warn("Duplicate event for file {}, skipping", p.fileId)
                ack.acknowledge()
                return
            }

            fileSummaryRepository.save(
                FileSummary(
                    fileId = p.fileId,
                    jobId = p.jobId,
                    filename = p.filename,
                    status = SummaryStatus.valueOf(p.status),
                    totalRows = p.totalRows,
                    validRows = p.validRows,
                    invalidRows = p.invalidRows,
                    summaryData = p.summaryData?.let {
                        objectMapper.writeValueAsString(it)
                    },
                    errorMessage = p.errorMessage,
                    processedAt = p.processedAt
                )
            )
            ack.acknowledge()
        } catch (e: Exception) {
            log.error("Failed to process event for file {}: {}", p.fileId, e.message)
        }
    }
}