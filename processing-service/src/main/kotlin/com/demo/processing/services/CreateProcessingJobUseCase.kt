package com.demo.processing.services

import com.demo.processing.batch.JobStatus
import com.demo.processing.events.FileUploadedPayload
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Service
class CreateProcessingJobUseCase(
    private val jdbc: JdbcTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun execute(payload: FileUploadedPayload) {
        val inserted = jdbc.update(
            """
            INSERT INTO processing_jobs (id, file_id, filename, status, created_at)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (file_id) DO NOTHING
            """.trimIndent(),
            UUID.randomUUID(),
            payload.fileId,
            payload.filename,
            JobStatus.QUEUED.name,
            Timestamp.from(Instant.now()),
        )

        if (inserted == 0) {
            log.warn("Duplicate event for file {}, skipping", payload.fileId)
        }
    }
}