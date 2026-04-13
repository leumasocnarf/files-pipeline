package com.demo.processing.events

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.kafka.support.Acknowledgment
import java.sql.Timestamp
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class FileUploadedEventConsumerTest {

    @Mock
    lateinit var jdbc: JdbcTemplate

    @Mock
    lateinit var ack: Acknowledgment

    @InjectMocks
    lateinit var consumer: FileUploadedEventConsumer

    @Test
    fun `should insert job and acknowledge on valid event`() {
        val event = FileUploadedEvent(
            eventId = UUID.randomUUID(),
            eventType = "FILE_UPLOADED",
            timestamp = Instant.now(),
            payload = FileUploadedPayload(
                fileId = UUID.randomUUID(),
                filename = "test.csv",
                contentType = "text/csv",
                fileSize = 1024L
            )
        )

        consumer.handle(event, ack)

        verify(jdbc).update(any(), any(), any(), any(), any())
        verify(ack).acknowledge()
    }

    @Test
    fun `should not acknowledge on database failure`() {
        val event = FileUploadedEvent(
            eventId = UUID.randomUUID(),
            eventType = "FILE_UPLOADED",
            timestamp = Instant.now(),
            payload = FileUploadedPayload(
                fileId = UUID.randomUUID(),
                filename = "test.csv",
                contentType = "text/csv",
                fileSize = 1024L
            )
        )

        `when`(jdbc.update(any(), any(), any(), any(), any()))
            .thenThrow(DataIntegrityViolationException("db error"))

        consumer.handle(event, ack)

        verify(ack, never()).acknowledge()
    }

    @Test
    fun `should acknowledge duplicate events`() {
        val event = FileUploadedEvent(
            eventId = UUID.randomUUID(),
            eventType = "FILE_UPLOADED",
            timestamp = Instant.now(),
            payload = FileUploadedPayload(
                fileId = UUID.randomUUID(),
                filename = "test.csv",
                contentType = "text/csv",
                fileSize = 1024L
            )
        )

        `when`(jdbc.update(any(), any(), any(), any(), any()))
            .thenThrow(DuplicateKeyException::class.java)

        consumer.handle(event, ack)

        verify(ack).acknowledge()
    }

    @Test
    fun `should pass correct sql arguments`() {
        val fileId = UUID.randomUUID()
        val event = FileUploadedEvent(
            eventId = UUID.randomUUID(),
            eventType = "FILE_UPLOADED",
            timestamp = Instant.now(),
            payload = FileUploadedPayload(
                fileId = fileId,
                filename = "test.csv",
                contentType = "text/csv",
                fileSize = 1024L
            )
        )

        `when`(jdbc.update(any(), any(), any(), any(), any()))
            .thenReturn(1)

        consumer.handle(event, ack)

        verify(jdbc).update(
            eq("INSERT INTO processing_jobs (id, file_id, filename, status, created_at) VALUES (?, ?, ?, 'QUEUED', ?)"),
            any(UUID::class.java),
            eq(fileId),
            eq("test.csv"),
            any(Timestamp::class.java)
        )
    }
}