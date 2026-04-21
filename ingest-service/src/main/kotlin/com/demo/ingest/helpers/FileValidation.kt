package com.demo.ingest.helpers

import org.springframework.web.multipart.MultipartFile
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import tools.jackson.databind.ObjectMapper
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Fields required to be present in every uploaded file, regardless of format.
 * These represent the agreed-upon schema for this ingestion context and are
 * validated against headers (CSV), object keys (JSON), and child elements (XML).
 */
private val REQUIRED_FIELDS = setOf("date", "product", "region", "revenue", "quantity")

/**
 * Represents the outcome of a file validation operation.
 *
 * Use [Valid] when all structural and field-level checks pass.
 * Use [Invalid] when one or more errors are found — errors are accumulated
 * rather than fail-fast, so all problems are reported in a single result.
 */
sealed class ValidationResult {

    /**
     * Indicates a successfully validated file.
     *
     * @property rowCount number of data rows found, excluding the header row
     * @property headers  normalised (lowercase, trimmed) column or field names
     */
    data class Valid(val rowCount: Int, val headers: List<String>) : ValidationResult()

    /**
     * Indicates a file that failed validation.
     *
     * @property errors human-readable descriptions of every problem found
     */
    data class Invalid(val errors: List<String>) : ValidationResult() {
        constructor(vararg messages: String) : this(messages.toList())
    }
}

/**
 * Accumulates validation errors and builds a [ValidationResult] at the end of a
 * validation pass. Prefer this over early returns when multiple errors should be
 * collected and reported together.
 */
private class Validator {
    private val errors = mutableListOf<String>()

    /**
     * Adds [message] to the error list if [condition] is false.
     * The message lambda is only evaluated on failure.
     */
    fun require(condition: Boolean, message: () -> String) {
        if (!condition) errors.add(message())
    }

    /**
     * Unconditionally adds an error. Use when the failure condition cannot be
     * expressed as a boolean predicate, e.g. inside a type-check branch.
     */
    fun error(message: () -> String) {
        errors.add(message())
    }

    /**
     * Produces a [ValidationResult.Valid] if no errors were recorded,
     * or a [ValidationResult.Invalid] containing all accumulated errors otherwise.
     *
     * @param rowCount number of data rows in the validated file
     * @param headers  normalised column or field names extracted from the file
     */
    fun build(rowCount: Int, headers: List<String>): ValidationResult =
        if (errors.isEmpty()) ValidationResult.Valid(rowCount, headers)
        else ValidationResult.Invalid(errors)
}


/** Shared [ObjectMapper] instance. Thread-safe for reads; reused across all JSON validations. */
private val objectMapper = ObjectMapper()


/** Adapts a [NodeList] to a [Sequence] for use with Kotlin's collection operators. */
private fun NodeList.asSequence(): Sequence<Node> =
    (0 until length).asSequence().map { item(it) }


/**
 * Returns the set of lowercase element child names for this [Node].
 * Used to extract field names from individual XML `<record>` elements.
 */
private fun Node.elementChildrenNames(): Set<String> =
    childNodes
        .asSequence()
        .filter { it.nodeType == Node.ELEMENT_NODE }
        .map { it.nodeName.lowercase() }
        .toSet()


/**
 * Creates a new [DocumentBuilder] for each invocation.
 * [DocumentBuilder] is not thread-safe and must not be shared across calls.
 */
private fun newDocumentBuilder(): DocumentBuilder =
    DocumentBuilderFactory.newInstance().newDocumentBuilder()


/**
 * Entry point for file validation. Delegates to the appropriate validator based
 * on the file extension. Supports CSV, JSON, and XML.
 *
 * Returns [ValidationResult.Invalid] immediately if the file is empty or the
 * extension is not supported, without attempting to read the content.
 *
 * @param file the uploaded file to validate
 * @return [ValidationResult.Valid] if the file is well-formed and contains all
 *         required fields, or [ValidationResult.Invalid] with a full error list otherwise
 */
fun validateFile(file: MultipartFile): ValidationResult {
    if (file.isEmpty) return ValidationResult.Invalid("File is empty")

    return when (file.extension) {
        "csv" -> file.validateCsv()
        "json" -> file.validateJson()
        "xml" -> file.validateXml()
        else -> ValidationResult.Invalid("Unsupported file type: '${file.extension}'")
    }
}


/** Lowercase file extension derived from [MultipartFile.getOriginalFilename], or empty string if absent. */
private val MultipartFile.extension: String
    get() = originalFilename?.substringAfterLast('.', "")?.lowercase() ?: ""


