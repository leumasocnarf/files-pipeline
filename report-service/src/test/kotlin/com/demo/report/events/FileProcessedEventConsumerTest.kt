package com.demo.report.events

import com.demo.report.services.SaveFileSummaryUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.support.Acknowledgment
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class FileProcessedEventConsumerTest {

    @Mock
    lateinit var saveFileSummaryUseCase: SaveFileSummaryUseCase

    @Mock
    lateinit var ack: Acknowledgment

    @InjectMocks
    lateinit var consumer: FileProcessedEventConsumer

    private fun validPayload(
        fileId: UUID = UUID.randomUUID(),
        jobId: UUID = UUID.randomUUID(),
        filename: String = "test.csv",
        status: String = "COMPLETED",
        totalRows: Int = 0,
        validRows: Int = 0,
        invalidRows: Int = 0,
        summaryData: String? = null,
        errorMessage: String? = null,
        processedAt: Instant = Instant.now()
    ) = FileProcessedPayload(
        fileId = fileId,
        jobId = jobId,
        filename = filename,
        status = status,
        totalRows = totalRows,
        validRows = validRows,
        invalidRows = invalidRows,
        summaryData = summaryData,
        errorMessage = errorMessage,
        processedAt = processedAt
    )

    private fun validEvent(payload: FileProcessedPayload = validPayload()) = FileProcessedEvent(
        eventId = UUID.randomUUID(),
        eventType = "FILE_PROCESSED",
        timestamp = Instant.now(),
        payload = payload
    )

    @Test
    fun `should execute use case and acknowledge on success`() {
        val event = validEvent()

        consumer.handle(event, ack)

        verify(saveFileSummaryUseCase).execute(event.payload)
        verify(ack).acknowledge()
    }

    @Test
    fun `should not acknowledge on use case failure`() {
        val payload = validPayload()
        val event = validEvent(payload)

        doThrow(RuntimeException("db error"))
            .whenever(saveFileSummaryUseCase).execute(payload)

        consumer.handle(event, ack)

        verify(ack, never()).acknowledge()
    }

    @Test
    fun `should pass correct payload to use case`() {
        val fileId = UUID.randomUUID()
        val jobId = UUID.randomUUID()
        val processedAt = Instant.now()
        val payload = validPayload(
            fileId = fileId,
            jobId = jobId,
            filename = "test.csv",
            status = "COMPLETED",
            totalRows = 100,
            validRows = 90,
            invalidRows = 10,
            processedAt = processedAt
        )
        val event = validEvent(payload)

        consumer.handle(event, ack)

        verify(saveFileSummaryUseCase).execute(payload)
        verify(ack).acknowledge()
    }
}