package com.winecellar.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DrinkHistoryTest {

    private lateinit var db: WineDatabase
    private lateinit var repo: WineRepository

    @Before fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, WineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = WineRepository(db.wineDao(), db.drinkLogDao())
    }

    @After fun tearDown() = db.close()

    @Test fun drinking_a_bottle_logs_it_and_decrements_quantity() = runBlocking {
        val id = repo.save(Wine(winery = "Woody Nook", vintage = 2021, name = "G&T", quantity = 2))
        val wine = repo.wine(id).first()!!

        repo.drinkBottle(wine, rating = 5, note = "delicious")

        val updated = repo.wine(id).first()!!
        assertEquals(1, updated.quantity)

        val log = repo.drinkLog.first()
        assertEquals(1, log.size)
        assertEquals("Woody Nook 2021 G&T", log.first().title)
        assertEquals(5, log.first().rating)
        assertEquals(id, log.first().wineId)
    }

    @Test fun quantity_never_goes_below_zero() = runBlocking {
        val id = repo.save(Wine(winery = "Test", quantity = 1))
        val wine = repo.wine(id).first()!!
        repo.drinkBottle(wine, null, null)
        // Drinking again from the stale (already-zeroed) copy must not underflow.
        repo.drinkBottle(repo.wine(id).first()!!, null, null)
        assertEquals(0, repo.wine(id).first()!!.quantity)
    }

    @Test fun blank_note_is_stored_as_null() = runBlocking {
        val id = repo.save(Wine(winery = "Test", quantity = 1))
        repo.drinkBottle(repo.wine(id).first()!!, null, "   ")
        assertNull(repo.drinkLog.first().first().note)
    }
}
