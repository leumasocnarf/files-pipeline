package com.demo.processing.helpers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class FileParsingTest {

    @Nested
    inner class Routing {

        @Test
        fun `routes csv by extension`() {
            val csv = "date,product\n2026-01-01,Widget"
            val result = parseFile(csv.toByteArray(), "data.csv")

            assertEquals(1, result.totalRows)
        }

        @Test
        fun `routes json by extension`() {
            val json = """[{"date":"2026-01-01","product":"Widget"}]"""
            val result = parseFile(json.toByteArray(), "data.json")

            assertEquals(1, result.totalRows)
        }

        @Test
        fun `unknown extension returns empty`() {
            val result = parseFile("content".toByteArray(), "data.txt")

            assertEquals(0, result.totalRows)
        }
    }

    @Nested
    inner class CsvParsing {

        private fun parse(vararg rows: String): ParseResult {
            val headers = "date,product,region,revenue,quantity"
            val csv = (listOf(headers) + rows.toList()).joinToString("\n")
            return parseCsv(csv.toByteArray())
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 3, 5])
        fun `row count matches input`(numRows: Int) {
            val rows = (1..numRows).map { "2026-01-01,Widget $it,North,100.0,$it" }
            val result = parse(*rows.toTypedArray())

            assertEquals(numRows, result.totalRows)
            assertEquals(numRows, result.validRows)
        }

        @ParameterizedTest
        @CsvSource("product,Widget", "region,North", "revenue,100.0", "quantity,10")
        fun `extracts fields correctly`(field: String, expected: String) {
            val result = parse("2026-01-01,Widget,North,100.0,10")

            assertEquals(expected, result.rows.first().data[field])
        }

        @Test
        fun `empty input returns empty result`() {
            val result = parseCsv(ByteArray(0))

            assertEquals(0, result.totalRows)
            assertTrue(result.headers.isEmpty())
        }

        @Test
        fun `trims whitespace`() {
            val csv = " date , product \n 2026-01-01 , Widget "
            val result = parseCsv(csv.toByteArray())

            assertEquals("Widget", result.rows.first().data["product"])
        }
    }

    @Nested
    inner class JsonParsing {

        @Test
        fun `parses valid json array`() {
            val json = """[
                {"date":"2026-01-01","product":"A","region":"North","revenue":"100.0","quantity":"10"},
                {"date":"2026-01-02","product":"B","region":"South","revenue":"200.0","quantity":"20"}
            ]"""
            val result = parseJson(json.toByteArray())

            assertEquals(2, result.totalRows)
            assertEquals(2, result.validRows)
        }

        @ParameterizedTest
        @CsvSource("product,A", "region,North", "revenue,100.0", "quantity,10")
        fun `extracts fields correctly`(field: String, expected: String) {
            val json = """[{"date":"2026-01-01","product":"A","region":"North","revenue":"100.0","quantity":"10"}]"""
            val result = parseJson(json.toByteArray())

            assertEquals(expected, result.rows.first().data[field])
        }

        @Test
        fun `empty input returns empty result`() {
            val result = parseJson(ByteArray(0))

            assertEquals(0, result.totalRows)
        }

        @Test
        fun `empty array returns empty result`() {
            val result = parseJson("[]".toByteArray())

            assertEquals(0, result.totalRows)
        }

        @Test
        fun `invalid json returns empty result`() {
            val result = parseJson("{broken".toByteArray())

            assertEquals(0, result.totalRows)
        }

        @Test
        fun `collects all field names as headers`() {
            val json = """[{"date":"2026-01-01","product":"A","region":"North"}]"""
            val result = parseJson(json.toByteArray())

            assertTrue(result.headers.containsAll(listOf("date", "product", "region")))
        }
    }
}