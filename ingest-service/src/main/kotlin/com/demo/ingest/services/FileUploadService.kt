package com.demo.ingest.services

import com.demo.ingest.domain.IngestedFile
import com.demo.ingest.domain.IngestedFileStatus
import com.demo.ingest.domain.IngestedFilesRepository
import com.demo.ingest.events.FileUploadedEventProducer
import com.demo.ingest.exceptions.CsvValidationException
import com.demo.ingest.helpers.ValidationResult
import com.demo.ingest.helpers.validateFile
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.UUID

@Service
class FileUploadService(
    private val ingestedFilesRepository: IngestedFilesRepository,
    private val fileUploadedEventProducer: FileUploadedEventProducer
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun upload(file: MultipartFile): Pair<IngestedFile, ValidationResult> {
        val validation = validateFile(file)

        if (!validation.valid) {
            throw CsvValidationException(validation.errors)
        }

        val ingestedFile = ingestedFilesRepository.save(
            IngestedFile(
                filename = file.originalFilename ?: "unknown",
                contentType = file.contentType ?: "application/octet-stream",
                fileSize = file.size,
                fileData = file.bytes
            )
        )

        try {
            fileUploadedEventProducer.publishEvent(ingestedFile)
            ingestedFilesRepository.updateStatus(ingestedFile.id!!, IngestedFileStatus.PUBLISHED)

        } catch (ex: Exception) {
            log.error("Failed to publish event for file {}: {}", ingestedFile.id, ex.message)
            ingestedFilesRepository.updateStatus(ingestedFile.id!!, IngestedFileStatus.FAILED)
        }

        return Pair(ingestedFile, validation)
    }

    fun getFile(id: UUID): IngestedFile =
        ingestedFilesRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
}