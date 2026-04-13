package com.demo.ingest.events

import java.time.Instant
import java.util.*

data class FileUploadedEvent(
    val eventId: UUID,
    val eventType: String,
    val timestamp: Instant,
    val payload: FileUploadedPayload
)

data class FileUploadedPayload(
    val fileId: UUID,
    val filename: String,
    val contentType: String,
    val fileSize: Long
)