package com.winecellar.data

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Exercises the real v1 → v2 migration end to end: a database is created with
 * the pre-migration ("v1") schema, then opened through Room with
 * [WineDatabase.MIGRATION_1_2]. Room validates the post-migration schema
 * against the current entities on open, so a wrong migration fails here rather
 * than on a user's device. No exported schema JSON is required.
 */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test fun migrate1To2_preservesRows_addsBarcodeColumn_andDrinkLogTable() = runBlocking {
        createV1DatabaseWithOneRow()

        val db = Room.databaseBuilder(context, WineDatabase::class.java, dbName)
            .addMigrations(WineDatabase.MIGRATION_1_2)
            .build()

        // First query triggers open → migration → Room schema validation.
        val wines = db.wineDao().observeAll().first()
        assertEquals(1, wines.size)
        val wine = wines.first()
        assertEquals("Woody Nook", wine.winery)
        assertEquals(2021, wine.vintage)
        assertEquals(3, wine.quantity)
        assertTrue(wine.favorite)
        assertNull(wine.barcode) // column added by the migration, defaults null

        // The new drink_log table exists and is writable.
        db.drinkLogDao().insert(DrinkLog(wineId = wine.id, title = "test", winery = "Woody Nook"))
        assertEquals(1, db.drinkLogDao().observeAll().first().size)

        db.close()
    }

    /** Creates the exact v1 `wines` schema (v2 minus the `barcode` column) and inserts a row. */
    private fun createV1DatabaseWithOneRow() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `wines` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`winery` TEXT NOT NULL, `vintage` INTEGER, `name` TEXT, " +
                            "`grapeType` TEXT, `location` TEXT, `shelf` TEXT, " +
                            "`rackColumn` INTEGER, `rackRow` INTEGER, `country` TEXT, " +
                            "`region` TEXT, `quantity` INTEGER NOT NULL, `drinkFrom` INTEGER, " +
                            "`drinkTo` INTEGER, `drinkWindowNote` TEXT, `notes` TEXT, " +
                            "`favorite` INTEGER NOT NULL, `dateAdded` INTEGER NOT NULL)",
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        helper.writableDatabase.execSQL(
            "INSERT INTO wines (winery, vintage, name, quantity, favorite, dateAdded) " +
                "VALUES ('Woody Nook', 2021, 'G&T', 3, 1, 100)",
        )
        helper.close()
    }
}
