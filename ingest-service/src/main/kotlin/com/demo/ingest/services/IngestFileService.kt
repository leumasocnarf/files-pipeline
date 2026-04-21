package com.demo.ingest.services

import com.demo.ingest.domain.IngestedFile
import com.demo.ingest.domain.IngestedFilesRepository
import com.demo.ingest.events.FileUploadedEventProducer
import com.demo.ingest.events.toUploadedEvent
import com.demo.ingest.exceptions.CsvValidationException
import com.demo.ingest.exceptions.FileNotFoundException
import com.demo.ingest.helpers.ValidationResult
import com.demo.ingest.helpers.validateFile
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*

data class UploadResult(
    val file: IngestedFile,
    val rowCount: Int,
    val headers: List<String>
)

@Service
class IngestFileService(
    private val ingestedFilesRepository: IngestedFilesRepository,
    private val fileUploadedEventProducer: FileUploadedEventProducer,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun upload(file: MultipartFile): UploadResult {
        val valid = when (val validation = validateFile(file)) {
            is ValidationResult.Invalid -> throw CsvValidationException(validation.errors)
            is ValidationResult.Valid -> validation
        }

        val ingestedFile = ingestedFilesRepository.save(
            IngestedFile(
                filename = file.originalFilename ?: "unknown",
                contentType = resolveContentType(file),
                fileSize = file.size,
                fileData = file.bytes,
            )
        )

        // TODO: Dual-write risk — DB save + Kafka publish are not atomic.
        //  Potential solution - Replace with outbox pattern: persist the event in the same transaction,
        //  let a poller/CDC relay it to Kafka.
        log.info("File {} saved successfully", ingestedFile.id)
        fileUploadedEventProducer.publishEvent(ingestedFile.toUploadedEvent())

        return UploadResult(
            file = ingestedFile,
            rowCount = valid.rowCount,
            headers = valid.headers,
        )
    }

    fun getFile(id: UUID): IngestedFile =
        ingestedFilesRepository.findById(id)
            .orElseThrow { FileNotFoundException(id) }

    private fun resolveContentType(file: MultipartFile): String {
        val extensionType = when (file.originalFilename?.substringAfterLast('.', "")?.lowercase()) {
            "csv" -> "text/csv"
            "json" -> "application/json"
            "xml" -> "application/xml"
            else -> null
        }

        return extensionType ?: file.contentType ?: "application/octet-stream"
    }
}