package com.demo.ingest.helpers

import org.springframework.web.multipart.MultipartFile

data class ValidationResult(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val rowCount: Int = 0,
    val headers: List<String> = emptyList()
)

fun validateFile(file: MultipartFile): ValidationResult {
    if (file.isEmpty) return invalid("File is empty")

    val filename = file.originalFilename ?: ""

    return when {
        filename.endsWith(".csv", ignoreCase = true) -> validateCsv(file)
        else -> invalid("Unsupported file type: '${filename.substringAfterLast('.', "unknown")}'")
    }
}

private fun validateCsv(file: MultipartFile): ValidationResult {

    val requiredCsvHeaders = setOf("date", "product", "region", "revenue", "quantity")

    val lines = file.inputStream.bufferedReader().readLines()
    if (lines.isEmpty()) return invalid("File contains no data")

    val headers = lines.first().split(",").map { it.trim().lowercase() }
    val missing = requiredCsvHeaders - headers.toSet()

    if (missing.isNotEmpty()) return invalid("Missing headers: ${missing.joinToString(", ")}")

    if (lines.size < 2) return invalid("No data rows")

    val errors = lines.drop(1).mapIndexedNotNull { i, line ->
        val cols = line.split(",").map { it.trim() }
        if (cols.size != headers.size) "Row ${i + 2}: expected ${headers.size} columns, found ${cols.size}" else null
    }

    return ValidationResult(valid = errors.isEmpty(), errors = errors, rowCount = lines.size - 1, headers = headers)
}


private fun invalid(message: String) = ValidationResult(valid = false, errors = listOf(message))
