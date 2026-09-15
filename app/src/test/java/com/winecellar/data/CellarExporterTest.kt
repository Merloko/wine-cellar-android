package com.winecellar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// JSON export uses org.json, which is provided by the Android runtime, so this
// runs under Robolectric.
@RunWith(RobolectricTestRunner::class)
class CellarExporterTest {

    private val wines = listOf(
        Wine(
            id = 1, winery = "McHenry Hohnen", vintage = 2021, name = "GSM",
            grapeType = "Grenache;Syrah;Mataro", location = "Cellar", shelf = "Top",
            rackColumn = 3, rackRow = 2, country = "Australia", region = "WA",
            quantity = 4, favorite = true, barcode = "9312345678900",
        ),
        Wine(
            id = 2, winery = "Test", name = """Big "Red", Reserve""", quantity = 1,
        ),
    )

    @Test fun csv_has_header_and_row_per_wine() {
        val csv = CellarExporter.toCsv(wines)
        val lines = csv.trim().split("\r\n")
        assertEquals(3, lines.size) // header + 2 rows
        assertTrue(lines[0].startsWith("Winery,Vintage,Name"))
    }

    @Test fun csv_escapes_commas_and_quotes() {
        val csv = CellarExporter.toCsv(wines)
        // The name field contains a comma and quotes → must be quoted and doubled.
        assertTrue(csv.contains(""""Big ""Red"", Reserve""""))
    }

    @Test fun json_round_trips_through_seed_parser() {
        val json = CellarExporter.toJson(wines)
        val parsed = SeedLoader.parse(json)
        assertEquals(2, parsed.size)
        val first = parsed.first()
        assertEquals("McHenry Hohnen", first.winery)
        assertEquals(2021, first.vintage)
        assertEquals("GSM", first.name)
        assertEquals(3, first.rackColumn)
        assertEquals(4, first.quantity)
        assertTrue(first.favorite)
        assertEquals("9312345678900", first.barcode)
        // Nulls survive the round trip.
        assertEquals(null, parsed[1].vintage)
        assertEquals(null, parsed[1].barcode)
    }
}
