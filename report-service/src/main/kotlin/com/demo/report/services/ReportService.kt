package com.demo.report.services

import com.demo.report.domain.FileSummary
import com.demo.report.domain.FileSummaryRepository
import com.demo.report.domain.SummaryStatus
import com.demo.report.exceptions.SummaryNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import java.util.*

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