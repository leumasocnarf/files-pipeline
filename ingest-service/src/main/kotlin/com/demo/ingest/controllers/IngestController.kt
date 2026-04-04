package com.demo.ingest.controllers

import com.demo.ingest.domain.IngestedFileStatus
import com.demo.ingest.services.FileUploadService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class UploadResponse(
    val id: UUID,
    val filename: String,
    val status: String,
    val rowCount: Int,
    val headers: List<String>,
)

@RestController
@RequestMapping("/api/v1/uploads")
class IngestController(
    private val uploadService: FileUploadService,
) {

    @PostMapping
    fun upload(@RequestParam("file") file: MultipartFile): ResponseEntity<UploadResponse> {
        val (fileUpload, validation) = uploadService.upload(file)

        return ResponseEntity.ok(
            UploadResponse(
                id = fileUpload.id!!,
                filename = fileUpload.filename,
                status = IngestedFileStatus.PUBLISHED.name,
                rowCount = validation.rowCount,
                headers = validation.headers
            )
        )
    }

    @GetMapping("/{id}/data")
    fun downloadFile(@PathVariable id: UUID): ResponseEntity<ByteArray> {
        val fileUpload = uploadService.getFile(id)

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(fileUpload.contentType))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"${fileUpload.filename}\""
            )
            .body(fileUpload.fileData)
    }
}