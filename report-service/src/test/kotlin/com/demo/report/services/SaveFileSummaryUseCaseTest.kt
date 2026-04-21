package com.demo.report.services

import com.demo.report.domain.FileSummary
import com.demo.report.domain.FileSummaryRepository
import com.demo.report.domain.SummaryStatus
import com.demo.report.events.FileProcessedPayload
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.Instant
import java.util.*

@ExtendWith(MockitoExtension::class)
class SaveFileSummaryUseCaseTest {

    @Mock
    lateinit var fileSummaryRepository: FileSummaryRepository

    @InjectMocks
    lateinit var useCase: SaveFileSummaryUseCase

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

    @Test
    fun `should save summary when file does not exist`() {
        val payload = validPayload()

        whenever(fileSummaryRepository.existsByFileId(payload.fileId)).thenReturn(false)
        whenever(fileSummaryRepository.save(any<FileSummary>())).thenAnswer { it.arguments[0] }

        useCase.execute(payload)

        verify(fileSummaryRepository).save(any<FileSummary>())
    }

    @Test
    fun `should skip save on duplicate event`() {
        val payload = validPayload()

        whenever(fileSummaryRepository.existsByFileId(payload.fileId)).thenReturn(true)

        useCase.execute(payload)

        verify(fileSummaryRepository, never()).save(any<FileSummary>())
    }

    @Test
    fun `should map payload fields correctly`() {
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
            summaryData = """{"key":"value"}""",
            errorMessage = null,
            processedAt = processedAt
        )

        whenever(fileSummaryRepository.existsByFileId(fileId)).thenReturn(false)
        whenever(fileSummaryRepository.save(any<FileSummary>())).thenAnswer { it.arguments[0] }

        useCase.execute(payload)

        verify(fileSummaryRepository).save(argThat { summary ->
            summary.fileId == fileId &&
                    summary.jobId == jobId &&
                    summary.filename == "test.csv" &&
                    summary.status == SummaryStatus.COMPLETED &&
                    summary.totalRows == 100 &&
                    summary.validRows == 90 &&
                    summary.invalidRows == 10 &&
                    summary.summaryData == """{"key":"value"}""" &&
                    summary.errorMessage == null &&
                    summary.processedAt == processedAt
        })
    }

    @Test
    fun `should propagate repository exceptions`() {
        val payload = validPayload()

        whenever(fileSummaryRepository.existsByFileId(payload.fileId))
            .thenThrow(RuntimeException("db error"))

        assertThrows<RuntimeException> {
            useCase.execute(payload)
        }
    }
}