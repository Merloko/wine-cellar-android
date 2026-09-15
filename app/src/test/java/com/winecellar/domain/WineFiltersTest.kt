package com.winecellar.domain

import com.winecellar.data.Wine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WineFiltersTest {

    private val year = 2026

    private val cellar = listOf(
        Wine(id = 1, winery = "McHenry Hohnen", vintage = 2021, grapeType = "Grenache;Syrah;Mataro", location = "Cellar", region = "WA"),
        Wine(id = 2, winery = "Woody Nook", vintage = 2024, grapeType = "Tempranillo", location = "Fridge", region = "WA"),
        Wine(id = 3, winery = "Villa Maria", vintage = 2023, grapeType = "Sauvignon Blanc", location = "Fridge", region = "Marlborough"),
        Wine(id = 4, winery = "G.H. Mumm", vintage = null, grapeType = "Champagne", location = "Fridge", country = "France"),
    )

    @Test fun search_matches_winery() {
        val r = WineFilters.apply(cellar, FilterState(query = "woody"), year)
        assertEquals(listOf(2L), r.map { it.id })
    }

    @Test fun search_matches_grape_and_year() {
        assertEquals(listOf(3L), WineFilters.apply(cellar, FilterState(query = "sauvignon"), year).map { it.id })
        assertEquals(listOf(2L), WineFilters.apply(cellar, FilterState(query = "2024"), year).map { it.id })
    }

    @Test fun search_matches_region() {
        assertEquals(listOf(3L), WineFilters.apply(cellar, FilterState(query = "marlborough"), year).map { it.id })
    }

    @Test fun multi_term_search_is_and() {
        assertEquals(listOf(1L), WineFilters.apply(cellar, FilterState(query = "mchenry syrah"), year).map { it.id })
        assertTrue(WineFilters.apply(cellar, FilterState(query = "woody champagne"), year).isEmpty())
    }

    @Test fun filter_by_location() {
        val r = WineFilters.apply(cellar, FilterState(location = "Fridge"), year)
        assertEquals(setOf(2L, 3L, 4L), r.map { it.id }.toSet())
    }

    @Test fun filter_by_style() {
        val r = WineFilters.apply(cellar, FilterState(style = WineStyle.SPARKLING), year)
        assertEquals(listOf(4L), r.map { it.id })
    }

    @Test fun sort_by_vintage_newest_puts_nv_last() {
        val r = WineFilters.apply(cellar, FilterState(sort = SortOrder.VINTAGE_NEWEST), year)
        assertEquals(listOf(2L, 3L, 1L, 4L), r.map { it.id })
    }

    @Test fun default_sort_is_winery_alpha() {
        val r = WineFilters.apply(cellar, FilterState(), year)
        assertEquals(listOf("G.H. Mumm", "McHenry Hohnen", "Villa Maria", "Woody Nook"), r.map { it.winery })
    }
}
