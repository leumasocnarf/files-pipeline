package com.demo.ingest.helpers

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.mock.web.MockMultipartFile

class FileValidationTest {

    private fun csvFile(content: String, filename: String = "test.csv") =
        MockMultipartFile("file", filename, "text/csv", content.toByteArray())

    private val validHeaders = "date,product,region,revenue,quantity"
    private val validRow = "2026-01-01,Widget,North,100.0,10"

    @Nested
    inner class FileTypeValidation {

        @Test
        fun `empty file fails`() {
            val file = MockMultipartFile("file", "empty.csv", "text/csv", ByteArray(0))
            val result = validateFile(file)

            assertFalse(result.valid)
            assertEquals("File is empty", result.errors.first())
        }

        @ParameterizedTest
        @ValueSource(strings = ["data.json", "report.xml", "image.png", "file.txt", "noextension"])
        fun `unsupported file types fail`(filename: String) {
            val file = MockMultipartFile("file", filename, "text/plain", "content".toByteArray())
            val result = validateFile(file)

            assertFalse(result.valid)
            assertTrue(result.errors.first().contains("Unsupported file type"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["test.csv", "test.CSV", "test.Csv", "DATA.CSV"])
        fun `csv extension is case insensitive`(filename: String) {
            val file = csvFile("$validHeaders\n$validRow", filename)
            val result = validateFile(file)

            assertTrue(result.valid)
        }
    }

    @Nested
    inner class HeaderValidation {

        @Test
        fun `valid headers pass`() {
            val file = csvFile("$validHeaders\n$validRow")
            val result = validateFile(file)

            assertTrue(result.valid)
            assertEquals(5, result.headers.size)
        }

        @Test
        fun `missing headers are reported`() {
            val file = csvFile("date,product\n2026-01-01,Widget")
            val result = validateFile(file)

            assertFalse(result.valid)
            assertTrue(result.errors.first().contains("Missing headers"))
        }

        @ParameterizedTest
        @ValueSource(
            strings = [
                "Date,PRODUCT,Region,Revenue,QUANTITY",
                "DATE,PRODUCT,REGION,REVENUE,QUANTITY",
                "date,product,region,revenue,quantity"
            ]
        )
        fun `headers are case insensitive`(headerLine: String) {
            val file = csvFile("$headerLine\n$validRow")
            val result = validateFile(file)

            assertTrue(result.valid)
        }

        @Test
        fun `headers with whitespace are trimmed`() {
            val file = csvFile(" date , product , region , revenue , quantity \n$validRow")
            val result = validateFile(file)

            assertTrue(result.valid)
        }
    }

    @Nested
    inner class ContentValidation {

        @Test
        fun `empty content fails`() {
            val file = csvFile("")
            val result = validateFile(file)

            assertFalse(result.valid)
            assertEquals("File is empty", result.errors.first())
        }

        @Test
        fun `headers only fails`() {
            val file = csvFile(validHeaders)
            val result = validateFile(file)

            assertFalse(result.valid)
            assertEquals("No data rows", result.errors.first())
        }

        @Test
        fun `wrong column count reports specific row`() {
            val csv = "$validHeaders\n$validRow\n2026-01-02,Widget"
            val file = csvFile(csv)
            val result = validateFile(file)

            assertFalse(result.valid)
            assertEquals(1, result.errors.size)
            assertTrue(result.errors.first().contains("Row 3"))
        }

        @Test
        fun `multiple invalid rows report all errors`() {
            val csv = "$validHeaders\n2026-01-01,Widget\n2026-01-02,Widget,North"
            val file = csvFile(csv)
            val result = validateFile(file)

            assertFalse(result.valid)
            assertEquals(2, result.errors.size)
        }

        @ParameterizedTest
        @ValueSource(ints = [1, 3, 5, 10])
        fun `row count matches actual data rows`(numRows: Int) {
            val rows = (1..numRows).joinToString("\n") { "2026-01-01,A,North,100.0,$it" }
            val file = csvFile("$validHeaders\n$rows")
            val result = validateFile(file)

            assertTrue(result.valid)
            assertEquals(numRows, result.rowCount)
        }
    }
}