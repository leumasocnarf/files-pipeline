package com.demo.processing.batch

import com.demo.processing.events.FileProcessedEvent
import com.demo.processing.events.FileProcessedEventProducer
import com.demo.processing.events.FileProcessedPayload
import com.demo.processing.gateways.IngestServiceGateway
import com.demo.processing.helpers.ParseResult
import com.demo.processing.helpers.aggregateData
import com.demo.processing.helpers.parseFile
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

data class QueuedJob(
    val id: UUID,
    val fileId: UUID,
    val filename: String,
)

enum class JobStatus { QUEUED, PROCESSING, COMPLETED, FAILED }

fun QueuedJob.toProcessedEvent(
    status: JobStatus,
    totalRows: Int,
    validRows: Int,
    invalidRows: Int,
    summaryData: String?,
    errorMessage: String?,
    processedAt: Instant,
) = FileProcessedEvent(
    eventId = UUID.randomUUID(),
    eventType = "FILE_PROCESSED",
    timestamp = processedAt,
    payload = FileProcessedPayload(
        fileId = fileId,
        jobId = id,
        filename = filename,
        status = status.name,
        totalRows = totalRows,
        validRows = validRows,
        invalidRows = invalidRows,
        summaryData = summaryData,
        errorMessage = errorMessage,
        processedAt = processedAt,
    )
)

@Service
class JobProcessor(
    private val jdbc: JdbcTemplate,
    private val ingestServiceGateway: IngestServiceGateway,
    private val fileProcessedEventProducer: FileProcessedEventProducer,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun processJob(job: QueuedJob) {
        val fileBytes = ingestServiceGateway.getFileContent(job.fileId)

        when (val parsed = parseFile(fileBytes, job.filename)) {
            is ParseResult.Empty -> {
                log.warn("File {} yielded no parseable content — marking as failed", job.fileId)
                finalizeJob(job, JobStatus.FAILED, errorMessage = "No parseable content")
            }

            is ParseResult.Success -> {
                val aggregation = aggregateData(parsed)
                val aggregationJson = objectMapper.writeValueAsString(aggregation)

                finalizeJob(
                    job = job,
                    status = JobStatus.COMPLETED,
                    totalRows = parsed.totalRows,
                    validRows = parsed.validRows,
                    invalidRows = parsed.invalidRows,
                    summaryData = aggregationJson,
                )

                log.info(
                    "Processed job {} for file {} — rows: {}",
                    job.id,
                    job.fileId,
                    parsed.totalRows,
                )
            }
        }
    }

    /**
     * Finalizes a processing job by updating its status in the database and
     * publishing a [FileProcessedEvent] to Kafka.
     *
     * When called internally from [processJob], this joins the existing transaction
     * since Spring proxies do not intercept self-calls. When called externally
     * (e.g. from [BatchProcessor] on failure), it runs in its own transaction.
     */
    @Transactional
    fun finalizeJob(
        job: QueuedJob,
        status: JobStatus,
        totalRows: Int = 0,
        validRows: Int = 0,
        invalidRows: Int = 0,
        summaryData: String? = null,
        errorMessage: String? = null,
    ) {
        val now = Instant.now()

        // First write
        jdbc.update(
            """
            UPDATE processing_jobs
            SET status = ?, completed_at = ?, row_count = ?, valid_rows = ?, invalid_rows = ?, error_message = ?
            WHERE id = ?
            """.trimIndent(),
            status.name, Timestamp.from(now), totalRows, validRows, invalidRows, errorMessage, job.id,
        )

        // TODO: Dual-write — DB save + Kafka publish are not atomic.
        //  Potential solution - Replace with outbox pattern: persist the event in the same transaction,
        //  let a poller/CDC relay it to Kafka.

        // Second write
        fileProcessedEventProducer.publishEvent(
            job.toProcessedEvent(
                status = status,
                totalRows = totalRows,
                validRows = validRows,
                invalidRows = invalidRows,
                summaryData = summaryData,
                errorMessage = errorMessage,
                processedAt = now,
            )
        )
    }
}