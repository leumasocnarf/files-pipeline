package com.demo.processing.helpers

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Aggregates parsed row data into a summary map for reporting and event publishing.
 * Only [ParsedRow.Valid] rows are included in all calculations.
 *
 * @param parsed a successfully parsed file — only call this on [ParseResult.Success]
 * @return a map of aggregated metrics including revenue, quantity, distributions, and date range
 */
fun aggregateData(parsed: ParseResult.Success): Map<String, Any> {
    val rows = parsed.rows.filterIsInstance<ParsedRow.Valid>()
    val total = rows.size.toDouble()

    val products = rows.groupBy { it.data["product"] ?: "unknown" }
    val regions = rows.groupBy { it.data["region"] ?: "unknown" }
    val dates = rows.mapNotNull {
        it.data["date"]?.let { date -> runCatching { LocalDate.parse(date) }.getOrNull() }
    }

    val result = mutableMapOf(
        "totalRevenue"         to sumColumn(rows, "revenue"),
        "totalQuantity"        to sumColumnInt(rows, "quantity"),
        "uniqueProducts"       to products.size,
        "uniqueRegions"        to regions.size,
        "revenueByProduct"     to revenueBy(products),
        "revenueByRegion"      to revenueBy(regions),
        "productDistribution"  to distributionOf(products, total),
        "regionDistribution"   to distributionOf(regions, total)
    )

    if (dates.isNotEmpty()) {
        result["dateRange"] = mapOf(
            "earliest" to dates.min().toString(),
            "latest"   to dates.max().toString(),
            // +1 because the range is inclusive
            "spanDays" to ChronoUnit.DAYS.between(dates.min(), dates.max()) + 1
        )
    }

    return result
}

/** Returns the percentage of [part] relative to [total], rounded to two decimal places. Returns 0.0 if [total] is zero. */
private fun percentage(part: Int, total: Double): Double =
    if (total == 0.0) 0.0 else (part / total * 10000).roundToInt() / 100.0


/** Sums the numeric values of [column] across all [rows], treating missing or unparseable values as zero. */
private fun sumColumn(rows: List<ParsedRow.Valid>, column: String): BigDecimal =
    rows.fold(BigDecimal.ZERO) { acc, row ->
        acc + (row.data[column]?.toBigDecimalOrNull() ?: BigDecimal.ZERO)
    }.setScale(2, RoundingMode.HALF_UP)


/** Sums the integer values of [column] across all [rows], treating missing or unparseable values as zero. */
private fun sumColumnInt(rows: List<ParsedRow.Valid>, column: String): Int =
    rows.sumOf { it.data[column]?.toIntOrNull() ?: 0 }


/** Returns total revenue grouped by the given [groups] key. */
private fun revenueBy(groups: Map<String, List<ParsedRow.Valid>>): Map<String, BigDecimal> =
    groups.mapValues { (_, rows) -> sumColumn(rows, "revenue") }


/** Returns the percentage distribution of row counts across the given [groups] relative to [total]. */
private fun distributionOf(groups: Map<String, List<ParsedRow.Valid>>, total: Double): Map<String, Double> =
    groups.mapValues { (_, rows) -> percentage(rows.size, total) }