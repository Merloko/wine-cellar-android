package com.winecellar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// parseJson reuses SeedLoader (org.json), so this runs under Robolectric.
@RunWith(RobolectricTestRunner::class)
class CellarImporterTest {

    @Test fun csv_round_trips_from_exporter() {
        val wines = listOf(
            Wine(id = 1, winery = "McHenry Hohnen", vintage = 2021, name = "GSM", grapeType = "Grenache;Syrah;Mataro", location = "Cellar", shelf = "Top", rackColumn = 3, rackRow = 2, country = "Australia", region = "WA", quantity = 4, favorite = true, barcode = "9312345678900"),
            Wine(id = 2, winery = "Test", name = """Big "Red", Reserve""", quantity = 1),
        )
        val csv = CellarExporter.toCsv(wines)
        val parsed = CellarImporter.parseCsv(csv)

        assertEquals(2, parsed.size)
        val first = parsed.first()
        assertEquals("McHenry Hohnen", first.winery)
        assertEquals(2021, first.vintage)
        assertEquals("GSM", first.name)
        assertEquals(3, first.rackColumn)
        assertEquals(4, first.quantity)
        assertTrue(first.favorite)
        assertEquals("9312345678900", first.barcode)
        // Quoted field with comma survives the round trip.
        assertEquals("""Big "Red", Reserve""", parsed[1].name)
        assertEquals(0L, parsed[1].id) // imported rows are new
    }

    @Test fun csv_skips_rows_without_a_winery() {
        val csv = "Winery,Vintage\n,2020\nWoody Nook,2021\n"
        val parsed = CellarImporter.parseCsv(csv)
        assertEquals(1, parsed.size)
        assertEquals("Woody Nook", parsed.first().winery)
    }

    @Test fun csv_handles_missing_optional_columns() {
        val csv = "Winery,Vintage\nWoody Nook,2021\n"
        val parsed = CellarImporter.parseCsv(csv)
        assertEquals(1, parsed.size)
        assertEquals(1, parsed.first().quantity) // defaulted
        assertNull(parsed.first().grapeType)
    }

    @Test fun zero_bottle_wine_round_trips_as_zero() {
        // A wine drunk down to its last bottle must not spring back to 1 on
        // re-import (unlike the bundled seed, which floors an empty count to 1).
        val wines = listOf(Wine(id = 1, winery = "Empty Rack", vintage = 2018, quantity = 0))

        val fromCsv = CellarImporter.parseCsv(CellarExporter.toCsv(wines))
        assertEquals(0, fromCsv.first().quantity)

        val fromJson = CellarImporter.parseJson(CellarExporter.toJson(wines))
        assertEquals(0, fromJson.first().quantity)
    }

    @Test fun json_round_trips_from_exporter() {
        val wines = listOf(
            Wine(id = 1, winery = "Woody Nook", vintage = 2021, name = "G&T", grapeType = "Grenache;Tempranillo", quantity = 2, favorite = true),
        )
        val json = CellarExporter.toJson(wines)
        val parsed = CellarImporter.parseJson(json)
        assertEquals(1, parsed.size)
        assertEquals("Woody Nook", parsed.first().winery)
        assertEquals(2021, parsed.first().vintage)
        assertEquals(2, parsed.first().quantity)
        assertTrue(parsed.first().favorite)
    }
}
