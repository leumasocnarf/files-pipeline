package com.demo.ingest.helpers

import org.springframework.web.multipart.MultipartFile
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import tools.jackson.databind.ObjectMapper
import javax.xml.parsers.DocumentBuilderFactory

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
        filename.endsWith(".xml", ignoreCase = true) -> validateXml(file)
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

private fun validateXml(file: MultipartFile): ValidationResult {
    return try {
        val content = file.inputStream.bufferedReader().readText().trim()
        if (content.isEmpty()) return invalid("File contains no data")

        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(content.byteInputStream())

        val records = document.getElementsByTagName("record")
        if (records.length == 0) return invalid("No <record> elements found")

        val errors = mutableListOf<String>()
        val headers = mutableSetOf<String>()

        for (i in 0 until records.length) {
            val fields = records.item(i).elementChildrenNames()

            if (i == 0) headers.addAll(fields)

            val missing = REQUIRED_FIELDS - fields

            if (missing.isNotEmpty()) {
                errors.add("Record ${i + 1}: missing fields: ${missing.joinToString(", ")}")
            }
        }

        ValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            rowCount = records.length,
            headers = headers.toList()
        )
    } catch (e: Exception) {
        invalid("Invalid XML")
    }
}

private fun invalid(message: String) = ValidationResult(valid = false, errors = listOf(message))

private fun NodeList.asSequence(): Sequence<Node> =
    (0 until length).asSequence().map { item(it) }

private fun Node.elementChildrenNames(): Set<String> =
    childNodes
        .asSequence()
        .filter { it.nodeType == Node.ELEMENT_NODE }
        .map { it.nodeName.lowercase() }
        .toSet()