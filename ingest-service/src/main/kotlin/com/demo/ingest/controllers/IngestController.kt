package com.demo.ingest.controllers

import com.demo.ingest.services.IngestFileService
import com.demo.ingest.services.UploadResult
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

data class UploadResponse(
    val id: UUID,
    val filename: String,
    val contentType: String,
    val fileSize: Long,
    val rowCount: Int,
    val headers: List<String>
)

fun UploadResult.toResponse() = UploadResponse(
    id = file.id,
    filename = file.filename,
    contentType = file.contentType,
    fileSize = file.fileSize,
    rowCount = rowCount,
    headers = headers
)

@RestController
@RequestMapping("/api/v1/uploads")
class IngestController(private val uploadService: IngestFileService) {

    @PreAuthorize("hasRole('ingest:write')")
    @PostMapping
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<UploadResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(uploadService.upload(file).toResponse())

    @PreAuthorize("hasRole('ingest:read')")
    @GetMapping("/{id}/data")
    fun downloadFile(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val file = uploadService.getFile(id)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(file.contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${file.filename}\"")
            .body(file.fileData)
    }
}