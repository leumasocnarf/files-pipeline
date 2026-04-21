package com.demo.processing.helpers

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import kotlin.test.assertIs

class DataAggregationTest {

    private fun parseAndAggregate(vararg rows: String): Map<String, Any> {
        val headers = "date,product,region,revenue,quantity"
        val csv = (listOf(headers) + rows.toList()).joinToString("\n")
        val result = parseFile(csv.toByteArray(), "data.csv")
        return aggregateData(assertIs(result))
    }

    @Nested
    inner class Totals {

        @ParameterizedTest
        @CsvSource(
            "100.0, 200.0, 300.00",
            "0.0, 0.0, 0.00",
            "999.99, 0.01, 1000.00"
        )
        fun `computes total revenue`(rev1: String, rev2: String, expected: String) {
            val result = parseAndAggregate(
                "2026-01-01,A,North,$rev1,10",
                "2026-01-02,B,South,$rev2,20"
            )
            assertEquals(BigDecimal(expected), result["totalRevenue"])
        }

        @ParameterizedTest
        @CsvSource("10, 20, 30", "0, 0, 0", "1, 999, 1000")
        fun `computes total quantity`(qty1: String, qty2: String, expected: Int) {
            val result = parseAndAggregate(
                "2026-01-01,A,North,100.0,$qty1",
                "2026-01-02,B,South,200.0,$qty2"
            )
            assertEquals(expected, result["totalQuantity"])
        }
    }

    @Nested
    inner class RevenueBreakdown {

        @Test
        fun `groups revenue by product`() {
            val result = parseAndAggregate(
                "2026-01-01,A,North,100.0,10",
                "2026-01-02,A,South,200.0,20",
                "2026-01-03,B,North,50.0,5"
            )
            @Suppress("UNCHECKED_CAST")
            val byProduct = result["revenueByProduct"] as Map<String, BigDecimal>

            assertEquals(BigDecimal("300.00"), byProduct["A"])
            assertEquals(BigDecimal("50.00"), byProduct["B"])
        }

        @Test
        fun `groups revenue by region`() {
            val result = parseAndAggregate(
                "2026-01-01,A,North,100.0,10",
                "2026-01-02,B,North,200.0,20",
                "2026-01-03,C,South,50.0,5"
            )
            @Suppress("UNCHECKED_CAST")
            val byRegion = result["revenueByRegion"] as Map<String, BigDecimal>

            assertEquals(BigDecimal("300.00"), byRegion["North"])
            assertEquals(BigDecimal("50.00"), byRegion["South"])
        }
    }

    @Nested
    inner class Distribution {

        @ParameterizedTest
        @CsvSource(
            "2, 1, 66.67, 33.33",
            "1, 1, 50.0, 50.0",
            "3, 1, 75.0, 25.0"
        )
        fun `product distribution percentages`(
            countA: Int, countB: Int,
            expectedA: Double, expectedB: Double
        ) {
            val rows = (1..countA).map { "2026-01-01,A,North,100.0,10" } +
                    (1..countB).map { "2026-01-01,B,South,100.0,10" }
            val result = parseAndAggregate(*rows.toTypedArray())
            val dist = result["productDistribution"] as Map<*, *>

            assertEquals(expectedA, dist["A"])
            assertEquals(expectedB, dist["B"])
        }

        @ParameterizedTest
        @CsvSource(
            "2, 2, 50.0, 50.0",
            "3, 1, 75.0, 25.0",
            "1, 3, 25.0, 75.0"
        )
        fun `region distribution percentages`(
            countNorth: Int, countSouth: Int,
            expectedNorth: Double, expectedSouth: Double
        ) {
            val rows = (1..countNorth).map { "2026-01-01,A,North,100.0,10" } +
                    (1..countSouth).map { "2026-01-01,B,South,100.0,10" }
            val result = parseAndAggregate(*rows.toTypedArray())
            val dist = result["regionDistribution"] as Map<*, *>

            assertEquals(expectedNorth, dist["North"])
            assertEquals(expectedSouth, dist["South"])
        }
    }

    @Nested
    inner class DateRange {

        @ParameterizedTest
        @CsvSource(
            "2026-01-01, 2026-03-15, 74",
            "2026-01-01, 2026-01-01, 1",
            "2026-01-01, 2026-12-31, 365",
            "2026-06-01, 2026-06-30, 30"
        )
        fun `computes date range and span`(date1: String, date2: String, expectedSpan: Long) {
            val result = parseAndAggregate(
                "$date1,A,North,100.0,10",
                "$date2,B,South,200.0,20"
            )
            val dateRange = result["dateRange"] as Map<*, *>

            assertEquals(date1, dateRange["earliest"])
            assertEquals(date2, dateRange["latest"])
            assertEquals(expectedSpan, dateRange["spanDays"])
        }

        @Test
        fun `no date range with invalid dates`() {
            val result = parseAndAggregate("invalid-date,A,North,100.0,10")

            assertFalse(result.containsKey("dateRange"))
        }
    }

    @Nested
    inner class InvalidData {

        @ParameterizedTest
        @ValueSource(strings = ["not-a-number", "abc", "", "null"])
        fun `non numeric revenue defaults to zero`(badRevenue: String) {
            val result = parseAndAggregate(
                "2026-01-01,A,North,$badRevenue,10",
                "2026-01-02,B,South,200.0,20"
            )
            assertEquals(BigDecimal("200.00"), result["totalRevenue"])
        }
    }
}