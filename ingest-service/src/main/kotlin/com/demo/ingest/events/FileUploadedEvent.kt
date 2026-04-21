package com.demo.ingest.events

import com.demo.ingest.domain.IngestedFile
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

fun IngestedFile.toUploadedEvent() = FileUploadedEvent(
    eventId = UUID.randomUUID(),
    eventType = "FILE_UPLOADED",
    timestamp = Instant.now(),
    payload = FileUploadedPayload(
        fileId = id,
        filename = filename,
        contentType = contentType,
        fileSize = fileSize,
    )
)