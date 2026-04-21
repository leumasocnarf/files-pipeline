package com.demo.report.services

import com.demo.report.domain.FileSummary
import com.demo.report.domain.FileSummaryRepository
import com.demo.report.domain.SummaryStatus
import com.demo.report.exceptions.SummaryNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.*

data class SummaryResponse(
    val id: UUID,
    val fileId: UUID,
    val jobId: UUID?,
    val filename: String,
    val status: String,
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val summaryData: Map<String, Any>?,
    val errorMessage: String?,
    val processedAt: Instant
)

fun FileSummary.toResponse(objectMapper: ObjectMapper) = SummaryResponse(
    id = id,
    fileId = fileId,
    jobId = jobId,
    filename = filename,
    status = status.name,
    totalRows = totalRows,
    validRows = validRows,
    invalidRows = invalidRows,
    summaryData = summaryData?.let {
        objectMapper.readValue(it, object : TypeReference<Map<String, Any>>() {})
    },
    errorMessage = errorMessage,
    processedAt = processedAt,
)

@Service
class ReportService(
    private val fileSummaryRepository: FileSummaryRepository,
) {
    fun listSummaries(status: SummaryStatus?, pageable: Pageable): Page<FileSummary> {
        return status
            ?.let { fileSummaryRepository.findAllByStatus(it, pageable) }
            ?: fileSummaryRepository.findAllByOrderByProcessedAtDesc(pageable)
    }

    fun getSummaryByFileId(fileId: UUID): FileSummary =
        fileSummaryRepository.findByFileId(fileId)
            ?: throw SummaryNotFoundException(fileId)
}