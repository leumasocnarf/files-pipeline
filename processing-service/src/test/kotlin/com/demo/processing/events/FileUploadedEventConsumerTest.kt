package com.demo.processing.events

import com.demo.processing.services.CreateProcessingJobUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.kafka.support.Acknowledgment
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class FileUploadedEventConsumerTest {

    @Mock
    lateinit var createProcessingJobUseCase: CreateProcessingJobUseCase

    @Mock
    lateinit var ack: Acknowledgment

    @InjectMocks
    lateinit var consumer: FileUploadedEventConsumer

    @Test
    fun `should execute use case and acknowledge on valid event`() {
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

        verify(createProcessingJobUseCase).execute(event.payload)
        verify(ack).acknowledge()
    }

    @Test
    fun `should not acknowledge on use case failure`() {
        val payload = FileUploadedPayload(
            fileId = UUID.randomUUID(),
            filename = "test.csv",
            contentType = "text/csv",
            fileSize = 1024L
        )
        val event = FileUploadedEvent(
            eventId = UUID.randomUUID(),
            eventType = "FILE_UPLOADED",
            timestamp = Instant.now(),
            payload = payload
        )

        doThrow(RuntimeException("processing failed"))
            .whenever(createProcessingJobUseCase).execute(payload)

        consumer.handle(event, ack)

        verify(ack, never()).acknowledge()
    }

    @Test
    fun `should pass correct payload to use case`() {
        val fileId = UUID.randomUUID()
        val payload = FileUploadedPayload(
            fileId = fileId,
            filename = "test.csv",
            contentType = "text/csv",
            fileSize = 1024L
        )
        val event = FileUploadedEvent(
            eventId = UUID.randomUUID(),
            eventType = "FILE_UPLOADED",
            timestamp = Instant.now(),
            payload = payload
        )

        consumer.handle(event, ack)

        verify(createProcessingJobUseCase).execute(payload)
        verify(ack).acknowledge()
    }
}