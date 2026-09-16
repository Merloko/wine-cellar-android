package com.winecellar.domain

import com.winecellar.data.Wine
import org.junit.Assert.assertEquals
import org.junit.Test

class CellarStatsTest {

    private val year = 2026

    private val wines = listOf(
        Wine(id = 1, winery = "McHenry Hohnen", vintage = 2021, grapeType = "Grenache;Syrah;Mataro", country = "Australia", quantity = 3), // medium red (GSM), ready
        Wine(id = 2, winery = "Woody Nook", vintage = 2024, grapeType = "Sauvignon Blanc", country = "Australia", quantity = 2), // white, ready
        Wine(id = 3, winery = "G.H. Mumm", vintage = null, grapeType = "Champagne", country = "France", quantity = 1), // sparkling NV, ready
        Wine(id = 4, winery = "Old Vasse", vintage = 2005, grapeType = "Rose", country = "Australia", quantity = 4), // rose, past peak
        Wine(id = 5, winery = "Young Cab", vintage = 2025, grapeType = "Cabernet Sauvignon", country = "Australia", quantity = 2), // bold red, cellaring
    )

    @Test fun empty_cellar_is_empty() {
        val stats = CellarStatsCalculator.compute(emptyList(), year)
        assertEquals(true, stats.isEmpty)
        assertEquals(0, stats.totalBottles)
    }

    @Test fun totals_and_distinct() {
        val stats = CellarStatsCalculator.compute(wines, year)
        assertEquals(12, stats.totalBottles)
        assertEquals(5, stats.distinctWines)
        assertEquals(false, stats.isEmpty)
    }

    @Test fun drink_status_counts_are_by_bottle() {
        val stats = CellarStatsCalculator.compute(wines, year)
        assertEquals(6, stats.readyNow)   // 3 + 2 + 1
        assertEquals(2, stats.cellaring)  // young cab
        assertEquals(4, stats.pastPeak)   // old rosé
    }

    @Test fun by_style_is_bottles_desc() {
        val stats = CellarStatsCalculator.compute(wines, year)
        // Rosé leads with 4 bottles; the GSM (3) is now medium, the Cabernet (2) bold.
        assertEquals(WineStyle.ROSE to 4, stats.byStyle.first())
    }

    @Test fun by_country_aggregates_bottles() {
        val stats = CellarStatsCalculator.compute(wines, year)
        assertEquals(StatBucket("Australia", 11), stats.byCountry.first())
        assertEquals(StatBucket("France", 1), stats.byCountry.last())
    }

    @Test fun top_wineries_sorted_by_bottles_then_name() {
        val stats = CellarStatsCalculator.compute(wines, year)
        assertEquals(StatBucket("Old Vasse", 4), stats.topWineries.first())
        // Tie at 2 bottles resolves alphabetically: Woody Nook before Young Cab.
        assertEquals(listOf("Old Vasse", "McHenry Hohnen", "Woody Nook", "Young Cab", "G.H. Mumm"), stats.topWineries.map { it.label })
    }

    @Test fun vintage_range() {
        val stats = CellarStatsCalculator.compute(wines, year)
        assertEquals(2005, stats.oldestVintage)
        assertEquals(2025, stats.newestVintage)
    }
}
