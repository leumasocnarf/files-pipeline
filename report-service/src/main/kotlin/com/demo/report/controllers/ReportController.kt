package com.demo.report.controllers

import com.demo.report.domain.FileSummary
import com.demo.report.domain.SummaryStatus
import com.demo.report.services.ReportService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
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

@RestController
@RequestMapping("/api/v1/reports")
class ReportController(
    private val reportService: ReportService,
    private val objectMapper: ObjectMapper,
) {

    @PreAuthorize("hasRole('report:read')")
    @GetMapping
    fun listSummaries(
        @RequestParam(required = false) status: SummaryStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): PagedResponse<SummaryResponse> =
        reportService.listSummaries(status, pageable)
            .map { it.toResponse(objectMapper) }
            .toPagedResponse()

    @PreAuthorize("hasRole('report:read')")
    @GetMapping("/files/{fileId}")
    fun getSummaryByFileId(@PathVariable fileId: UUID): SummaryResponse =
        reportService.getSummaryByFileId(fileId).toResponse(objectMapper)
}