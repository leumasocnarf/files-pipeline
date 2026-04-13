package com.demo.processing.events

import java.time.Instant
import java.util.*

data class FileProcessedEvent(
    val eventId: UUID,
    val eventType: String,
    val timestamp: Instant,
    val payload: FileProcessedPayload
)

data class FileProcessedPayload(
    val fileId: UUID,
    val jobId: UUID,
    val filename: String,
    val status: String,
    val totalRows: Int,
    val validRows: Int,
    val invalidRows: Int,
    val summaryData: String?,
    val errorMessage: String?,
    val processedAt: Instant
)