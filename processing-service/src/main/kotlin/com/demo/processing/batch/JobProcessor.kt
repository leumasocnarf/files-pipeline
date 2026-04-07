package com.demo.processing.batch

import com.demo.processing.events.FileProcessedEvent
import com.demo.processing.events.FileProcessedEventProducer
import com.demo.processing.events.FileProcessedPayload
import com.demo.processing.gateways.IngestServiceGateway
import com.demo.processing.helpers.aggregateData
import com.demo.processing.helpers.parseFile
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class JobProcessor(
    private val jdbc: JdbcTemplate,
    private val ingestServiceGateway: IngestServiceGateway,
    private val fileProcessedEventProducer: FileProcessedEventProducer
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processJob(jobId: UUID, fileId: UUID, filename: String) {

        val fileBytes = ingestServiceGateway.getFileContent(fileId)
        val parsed = parseFile(fileBytes, filename)
        val aggregation = aggregateData(parsed)

        jdbc.update(
            "UPDATE processing_jobs SET status = 'COMPLETED', completed_at = ?, row_count = ?, valid_rows = ?, invalid_rows = ? WHERE id = ?",
            Timestamp.from(Instant.now()), parsed.totalRows, parsed.validRows, parsed.invalidRows, jobId
        )

        fileProcessedEventProducer.publishEvent(
            FileProcessedEvent(
                payload = FileProcessedPayload(
                    fileId = fileId,
                    jobId = jobId,
                    filename = filename,
                    status = "COMPLETED",
                    totalRows = parsed.totalRows,
                    validRows = parsed.validRows,
                    invalidRows = parsed.invalidRows,
                    summaryData = aggregation,
                    errorMessage = null,
                    processedAt = Instant.now()
                )
            )
        )

        log.info("Processed file {} — {} rows, revenue: {}", fileId, parsed.totalRows, aggregation["totalRevenue"])
    }

    @Transactional
    fun failJob(jobId: UUID, fileId: UUID, filename: String, error: String?) {
        jdbc.update(
            "UPDATE processing_jobs SET status = 'FAILED', completed_at = ?, error_message = ? WHERE id = ?",
            Timestamp.from(Instant.now()), error, jobId
        )

        fileProcessedEventProducer.publishEvent(
            FileProcessedEvent(
                payload = FileProcessedPayload(
                    fileId = fileId,
                    jobId = jobId,
                    filename = filename,
                    status = "FAILED",
                    totalRows = 0,
                    validRows = 0,
                    invalidRows = 0,
                    summaryData = null,
                    errorMessage = error,
                    processedAt = Instant.now()
                )
            )
        )
    }
}