/** Reads and trims the full file content as a UTF-8 string. Opens a new stream on each call. */
private fun MultipartFile.readText(): String =
    inputStream.bufferedReader().readText().trim()


/** Reads all lines from the file. Opens a new stream on each call. */
private fun MultipartFile.readLines(): List<String> =
    inputStream.bufferedReader().readLines()


/**
 * Validates a CSV file, checking that:
 * - the file contains at least a header row and one data row
 * - all [REQUIRED_FIELDS] are present in the header
 * - every data row has the same number of columns as the header
 *
 * All column errors are accumulated before returning, so a single call
 * reports every malformed row rather than stopping at the first.
 */
private fun MultipartFile.validateCsv(): ValidationResult {
    val lines = readLines()
    if (lines.isEmpty()) return ValidationResult.Invalid("File contains no data")
    if (lines.size < 2) return ValidationResult.Invalid("No data rows")

    val headers = lines.first().split(",").map { it.trim().lowercase() }
    val validator = Validator()

    val missing = REQUIRED_FIELDS - headers.toSet()
    validator.require(missing.isEmpty()) {
        "Missing headers: ${missing.joinToString(", ")}"
    }

    lines.drop(1).forEachIndexed { i, line ->
        val cols = line.split(",").map { it.trim() }
        validator.require(cols.size == headers.size) {
            "Row ${i + 2}: expected ${headers.size} columns, found ${cols.size}"
        }
    }

    return validator.build(rowCount = lines.size - 1, headers = headers)
}

/**
 * Validates a JSON file, checking that:
 * - the content is a non-empty JSON array
 * - every element in the array is an object
 * - every object contains all [REQUIRED_FIELDS]
 *
 * Headers are derived from the keys of the first element.
 * All item-level errors are accumulated before returning.
 *
 * @throws nothing — parse exceptions are caught and returned as [ValidationResult.Invalid]
 */
private fun MultipartFile.validateJson(): ValidationResult {
    return try {
        val content = readText()

        if (content.isEmpty()) return ValidationResult.Invalid("File contains no data")
        if (!content.startsWith("[")) return ValidationResult.Invalid("Expected a JSON array of objects")

        val nodes = objectMapper.readTree(content)

        if (!nodes.isArray) return ValidationResult.Invalid("Expected a JSON array of objects")
        if (nodes.isEmpty) return ValidationResult.Invalid("JSON array is empty")

        val nodeList = nodes.toList()
        val headers = nodeList.first().propertyNames().asSequence().map { it.lowercase() }.toSet()
        val validator = Validator()

        nodeList.forEachIndexed { i, node ->
            if (!node.isObject) {
                validator.error { "Item ${i + 1}: expected an object" }
            } else {
                val missing = REQUIRED_FIELDS - node.propertyNames().asSequence().map { it.lowercase() }.toSet()
                validator.require(missing.isEmpty()) {
                    "Item ${i + 1}: missing fields: ${missing.joinToString(", ")}"
                }
            }
        }

        validator.build(rowCount = nodes.size(), headers = headers.toList())
    } catch (e: Exception) {
        ValidationResult.Invalid("Invalid JSON: ${e.message}")
    }
}

/**
 * Validates an XML file, checking that:
 * - the content is well-formed XML
 * - at least one `<record>` element is present
 * - every `<record>` contains all [REQUIRED_FIELDS] as child elements
 *
 * Headers are derived from the child element names of the first `<record>`.
 * All record-level errors are accumulated before returning.
 *
 * @throws nothing — parse exceptions are caught and returned as [ValidationResult.Invalid]
 */
private fun MultipartFile.validateXml(): ValidationResult {
    return try {
        val content = readText()

        if (content.isEmpty()) return ValidationResult.Invalid("File contains no data")

        val document = newDocumentBuilder().parse(content.byteInputStream())
        val records = document.getElementsByTagName("record")

        if (records.length == 0) return ValidationResult.Invalid("No <record> elements found")

        val recordList = (0 until records.length).map { records.item(it) }
        val headers = recordList.first().elementChildrenNames()
        val validator = Validator()

        recordList.forEachIndexed { i, record ->
            val missing = REQUIRED_FIELDS - record.elementChildrenNames()
            validator.require(missing.isEmpty()) {
                "Record ${i + 1}: missing fields: ${missing.joinToString(", ")}"
            }
        }

        validator.build(rowCount = records.length, headers = headers.toList())
    } catch (e: Exception) {
        ValidationResult.Invalid("Invalid XML: ${e.message}")
    }
}