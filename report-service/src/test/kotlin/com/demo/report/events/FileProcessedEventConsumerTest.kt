package com.demo.report.events

import com.demo.report.domain.FileSummaryRepository
import com.demo.report.domain.SummaryStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.kafka.support.Acknowledgment
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class FileProcessedEventConsumerTest {

    @Mock
    lateinit var fileSummaryRepository: FileSummaryRepository

    @Mock
    lateinit var objectMapper: ObjectMapper

    @Mock
    lateinit var ack: Acknowledgment

    @InjectMocks
    lateinit var consumer: FileProcessedEventConsumer

    @Test
    fun `should not acknowledge on repository failure`() {
        val event = FileProcessedEvent()

        whenever(fileSummaryRepository.existsByFileId(any()))
            .thenThrow(RuntimeException("db error"))

        consumer.handle(event, ack)

        verify(ack, never()).acknowledge()
    }

    @Test
    fun `should save summary and acknowledge on success`() {
        val event = FileProcessedEvent(
            payload = FileProcessedPayload(
                status = "COMPLETED"
            )
        )

        whenever(fileSummaryRepository.existsByFileId(any())).thenReturn(false)
        whenever(fileSummaryRepository.save(any())).thenAnswer { it.arguments[0] }

        consumer.handle(event, ack)

        verify(fileSummaryRepository).save(any())
        verify(ack).acknowledge()
    }

    @Test
    fun `should acknowledge and skip on duplicate event`() {
        val event = FileProcessedEvent()

        whenever(fileSummaryRepository.existsByFileId(any())).thenReturn(true)

        consumer.handle(event, ack)

        verify(fileSummaryRepository, never()).save(any())
        verify(ack).acknowledge()
    }

    @Test
    fun `should serialize summaryData when present`() {
        val summaryData = mapOf("key" to "value")
        val event = FileProcessedEvent(
            payload = FileProcessedPayload(
                summaryData = summaryData,
                status = "COMPLETED"
            )
        )
        val serialized = """{"key":"value"}"""

        whenever(fileSummaryRepository.existsByFileId(any())).thenReturn(false)
        whenever(objectMapper.writeValueAsString(summaryData)).thenReturn(serialized)
        whenever(fileSummaryRepository.save(any())).thenAnswer { invocation -> invocation.arguments[0] }

        consumer.handle(event, ack)

        verify(objectMapper).writeValueAsString(summaryData)
        verify(fileSummaryRepository).save(argThat { summary -> summary.summaryData == serialized })
    }

    @Test
    fun `should not serialize summaryData when null`() {
        val event = FileProcessedEvent(
            payload = FileProcessedPayload(
                summaryData = null,
                status = "COMPLETED"
            )
        )

        whenever(fileSummaryRepository.existsByFileId(any())).thenReturn(false)
        whenever(fileSummaryRepository.save(any())).thenAnswer { invocation -> invocation.arguments[0] }

        consumer.handle(event, ack)

        verify(objectMapper, never()).writeValueAsString(any())
        verify(fileSummaryRepository).save(argThat { summary -> summary.summaryData == null })
    }

    @Test
    fun `should save correct fields from event payload`() {
        val fileId = UUID.randomUUID()
        val jobId = UUID.randomUUID()
        val processedAt = Instant.now()
        val event = FileProcessedEvent(
            payload = FileProcessedPayload(
                fileId = fileId,
                jobId = jobId,
                filename = "test.csv",
                status = "COMPLETED",
                totalRows = 100,
                validRows = 90,
                invalidRows = 10,
                errorMessage = null,
                processedAt = processedAt
            )
        )

        whenever(fileSummaryRepository.existsByFileId(any())).thenReturn(false)
        whenever(fileSummaryRepository.save(any())).thenAnswer { it.arguments[0] }

        consumer.handle(event, ack)

        verify(fileSummaryRepository).save(argThat { summary ->
            summary.fileId == fileId &&
                    summary.jobId == jobId &&
                    summary.filename == "test.csv" &&
                    summary.status == SummaryStatus.COMPLETED &&
                    summary.totalRows == 100 &&
                    summary.validRows == 90 &&
                    summary.invalidRows == 10 &&
                    summary.errorMessage == null &&
                    summary.processedAt == processedAt
        })
    }
}