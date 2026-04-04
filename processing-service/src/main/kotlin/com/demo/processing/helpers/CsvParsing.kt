package com.demo.processing.helpers

import java.io.BufferedReader
import java.io.InputStreamReader

data class ParseResult(val headers: List<String>, val rows: List<CsvRow>) {
    val totalRows get() = rows.size
    val validRows get() = rows.count { it.valid }
    val invalidRows get() = rows.count { !it.valid }
}

data class CsvRow(val rowIndex: Int, val data: Map<String, String>, val valid: Boolean)

fun parseCsv(fileBytes: ByteArray): ParseResult {

    val lines = BufferedReader(InputStreamReader(fileBytes.inputStream())).readLines()

    if (lines.isEmpty()) return ParseResult(emptyList(), emptyList())

    val headers = lines.first().split(",").map { it.trim() }

    val rows = lines.drop(1).mapIndexed { i, line ->
        try {
            val values = line.split(",").map { it.trim() }
            CsvRow(i + 1, headers.zip(values).toMap(), true)

        } catch (_: Exception) {
            CsvRow(i + 1, emptyMap(), false)
        }
    }

    return ParseResult(headers, rows)
}