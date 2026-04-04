package com.demo.ingest.exceptions

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class ExceptionsHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CsvValidationException::class)
    fun handleValidation(ex: CsvValidationException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "CSV validation failed")
        problem.title = "Validation Error"
        problem.setProperty("errors", ex.errors)
        return problem
    }

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxSize(ex: MaxUploadSizeExceededException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, "File exceeds maximum upload size")
        problem.title = "File Too Large"
        return problem
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): ProblemDetail {
        if (ex is ResponseStatusException || ex is NoResourceFoundException) {
            throw ex
        }

        log.error("Unexpected error: {}", ex.message, ex)
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error")
        problem.title = "Server Error"

        return problem
    }
}