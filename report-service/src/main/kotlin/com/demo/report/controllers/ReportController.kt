package com.demo.report.controllers

import com.demo.report.domain.SummaryStatus
import com.demo.report.services.ReportService
import com.demo.report.services.SummaryResponse
import com.demo.report.services.toResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import tools.jackson.databind.ObjectMapper
import java.util.*

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