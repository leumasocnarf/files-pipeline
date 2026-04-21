package com.demo.report.services

import com.demo.report.domain.FileSummary
import com.demo.report.domain.FileSummaryRepository
import com.demo.report.domain.SummaryStatus
import com.demo.report.events.FileProcessedPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SaveFileSummaryUseCase(
    private val fileSummaryRepository: FileSummaryRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun execute(payload: FileProcessedPayload) {
        if (fileSummaryRepository.existsByFileId(payload.fileId)) {
            log.warn("Duplicate event for file {}, skipping", payload.fileId)
            return
        }

        fileSummaryRepository.save(payload.toFileSummary())
    }
}

fun FileProcessedPayload.toFileSummary() = FileSummary(
    fileId = fileId,
    jobId = jobId,
    filename = filename,
    status = SummaryStatus.valueOf(status),
    totalRows = totalRows,
    validRows = validRows,
    invalidRows = invalidRows,
    summaryData = summaryData,
    errorMessage = errorMessage,
    processedAt = processedAt,
)