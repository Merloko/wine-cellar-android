package com.winecellar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Wine::class, DrinkLog::class], version = 2, exportSchema = true)
abstract class WineDatabase : RoomDatabase() {

    abstract fun wineDao(): WineDao
    abstract fun drinkLogDao(): DrinkLogDao

    companion object {
        @Volatile
        private var INSTANCE: WineDatabase? = null

        /** v1 → v2: add the barcode column and the drink history table. */
        internal val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wines ADD COLUMN barcode TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS drink_log (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        wineId INTEGER,
                        title TEXT NOT NULL,
                        winery TEXT NOT NULL,
                        vintage INTEGER,
                        grapeType TEXT,
                        rating INTEGER,
                        note TEXT,
                        drunkAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): WineDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WineDatabase::class.java,
                    "wine_cellar.db",
                ).addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
            }
    }
}
