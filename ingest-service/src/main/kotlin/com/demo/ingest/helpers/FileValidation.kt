package com.demo.ingest.helpers

import org.springframework.web.multipart.MultipartFile
import tools.jackson.databind.ObjectMapper

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
        filename.endsWith(".json", ignoreCase = true) -> validateJson(file)
        else -> invalid("Unsupported file type: '${filename.substringAfterLast('.', "unknown")}'")
    }
}

private val REQUIRED_FIELDS = setOf("date", "product", "region", "revenue", "quantity")

private fun validateCsv(file: MultipartFile): ValidationResult {

    val lines = file.inputStream.bufferedReader().readLines()
    if (lines.isEmpty()) return invalid("File contains no data")

    val headers = lines.first().split(",").map { it.trim().lowercase() }
    val missing = REQUIRED_FIELDS - headers.toSet()

    if (missing.isNotEmpty()) return invalid("Missing headers: ${missing.joinToString(", ")}")

    if (lines.size < 2) return invalid("No data rows")

    val errors = lines.drop(1).mapIndexedNotNull { i, line ->
        val cols = line.split(",").map { it.trim() }
        if (cols.size != headers.size) "Row ${i + 2}: expected ${headers.size} columns, found ${cols.size}" else null
    }

    return ValidationResult(valid = errors.isEmpty(), errors = errors, rowCount = lines.size - 1, headers = headers)
}

private fun validateJson(file: MultipartFile): ValidationResult {
    return try {
        val content = file.inputStream.bufferedReader().readText().trim()

        if (content.isEmpty()) return invalid("File contains no data")

        // Must be an array of objects
        if (!content.startsWith("[")) return invalid("Expected a JSON array of objects")

        val mapper = ObjectMapper()
        val nodes = mapper.readTree(content)

        if (!nodes.isArray) return invalid("Expected a JSON array of objects")
        if (nodes.isEmpty) return invalid("JSON array is empty")

        val errors = mutableListOf<String>()
        val headers = mutableSetOf<String>()

        nodes.forEachIndexed { i, node ->
            if (!node.isObject) {
                errors.add("Item ${i + 1}: expected an object")
                return@forEachIndexed
            }
            val fields = node.propertyNames().asSequence().map { it.lowercase() }.toSet()
            if (i == 0) headers.addAll(fields)
            val missing = REQUIRED_FIELDS - fields
            if (missing.isNotEmpty()) {
                errors.add("Item ${i + 1}: missing fields: ${missing.joinToString(", ")}")
            }
        }

        ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            rowCount = nodes.size(),
            headers = headers.toList()
        )
    } catch (e: Exception) {
        invalid("Invalid JSON: ${e.message}")
    }
}

private fun invalid(message: String) = ValidationResult(valid = false, errors = listOf(message))
