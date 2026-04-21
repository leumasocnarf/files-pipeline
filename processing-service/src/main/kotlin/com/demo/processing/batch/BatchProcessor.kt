package com.demo.processing.batch

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class BatchProcessor(
    private val jdbc: JdbcTemplate,
    private val jobProcessor: JobProcessor,
    @Value($$"${processing.batch.max-size}") private val maxBatchSize: Int
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = $$"${processing.batch.interval-ms}")
    fun processBatch() {
        val batch = claimQueuedJobs()
        if (batch.isEmpty()) return

        log.info("Processing batch of {} files", batch.size)

        batch.forEach { job ->
            try {
                jobProcessor.processJob(job)
            } catch (ex: Exception) {
                log.error("Failed to process job {} for file {}", job.id, job.fileId, ex)
                try {
                    jobProcessor.finalizeJob(job, JobStatus.FAILED, errorMessage = ex.message)
                } catch (e: Exception) {
                    log.error("Failed to mark job {} as failed", job.id, e)
                }
            }
        }
    }

    private fun claimQueuedJobs(): List<QueuedJob> =
        jdbc.queryForList(
            """
            UPDATE processing_jobs
            SET status = ?, started_at = ?
            WHERE id IN (
                SELECT id FROM processing_jobs
                WHERE status = ?
                ORDER BY created_at ASC
                LIMIT ?
                FOR UPDATE SKIP LOCKED
            )
            RETURNING id, file_id, filename
            """.trimIndent(),
            JobStatus.PROCESSING.name,
            Timestamp.from(Instant.now()),
            JobStatus.QUEUED.name,
            maxBatchSize,
        ).map { row ->
            QueuedJob(
                id = row["id"] as UUID,
                fileId = row["file_id"] as UUID,
                filename = row["filename"] as String,
            )
        }
}