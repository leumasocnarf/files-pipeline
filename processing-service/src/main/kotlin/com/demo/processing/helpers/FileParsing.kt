package com.demo.processing.helpers

import org.w3c.dom.Node
import tools.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.xml.parsers.DocumentBuilderFactory

data class ParseResult(val headers: List<String>, val rows: List<ParsedRow>) {
    val totalRows get() = rows.size
    val validRows get() = rows.count { it.valid }
    val invalidRows get() = rows.count { !it.valid }
}

data class ParsedRow(val rowIndex: Int, val data: Map<String, String>, val valid: Boolean)

fun parseFile(fileBytes: ByteArray, filename: String): ParseResult {
    return when {
        filename.endsWith(".csv", ignoreCase = true) -> parseCsv(fileBytes)
        filename.endsWith(".json", ignoreCase = true) -> parseJson(fileBytes)
        filename.endsWith(".xml", ignoreCase = true) -> parseXml(fileBytes)
        else -> ParseResult(emptyList(), emptyList())
    }
}

fun parseCsv(fileBytes: ByteArray): ParseResult {

    val lines = BufferedReader(InputStreamReader(fileBytes.inputStream())).readLines()

    if (lines.isEmpty()) return ParseResult(emptyList(), emptyList())

    val headers = lines.first().split(",").map { it.trim() }

    val rows = lines.drop(1).mapIndexed { i, line ->
        try {
            val values = line.split(",").map { it.trim() }
            ParsedRow(i + 1, headers.zip(values).toMap(), true)

        } catch (_: Exception) {
            ParsedRow(i + 1, emptyMap(), false)
        }
    }

    return ParseResult(headers, rows)
}

fun parseJson(fileBytes: ByteArray): ParseResult {
    return try {
        val mapper = ObjectMapper()
        val nodes = mapper.readTree(String(fileBytes).trim())

        if (!nodes.isArray || nodes.isEmpty) return ParseResult(emptyList(), emptyList())

        val headers = mutableSetOf<String>()
        val rows = mutableListOf<ParsedRow>()

        nodes.forEachIndexed { i, node ->
            try {
                val data = mutableMapOf<String, String>()
                node.propertyNames().forEach { field ->
                    headers.add(field)
                    data[field] = node.get(field).asString()
                }
                rows.add(ParsedRow(i + 1, data, true))
            } catch (ex: Exception) {
                rows.add(ParsedRow(i + 1, emptyMap(), false))
            }
        }

        ParseResult(headers.toList(), rows)

    } catch (ex: Exception) {
        ParseResult(emptyList(), emptyList())
    }
}

fun parseXml(fileBytes: ByteArray): ParseResult {
    return try {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(fileBytes.inputStream())

        val records = document.getElementsByTagName("record")
        if (records.length == 0) return ParseResult(emptyList(), emptyList())

        val headers = mutableSetOf<String>()
        val rows = mutableListOf<ParsedRow>()

        for (i in 0 until records.length) {
            try {
                val node = records.item(i)
                val data = mutableMapOf<String, String>()
                val children = node.childNodes
                for (j in 0 until children.length) {
                    val child = children.item(j)
                    if (child.nodeType == Node.ELEMENT_NODE) {
                        headers.add(child.nodeName)
                        data[child.nodeName] = child.textContent.trim()
                    }
                }
                rows.add(ParsedRow(i + 1, data, true))
            } catch (ex: Exception) {
                rows.add(ParsedRow(i + 1, emptyMap(), false))
            }
        }

        ParseResult(headers.toList(), rows)
    } catch (ex: Exception) {
        ParseResult(emptyList(), emptyList())
    }
}