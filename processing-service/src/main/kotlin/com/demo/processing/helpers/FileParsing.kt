package com.demo.processing.helpers

import org.w3c.dom.Node
import org.w3c.dom.NodeList
import tools.jackson.databind.ObjectMapper
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Represents the outcome of a file parse operation.
 *
 * Use [Success] when the file was parsed and contains at least one record.
 * Use [Empty] when the file could not be parsed, was empty, or the format was unsupported.
 */
sealed class ParseResult {

    /**
     * A successfully parsed file with at least one record.
     *
     * @property headers column or field names extracted from the file
     * @property rows    each parsed row, represented as either [ParsedRow.Valid] or [ParsedRow.Invalid]
     */
    data class Success(
        val headers: List<String>,
        val rows: List<ParsedRow>
    ) : ParseResult() {
        val totalRows get() = rows.size
        val validRows get() = rows.count { it is ParsedRow.Valid }
        val invalidRows get() = rows.count { it is ParsedRow.Invalid }
    }

    /**
     * Indicates the file could not be parsed, was empty, or has an unsupported format.
     */
    data object Empty : ParseResult()
}

/**
 * Represents a single parsed row from an uploaded file.
 *
 * Use [Valid] when the row was parsed successfully.
 * Use [Invalid] when the row could not be parsed due to a structural error.
 */
sealed class ParsedRow {
    abstract val rowIndex: Int

    /** A successfully parsed row with its field data mapped by header name. */
    data class Valid(override val rowIndex: Int, val data: Map<String, String>) : ParsedRow()

    /** A row that could not be parsed. */
    data class Invalid(override val rowIndex: Int) : ParsedRow()
}


/** Shared [ObjectMapper] instance. Thread-safe for reads; reused across all JSON parsing. */
private val objectMapper = ObjectMapper()


/** Adapts a [NodeList] to a [Sequence] for use with Kotlin's collection operators. */
private fun NodeList.asSequence(): Sequence<Node> =
    (0 until length).asSequence().map { item(it) }


/** Returns only the direct element-type children of this [Node], skipping text and comment nodes. */
private fun Node.elementChildren(): Sequence<Node> =
    childNodes.asSequence().filter { it.nodeType == Node.ELEMENT_NODE }


/**
 * Creates a new [DocumentBuilder] for each invocation.
 * [DocumentBuilder] is not thread-safe and must not be shared across calls.
 */
private fun newDocumentBuilder(): DocumentBuilder =
    DocumentBuilderFactory.newInstance().newDocumentBuilder()


/** Reads all lines from this [ByteArray] as UTF-8. Opens a new stream on each call. */
private fun ByteArray.readLines(): List<String> =
    inputStream().bufferedReader().readLines()


/** Reads and trims the full content of this [ByteArray] as a UTF-8 string. Opens a new stream on each call. */
private fun ByteArray.readText(): String =
    inputStream().bufferedReader().readText().trim()


/** Returns the lowercase file extension, or an empty string if none is present. */
private fun String.extension(): String =
    substringAfterLast('.', "").lowercase()

/**
 * Entry point for file parsing. Delegates to the appropriate parser based on
 * the file extension. Returns [ParseResult.Empty] for unsupported file types.
 *
 * @param fileBytes raw bytes of the uploaded file
 * @param filename  original filename used to determine the file type
 */
fun parseFile(fileBytes: ByteArray, filename: String): ParseResult =
    when (filename.extension()) {
        "csv" -> fileBytes.parseCsv()
        "json" -> fileBytes.parseJson()
        "xml" -> fileBytes.parseXml()
        else -> ParseResult.Empty
    }


/**
 * Parses a CSV file into a [ParseResult].
 * The first line is treated as the header row. Each subsequent line is parsed
 * into a [ParsedRow.Valid] with values zipped against headers, or a
 * [ParsedRow.Invalid] if parsing fails.
 */
private fun ByteArray.parseCsv(): ParseResult {
    val lines = readLines()
    if (lines.isEmpty()) return ParseResult.Empty

    val headers = lines.first().split(",").map { it.trim() }

    val rows = lines.drop(1).mapIndexed { i, line ->
        try {
            val values = line.split(",").map { it.trim() }
            ParsedRow.Valid(rowIndex = i + 1, data = headers.zip(values).toMap())
        } catch (_: Exception) {
            ParsedRow.Invalid(rowIndex = i + 1)
        }
    }

    return ParseResult.Success(headers, rows)
}

/**
 * Parses a JSON file into a [ParseResult].
 * Expects a non-empty JSON array of objects. Headers are derived from the union
 * of all field names across every object. Each element resolves to a
 * [ParsedRow.Valid] or [ParsedRow.Invalid] depending on whether it can be read.
 *
 * @throws nothing — parse exceptions are caught and return [ParseResult.Empty]
 */
private fun ByteArray.parseJson(): ParseResult {
    return try {
        val nodes = objectMapper.readTree(readText())
        if (!nodes.isArray || nodes.isEmpty) return ParseResult.Empty

        val headers = mutableSetOf<String>()

        val rows = nodes.toList().mapIndexed { i, node ->
            try {
                val data = node.properties().associate { (field, value) ->
                    headers.add(field)
                    field to value.asString()
                }
                ParsedRow.Valid(rowIndex = i + 1, data = data)
            } catch (_: Exception) {
                ParsedRow.Invalid(rowIndex = i + 1)
            }
        }

        ParseResult.Success(headers.toList(), rows)
    } catch (_: Exception) {
        ParseResult.Empty
    }
}

/**
 * Parses an XML file into a [ParseResult].
 * Expects one or more `<record>` elements. Headers are derived from the union
 * of all child element names across every record. Each record resolves to a
 * [ParsedRow.Valid] or [ParsedRow.Invalid] depending on whether it can be read.
 *
 * @throws nothing — parse exceptions are caught and return [ParseResult.Empty]
 */
private fun ByteArray.parseXml(): ParseResult {
    return try {
        val document = newDocumentBuilder().parse(inputStream())
        val records = document.getElementsByTagName("record")
        if (records.length == 0) return ParseResult.Empty

        val headers = mutableSetOf<String>()

        val rows = (0 until records.length).map { i ->
            try {
                val data = records.item(i).elementChildren().associate { child ->
                    headers.add(child.nodeName)
                    child.nodeName to child.textContent.trim()
                }
                ParsedRow.Valid(rowIndex = i + 1, data = data)
            } catch (_: Exception) {
                ParsedRow.Invalid(rowIndex = i + 1)
            }
        }

        ParseResult.Success(headers.toList(), rows)
    } catch (_: Exception) {
        ParseResult.Empty
    }
}