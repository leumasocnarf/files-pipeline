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

        // Find oldest queued jobs, claim them by marking as processing (skip locked ones), and return them
        val batch = jdbc.queryForList(
            """
        UPDATE processing_jobs 
        SET status = 'PROCESSING', started_at = ?
        WHERE id IN (
            SELECT id FROM processing_jobs 
            WHERE status = 'QUEUED' 
            ORDER BY created_at ASC 
            LIMIT ?
            FOR UPDATE SKIP LOCKED
        )
        RETURNING id, file_id, filename
        """,
            Timestamp.from(Instant.now()), maxBatchSize
        )

        if (batch.isNotEmpty()) {
            log.info("Processing batch of {} files", batch.size)

            batch.forEach { row ->
                val jobId = row["id"] as UUID
                val fileId = row["file_id"] as UUID
                val filename = row["filename"] as String

                try {
                    jobProcessor.processJob(jobId, fileId, filename)

                } catch (ex: Exception) {
                    log.error("Failed to process job {} for file {}: {}", jobId, fileId, ex.message)

                    try {
                        jobProcessor.failJob(jobId, fileId, filename, ex.message)

                    } catch (e: Exception) {
                        log.error("Failed to mark job {} as failed: {}", jobId, e.message)
                    }
                }
            }
        }
    }
}