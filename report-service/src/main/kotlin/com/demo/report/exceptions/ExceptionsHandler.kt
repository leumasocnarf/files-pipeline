package com.demo.report.exceptions

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class ExceptionsHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Insufficient permissions")
        problem.title = "Access Denied"
        return problem
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.message ?: "Bad request")
        problem.title = "Bad Request"
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

    @ExceptionHandler(SummaryNotFoundException::class)
    fun handleNotFound(ex: SummaryNotFoundException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.message.orEmpty()).apply {
            title = "Summary Not Found"
            setProperty("fileId", ex.fileId)
        }
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthentication(ex: AuthenticationException): ProblemDetail {
        val problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid or missing authentication")
        problem.title = "Authentication Error"
        return problem
    }
}