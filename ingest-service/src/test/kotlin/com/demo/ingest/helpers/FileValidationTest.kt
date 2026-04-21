package com.demo.ingest.helpers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockMultipartFile
import kotlin.test.assertIs

class FileValidationTest {

    private fun fileOf(content: String, filename: String) =
        MockMultipartFile("file", filename, "text/plain", content.toByteArray())

    private val validCsvHeaders = "date,product,region,revenue,quantity"
    private val validCsvRow = "2026-01-01,Widget,North,100.0,10"

    @Nested
    inner class FileTypeRouting {

        @Test
        fun `empty file fails`() {
            val file = MockMultipartFile("file", "empty.csv", "text/csv", ByteArray(0))
            val result = validateFile(file)

            assertIs<ValidationResult.Invalid>(result)
            assertEquals("File is empty", result.errors.first())
        }

        @ParameterizedTest
        @ValueSource(strings = ["data.txt", "image.png", "report.pdf", "noextension"])
        fun `unsupported file types fail`(filename: String) {
            val result = validateFile(fileOf("content", filename))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("Unsupported file type"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["test.csv", "test.CSV", "test.json", "test.JSON", "test.xml", "test.XML"])
        fun `supported extensions are case insensitive`(filename: String) {
            val content = when {
                filename.endsWith(".csv", ignoreCase = true) -> "$validCsvHeaders\n$validCsvRow"
                filename.endsWith(".json", ignoreCase = true) ->
                    """[{"date":"2026-01-01","product":"A","region":"North","revenue":100,"quantity":10}]"""

                else -> """
                    <records>
                        <record>
                            <date>2026-01-01</date>
                            <product>A</product>
                            <region>North</region>
                            <revenue>100</revenue>
                            <quantity>10</quantity>
                        </record>
                    </records>
                """.trimIndent()
            }

            assertIs<ValidationResult.Valid>(validateFile(fileOf(content, filename)))
        }
    }

    @Nested
    inner class CsvValidation {

        @Test
        fun `valid csv passes`() {
            val result = validateFile(fileOf("$validCsvHeaders\n$validCsvRow", "test.csv"))

            assertIs<ValidationResult.Valid>(result)
            assertEquals(1, result.rowCount)
            assertEquals(5, result.headers.size)
        }

        @Test
        fun `missing headers fails`() {
            val result = validateFile(fileOf("date,product\n2026-01-01,Widget", "test.csv"))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("Missing headers"))
        }

        @Test
        fun `headers only fails`() {
            val result = validateFile(fileOf(validCsvHeaders, "test.csv"))

            assertIs<ValidationResult.Invalid>(result)
            assertEquals("No data rows", result.errors.first())
        }

        @Test
        fun `wrong column count reports row number`() {
            val csv = "$validCsvHeaders\n$validCsvRow\n2026-01-02,Widget"
            val result = validateFile(fileOf(csv, "test.csv"))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("Row 3"))
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 3, 5])
        fun `row count matches data rows`(numRows: Int) {
            val rows = (1..numRows).joinToString("\n") { "2026-01-01,A,North,100.0,$it" }
            val result = validateFile(fileOf("$validCsvHeaders\n$rows", "test.csv"))

            assertIs<ValidationResult.Valid>(result)
            assertEquals(numRows, result.rowCount)
        }
    }

    @Nested
    inner class JsonValidation {

        private fun jsonFile(content: String) = fileOf(content, "test.json")

        @Test
        fun `valid json array passes`() {
            val json = """[
                {"date":"2026-01-01","product":"A","region":"North","revenue":100,"quantity":10},
                {"date":"2026-01-02","product":"B","region":"South","revenue":200,"quantity":20}
            ]"""
            val result = validateFile(jsonFile(json))

            assertIs<ValidationResult.Valid>(result)
            assertEquals(2, result.rowCount)
        }

        @Test
        fun `empty content fails`() {
            val result = validateFile(jsonFile(""))

            assertIs<ValidationResult.Invalid>(result)
            assertEquals("File is empty", result.errors.first())
        }

        @Test
        fun `non array fails`() {
            val result = validateFile(jsonFile("""{"date":"2026-01-01"}"""))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("Expected a JSON array"))
        }

        @Test
        fun `empty array fails`() {
            val result = validateFile(jsonFile("[]"))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("empty"))
        }

        @Test
        fun `missing fields reports item number`() {
            val json = """[
                {"date":"2026-01-01","product":"A","region":"North","revenue":100,"quantity":10},
                {"date":"2026-01-02","product":"B"}
            ]"""
            val result = validateFile(jsonFile(json))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("Item 2"))
            assertTrue(result.errors.first().contains("missing fields"))
        }

        @Test
        fun `non object items are reported`() {
            val json =
                """[{"date":"2026-01-01","product":"A","region":"North","revenue":100,"quantity":10}, "not an object"]"""
            val result = validateFile(jsonFile(json))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("Item 2"))
        }

        @Test
        fun `invalid json fails gracefully`() {
            val result = validateFile(jsonFile("{broken json"))

            assertIs<ValidationResult.Invalid>(result)
            assertEquals("Expected a JSON array of objects", result.errors.first())
        }
    }

    @Nested
    inner class XmlValidation {

        private fun xmlFile(content: String) = fileOf(content, "test.xml")

        @Test
        fun `valid xml passes`() {
            val xml = """
                <records>
                    <record>
                        <date>2026-01-01</date>
                        <product>A</product>
                        <region>North</region>
                        <revenue>100</revenue>
                        <quantity>10</quantity>
                    </record>
                    <record>
                        <date>2026-01-02</date>
                        <product>B</product>
                        <region>South</region>
                        <revenue>200</revenue>
                        <quantity>20</quantity>
                    </record>
                </records>
            """.trimIndent()
            val result = validateFile(xmlFile(xml))

            assertIs<ValidationResult.Valid>(result)
            assertEquals(2, result.rowCount)
        }

        @Test
        fun `empty content fails`() {
            val result = validateFile(xmlFile(""))

            assertIs<ValidationResult.Invalid>(result)
            assertEquals("File is empty", result.errors.first())
        }

        @Test
        fun `no record elements fails`() {
            val result = validateFile(xmlFile("<root><item>data</item></root>"))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("No <record> elements"))
        }

        @Test
        fun `missing fields reports record number`() {
            val xml = """
                <records>
                    <record>
                        <date>2026-01-01</date>
                        <product>A</product>
                        <region>North</region>
                        <revenue>100</revenue>
                        <quantity>10</quantity>
                    </record>
                    <record>
                        <date>2026-01-02</date>
                        <product>B</product>
                    </record>
                </records>
            """.trimIndent()
            val result = validateFile(xmlFile(xml))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("Record 2"))
            assertTrue(result.errors.first().contains("missing fields"))
        }

        @Test
        fun `invalid xml fails gracefully`() {
            val result = validateFile(xmlFile("<broken><unclosed>"))

            assertIs<ValidationResult.Invalid>(result)
            assertTrue(result.errors.first().contains("Invalid XML"))
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 3, 5])
        fun `row count matches record elements`(numRecords: Int) {
            val records = (1..numRecords).joinToString("\n") {
                """
                <record>
                    <date>2026-01-01</date>
                    <product>A</product>
                    <region>North</region>
                    <revenue>100</revenue>
                    <quantity>10</quantity>
                </record>
                """.trimIndent()
            }
            val result = validateFile(xmlFile("<records>$records</records>"))

            assertIs<ValidationResult.Valid>(result)
            assertEquals(numRecords, result.rowCount)
        }
    }
}