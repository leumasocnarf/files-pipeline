package com.demo.processing.helpers

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

fun aggregateData(parsed: ParseResult): Map<String, Any> {
    val rows = parsed.rows.filter { it.valid }

    val products = rows.groupBy { it.data["product"] ?: "unknown" }
    val regions = rows.groupBy { it.data["region"] ?: "unknown" }
    val dates = rows.mapNotNull {
        it.data["date"]?.let { date ->
            runCatching { LocalDate.parse(date) }.getOrNull()
        }
    }
    val total = rows.size.toDouble()

    val result = mutableMapOf(
        "totalRevenue" to sumColumn(rows, "revenue"),
        "totalQuantity" to sumColumn(rows, "quantity").toInt(),
        "uniqueProducts" to products.size,
        "uniqueRegions" to regions.size,
        "revenueByProduct" to revenueBy(products),
        "revenueByRegion" to revenueBy(regions),
        "productDistribution" to distributionOf(products, total),
        "regionDistribution" to distributionOf(regions, total)
    )

    if (dates.isNotEmpty()) {
        val earliest = dates.minOrNull()!!
        val latest = dates.maxOrNull()!!

        result["dateRange"] = mapOf(
            "earliest" to earliest.toString(),
            "latest" to latest.toString(),
            // +1 because the range is inclusive
            "spanDays" to ChronoUnit.DAYS.between(earliest, latest) + 1
        )
    }

    return result
}

private fun percentage(part: Int, total: Double) =
    if (total == 0.0) 0.0 else (part / total * 10000).roundToInt() / 100.0

private fun sumColumn(rows: List<CsvRow>, column: String): Double =
    rows.sumOf { it.data[column]?.toDoubleOrNull() ?: 0.0 }

private fun revenueBy(groups: Map<String, List<CsvRow>>): Map<String, Double> {
    val result = mutableMapOf<String, Double>()
    for (entry in groups) {
        result[entry.key] = sumColumn(entry.value, "revenue")
    }

    return result
}

private fun distributionOf(groups: Map<String, List<CsvRow>>, total: Double): Map<String, Double> {
    val result = mutableMapOf<String, Double>()

    for (entry in groups) {
        result[entry.key] = percentage(entry.value.size, total)
    }

    return result
}