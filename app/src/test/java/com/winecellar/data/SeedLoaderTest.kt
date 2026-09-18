package com.winecellar.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SeedLoaderTest {

    private lateinit var context: Context
    private lateinit var db: WineDatabase
    private lateinit var dao: WineDao

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.wineDao()
    }

    @After fun tearDown() = db.close()

    @Test fun parses_literal_json() {
        val json = """
            [{"winery":"Woody Nook","vintage":2021,"name":"G&T","grapeType":"Grenache;Tempranillo",
              "location":"Fridge","shelf":"On Top","rackColumn":null,"rackRow":null,
              "country":"Australia","region":"WA","quantity":1,"favorite":true,
              "drinkFrom":null,"drinkTo":null,"drinkWindowNote":null,"notes":null}]
        """.trimIndent()
        val wines = SeedLoader.parse(json)
        assertEquals(1, wines.size)
        val w = wines.first()
        assertEquals("Woody Nook", w.winery)
        assertEquals(2021, w.vintage)
        assertTrue(w.favorite)
        assertEquals(null, w.rackColumn)
    }

    @Test fun seed_floors_quantity_to_one_but_import_can_keep_zero() {
        val json = """[{"winery":"Empty Rack","quantity":0}]"""
        // Default (seed) path: a zero/blank count becomes 1 bottle.
        assertEquals(1, SeedLoader.parse(json).first().quantity)
        // Import path passes minQuantity = 0, preserving an emptied wine.
        assertEquals(0, SeedLoader.parse(json, minQuantity = 0).first().quantity)
    }

    @Test fun seeds_bundled_asset_into_database() = runBlocking {
        assertEquals(0, dao.count())
        SeedLoader.seedIfEmpty(context, dao)
        val count = dao.count()
        // The exported spreadsheet held 108 rows.
        assertEquals(108, count)

        // Seeding again must be a no-op (idempotent).
        SeedLoader.seedIfEmpty(context, dao)
        assertEquals(count, dao.count())
    }

    @Test fun known_wine_round_trips_through_dao() = runBlocking {
        val id = dao.upsert(
            Wine(
                winery = "Cos D'Estournel",
                vintage = 2004,
                grapeType = "Bordeaux",
                location = "Cellar",
                shelf = "Bottom",
                rackColumn = 4,
                rackRow = 6,
                drinkTo = 2030,
            ),
        )
        assertTrue(id > 0)
        val stored = dao.observeAll().first().first { it.id == id }
        assertNotNull(stored)
        assertEquals("Cellar · Bottom · Col 4, Row 6", stored.locationSummary)
        assertEquals(2030, stored.drinkTo)
    }
}
