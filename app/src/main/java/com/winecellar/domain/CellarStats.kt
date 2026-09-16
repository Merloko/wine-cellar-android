package com.winecellar.domain

import com.winecellar.data.Wine

/** A count of bottles under some label (style, country, winery…), for breakdowns. */
data class StatBucket(val label: String, val bottles: Int)

/** Aggregate view of the cellar for the stats screen. All counts are in bottles. */
data class CellarStats(
    val totalBottles: Int = 0,
    val distinctWines: Int = 0,
    val readyNow: Int = 0,
    val cellaring: Int = 0,
    val pastPeak: Int = 0,
    val byStyle: List<Pair<WineStyle, Int>> = emptyList(),
    val byCountry: List<StatBucket> = emptyList(),
    val topWineries: List<StatBucket> = emptyList(),
    val oldestVintage: Int? = null,
    val newestVintage: Int? = null,
) {
    val isEmpty: Boolean get() = distinctWines == 0
}

object CellarStatsCalculator {

    private const val TOP_WINERIES = 5

    fun compute(wines: List<Wine>, currentYear: Int): CellarStats {
        if (wines.isEmpty()) return CellarStats()

        var ready = 0
        var cellaring = 0
        var pastPeak = 0
        for (w in wines) {
            when (DrinkWindowCalculator.statusFor(w, currentYear)) {
                DrinkStatus.READY -> ready += w.quantity
                DrinkStatus.TOO_YOUNG -> cellaring += w.quantity
                DrinkStatus.PAST_PEAK -> pastPeak += w.quantity
                DrinkStatus.UNKNOWN -> Unit
            }
        }

        val byStyle = wines
            .groupBy { WineStyle.classify(it.grapeType, it.name) }
            .map { (style, list) -> style to list.sumOf { it.quantity } }
            .sortedWith(compareByDescending<Pair<WineStyle, Int>> { it.second }.thenBy { it.first.ordinal })

        val byCountry = wines
            .filter { !it.country.isNullOrBlank() }
            .groupBy { it.country!!.trim() }
            .map { (country, list) -> StatBucket(country, list.sumOf { it.quantity }) }
            .sortedWith(compareByDescending<StatBucket> { it.bottles }.thenBy { it.label.lowercase() })

        val topWineries = wines
            .groupBy { it.winery }
            .map { (winery, list) -> StatBucket(winery, list.sumOf { it.quantity }) }
            .sortedWith(compareByDescending<StatBucket> { it.bottles }.thenBy { it.label.lowercase() })
            .take(TOP_WINERIES)

        val vintages = wines.mapNotNull { it.vintage }

        return CellarStats(
            totalBottles = wines.sumOf { it.quantity },
            distinctWines = wines.size,
            readyNow = ready,
            cellaring = cellaring,
            pastPeak = pastPeak,
            byStyle = byStyle,
            byCountry = byCountry,
            topWineries = topWineries,
            oldestVintage = vintages.minOrNull(),
            newestVintage = vintages.maxOrNull(),
        )
    }
}
