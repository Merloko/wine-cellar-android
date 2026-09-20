package com.winecellar.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Covers WineDao.replaceAll, which backs Restore-from-file (replace cellar). */
@RunWith(RobolectricTestRunner::class)
class WineDaoReplaceTest {

    private lateinit var db: WineDatabase
    private lateinit var dao: WineDao

    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WineDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.wineDao()
    }

    @After fun tearDown() = db.close()

    @Test fun replaceAll_swaps_the_whole_cellar() = runBlocking {
        dao.insertAll(listOf(Wine(winery = "Old One"), Wine(winery = "Old Two")))
        assertEquals(2, dao.count())

        dao.replaceAll(listOf(Wine(winery = "New One", vintage = 2020)))

        val wines = dao.observeAll().first()
        assertEquals(1, wines.size)
        assertEquals("New One", wines.first().winery)
        assertEquals(2020, wines.first().vintage)
    }

    @Test fun replaceAll_with_empty_list_clears_the_cellar() = runBlocking {
        dao.insertAll(listOf(Wine(winery = "Solo")))
        dao.replaceAll(emptyList())
        assertEquals(0, dao.count())
    }
}
