package com.demo.processing.helpers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class CsvParsingTest {

    private val validHeaders = "date,product,region,revenue,quantity"

    private fun parse(vararg rows: String): ParseResult {
        val csv = if (rows.isEmpty()) "" else (listOf(validHeaders) + rows.toList()).joinToString("\n")
        return parseCsv(csv.toByteArray())
    }

    @Nested
    inner class BasicParsing {

        @ParameterizedTest
        @ValueSource(ints = [1, 3, 5, 10])
        fun `row count matches input`(numRows: Int) {
            val rows = (1..numRows).map { "2026-01-01,Widget $it,North,100.0,$it" }
            val result = parse(*rows.toTypedArray())

            assertEquals(numRows, result.totalRows)
            assertEquals(numRows, result.validRows)
            assertEquals(0, result.invalidRows)
        }

        @ParameterizedTest
        @CsvSource(
            "product, Widget A",
            "region, North",
            "revenue, 100.0",
            "quantity, 10",
            "date, 2026-01-01"
        )
        fun `extracts field correctly`(field: String, expected: String) {
            val result = parse("2026-01-01,Widget A,North,100.0,10")

            assertEquals(expected, result.rows.first().data[field])
        }

        @Test
        fun `row index starts at 1 and increments`() {
            val result = parse(
                "2026-01-01,A,North,100.0,10",
                "2026-02-01,B,South,200.0,20",
                "2026-03-01,C,East,300.0,30"
            )

            assertEquals(1, result.rows[0].rowIndex)
            assertEquals(2, result.rows[1].rowIndex)
            assertEquals(3, result.rows[2].rowIndex)
        }
    }

    @Nested
    inner class EdgeCases {

        @Test
        fun `empty input returns empty result`() {
            val result = parseCsv(ByteArray(0))

            assertEquals(0, result.totalRows)
            assertTrue(result.headers.isEmpty())
        }

        @Test
        fun `headers only returns no rows`() {
            val result = parseCsv(validHeaders.toByteArray())

            assertEquals(0, result.totalRows)
            assertEquals(5, result.headers.size)
        }

        @Test
        fun `short row produces partial data without failing`() {
            val csv = "$validHeaders\n2026-01-01,Widget"
            val result = parseCsv(csv.toByteArray())

            assertEquals(1, result.totalRows)
            assertTrue(result.rows.first().valid)
            assertEquals("Widget", result.rows.first().data["product"])
            assertNull(result.rows.first().data["revenue"])
        }

        @ParameterizedTest
        @CsvSource(
            "product, Widget",
            "region, North",
            "revenue, 100.0"
        )
        fun `trims whitespace from values`(field: String, expected: String) {
            val csv = " date , product , region , revenue , quantity \n 2026-01-01 , Widget , North , 100.0 , 10 "
            val result = parseCsv(csv.toByteArray())

            assertEquals(expected, result.rows.first().data[field])
        }
    }
}