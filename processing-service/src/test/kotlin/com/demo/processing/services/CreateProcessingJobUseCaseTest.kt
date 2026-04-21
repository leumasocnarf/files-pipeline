package com.demo.processing.services

import com.demo.processing.events.FileUploadedPayload
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.contains
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.util.*

@ExtendWith(MockitoExtension::class)
class CreateProcessingJobUseCaseTest {

    @Mock
    lateinit var jdbc: JdbcTemplate

    @InjectMocks
    lateinit var useCase: CreateProcessingJobUseCase

    private val payload = FileUploadedPayload(
        fileId = UUID.randomUUID(),
        filename = "test.csv",
        contentType = "text/csv",
        fileSize = 1024L
    )

    @Test
    fun `should insert job with correct arguments`() {
        whenever(jdbc.update(any<String>(), any(), any(), any(), any(), any()))
            .thenReturn(1)

        useCase.execute(payload)

        verify(jdbc).update(
            contains("INSERT INTO processing_jobs"),
            any<UUID>(),
            eq(payload.fileId),
            eq(payload.filename),
            eq("QUEUED"),
            any<Timestamp>()
        )
    }

    @Test
    fun `should handle duplicate file gracefully`() {
        whenever(jdbc.update(any<String>(), any(), any(), any(), any(), any()))
            .thenReturn(0)

        // Should not throw
        useCase.execute(payload)

        verify(jdbc).update(any<String>(), any(), any(), any(), any(), any())
    }

    @Test
    fun `should propagate database exceptions`() {
        doThrow(DataIntegrityViolationException("db error"))
            .whenever(jdbc).update(any<String>(), any(), any(), any(), any(), any())

        assertThrows<DataIntegrityViolationException> {
            useCase.execute(payload)
        }
    }
}