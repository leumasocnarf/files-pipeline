package com.demo.processing.helpers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertIs

class FileParsingTest {

    @Nested
    inner class Routing {

        @Test
        fun `routes csv by extension`() {
            val csv = "date,product\n2026-01-01,Widget"
            val result = parseFile(csv.toByteArray(), "data.csv")

            assertIs<ParseResult.Success>(result)
            assertEquals(1, result.totalRows)
        }

        @Test
        fun `routes json by extension`() {
            val json = """[{"date":"2026-01-01","product":"Widget"}]"""
            val result = parseFile(json.toByteArray(), "data.json")

            assertIs<ParseResult.Success>(result)
            assertEquals(1, result.totalRows)
        }

        @Test
        fun `routes xml by extension`() {
            val xml = "<records><record><date>2026-01-01</date><product>Widget</product></record></records>"
            val result = parseFile(xml.toByteArray(), "data.xml")

            assertIs<ParseResult.Success>(result)
            assertEquals(1, result.totalRows)
        }

        @Test
        fun `unknown extension returns empty`() {
            assertIs<ParseResult.Empty>(parseFile("content".toByteArray(), "data.txt"))
        }
    }

    @Nested
    inner class CsvParsing {

        private fun parse(vararg rows: String): ParseResult {
            val headers = "date,product,region,revenue,quantity"
            val csv = (listOf(headers) + rows.toList()).joinToString("\n")
            return parseFile(csv.toByteArray(), "data.csv")
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 3, 5])
        fun `row count matches input`(numRows: Int) {
            val rows = (1..numRows).map { "2026-01-01,Widget $it,North,100.0,$it" }
            val result = parse(*rows.toTypedArray())

            assertIs<ParseResult.Success>(result)
            assertEquals(numRows, result.totalRows)
            assertEquals(numRows, result.validRows)
        }

        @ParameterizedTest
        @CsvSource("product,Widget", "region,North", "revenue,100.0", "quantity,10")
        fun `extracts fields correctly`(field: String, expected: String) {
            val result = parse("2026-01-01,Widget,North,100.0,10")

            assertIs<ParseResult.Success>(result)
            val row = assertIs<ParsedRow.Valid>(result.rows.first())
            assertEquals(expected, row.data[field])
        }

        @Test
        fun `empty input returns empty result`() {
            assertIs<ParseResult.Empty>(parseFile(ByteArray(0), "data.csv"))
        }

        @Test
        fun `trims whitespace`() {
            val csv = " date , product \n 2026-01-01 , Widget "
            val result = parseFile(csv.toByteArray(), "data.csv")

            assertIs<ParseResult.Success>(result)
            val row = assertIs<ParsedRow.Valid>(result.rows.first())
            assertEquals("Widget", row.data["product"])
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
            val result = parseFile(json.toByteArray(), "data.json")

            assertIs<ParseResult.Success>(result)
            assertEquals(2, result.totalRows)
            assertEquals(2, result.validRows)
        }

        @ParameterizedTest
        @CsvSource("product,A", "region,North", "revenue,100.0", "quantity,10")
        fun `extracts fields correctly`(field: String, expected: String) {
            val json = """[{"date":"2026-01-01","product":"A","region":"North","revenue":"100.0","quantity":"10"}]"""
            val result = parseFile(json.toByteArray(), "data.json")

            assertIs<ParseResult.Success>(result)
            val row = assertIs<ParsedRow.Valid>(result.rows.first())
            assertEquals(expected, row.data[field])
        }

        @Test
        fun `empty input returns empty result`() {
            assertIs<ParseResult.Empty>(parseFile(ByteArray(0), "data.json"))
        }

        @Test
        fun `empty array returns empty result`() {
            assertIs<ParseResult.Empty>(parseFile("[]".toByteArray(), "data.json"))
        }

        @Test
        fun `invalid json returns empty result`() {
            assertIs<ParseResult.Empty>(parseFile("{broken".toByteArray(), "data.json"))
        }

        @Test
        fun `collects all field names as headers`() {
            val json = """[{"date":"2026-01-01","product":"A","region":"North"}]"""
            val result = parseFile(json.toByteArray(), "data.json")

            assertIs<ParseResult.Success>(result)
            assertTrue(result.headers.containsAll(listOf("date", "product", "region")))
        }
    }

    @Nested
    inner class XmlParsing {

        private fun wrap(vararg records: String): ByteArray =
            "<records>${records.joinToString("\n")}</records>".toByteArray()

        private fun record(date: String, product: String, region: String, revenue: String, quantity: String) =
            "<record><date>$date</date><product>$product</product><region>$region</region><revenue>$revenue</revenue><quantity>$quantity</quantity></record>"

        @Test
        fun `parses valid xml`() {
            val result = parseFile(
                wrap(
                    record("2026-01-01", "A", "North", "100.0", "10"),
                    record("2026-01-02", "B", "South", "200.0", "20")
                ),
                "data.xml"
            )

            assertIs<ParseResult.Success>(result)
            assertEquals(2, result.totalRows)
            assertEquals(2, result.validRows)
        }

        @ParameterizedTest
        @CsvSource("product,Widget", "region,North", "revenue,100.0", "quantity,10")
        fun `extracts fields correctly`(field: String, expected: String) {
            val result = parseFile(wrap(record("2026-01-01", "Widget", "North", "100.0", "10")), "data.xml")

            assertIs<ParseResult.Success>(result)
            val row = assertIs<ParsedRow.Valid>(result.rows.first())
            assertEquals(expected, row.data[field])
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 3, 5])
        fun `row count matches records`(numRecords: Int) {
            val records = (1..numRecords).map { record("2026-01-01", "A", "North", "100.0", "$it") }
            val result = parseFile(wrap(*records.toTypedArray()), "data.xml")

            assertIs<ParseResult.Success>(result)
            assertEquals(numRecords, result.totalRows)
        }

        @Test
        fun `empty input returns empty result`() {
            assertIs<ParseResult.Empty>(parseFile(ByteArray(0), "data.xml"))
        }

        @Test
        fun `no record elements returns empty`() {
            assertIs<ParseResult.Empty>(parseFile("<root><item>data</item></root>".toByteArray(), "data.xml"))
        }

        @Test
        fun `collects field names as headers`() {
            val result = parseFile(wrap(record("2026-01-01", "A", "North", "100.0", "10")), "data.xml")

            assertIs<ParseResult.Success>(result)
            assertTrue(result.headers.containsAll(listOf("date", "product", "region", "revenue", "quantity")))
        }
    }
}