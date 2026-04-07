package com.demo.report.controllers

import com.demo.report.domain.FileSummary
import com.demo.report.domain.FileSummaryRepository
import com.demo.report.domain.SummaryStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

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

@RestController
@RequestMapping("/api/v1/reports")
class ReportController(
    private val fileSummaryRepository: FileSummaryRepository,
    private val objectMapper: ObjectMapper
) {

    @PreAuthorize("hasRole('report:read')")
    @GetMapping
    fun listSummaries(
        @RequestParam(required = false) status: SummaryStatus?,
        @PageableDefault(size = 20) pageable: Pageable
    ): ResponseEntity<Page<SummaryResponse>> {

        val page = if (status != null) {
            fileSummaryRepository.findAllByStatus(status, pageable)
        } else {
            fileSummaryRepository.findAllByOrderByProcessedAtDesc(pageable)
        }

        return ResponseEntity.ok(page.map { it.toResponse() })
    }

    @PreAuthorize("hasRole('report:read')")
    @GetMapping("/files/{fileId}")
    fun getSummaryByFileId(@PathVariable fileId: UUID): ResponseEntity<SummaryResponse> {

        val summary = fileSummaryRepository.findByFileId(fileId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No summary found for file $fileId")

        return ResponseEntity.ok(summary.toResponse())
    }

    private fun FileSummary.toResponse() = SummaryResponse(
        id = id!!,
        fileId = fileId,
        jobId = jobId,
        filename = filename,
        status = status.name,
        totalRows = totalRows,
        validRows = validRows,
        invalidRows = invalidRows,
        summaryData = summaryData?.let {
            objectMapper.readValue(
                it,
                object : TypeReference<Map<String, Any>>() {})
        },
        errorMessage = errorMessage,
        processedAt = processedAt,
    )
}