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

        // Classify each wine once and reuse the style for both its drink status
        // and the by-style breakdown (statusFor previously re-classified).
        var ready = 0
        var cellaring = 0
        var pastPeak = 0
        val bottlesByStyle = HashMap<WineStyle, Int>()
        for (w in wines) {
            val style = WineStyle.classify(w.grapeType, w.name)
            when (DrinkWindowCalculator.statusFor(w, style, currentYear)) {
                DrinkStatus.READY -> ready += w.quantity
                DrinkStatus.TOO_YOUNG -> cellaring += w.quantity
                DrinkStatus.PAST_PEAK -> pastPeak += w.quantity
                DrinkStatus.UNKNOWN -> Unit
            }
            bottlesByStyle[style] = (bottlesByStyle[style] ?: 0) + w.quantity
        }

        val byStyle = bottlesByStyle.entries
            .map { (style, bottles) -> style to bottles }
            .sortedWith(compareByDescending<Pair<WineStyle, Int>> { it.second }.thenBy { it.first.ordinal })

        // Group case-insensitively so inconsistent casing from an import merges;
        // display the first-seen spelling.
        val byCountry = wines
            .filter { !it.country.isNullOrBlank() }
            .groupBy { it.country!!.trim().lowercase() }
            .map { (_, list) -> StatBucket(list.first().country!!.trim(), list.sumOf { it.quantity }) }
            .sortedWith(compareByDescending<StatBucket> { it.bottles }.thenBy { it.label.lowercase() })

        val topWineries = wines
            .groupBy { it.winery.trim().lowercase() }
            .map { (_, list) -> StatBucket(list.first().winery.trim(), list.sumOf { it.quantity }) }
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
