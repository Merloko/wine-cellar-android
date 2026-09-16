package com.winecellar.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** Thin façade over the DAOs so the UI never touches Room types directly. */
class WineRepository(
    private val dao: WineDao,
    private val drinkLogDao: DrinkLogDao,
) {

    val wines: Flow<List<Wine>> = dao.observeAll()
    val drinkLog: Flow<List<DrinkLog>> = drinkLogDao.observeAll()

    fun wine(id: Long): Flow<Wine?> = dao.observeById(id)

    suspend fun save(wine: Wine): Long = dao.upsert(wine)

    suspend fun delete(wine: Wine) = dao.delete(wine)

    suspend fun allWinesOnce(): List<Wine> = dao.getAllOnce()

    /** Insert imported wines as new rows. Returns how many were added. */
    suspend fun importWines(wines: List<Wine>): Int {
        if (wines.isEmpty()) return 0
        dao.insertAll(wines)
        return wines.size
    }

    suspend fun findByBarcode(barcode: String): Wine? = dao.findByBarcode(barcode)

    /**
     * Record that one bottle of [wine] was drunk: add a history entry and
     * decrement the remaining quantity (never below zero).
     */
    suspend fun drinkBottle(wine: Wine, rating: Int?, note: String?) {
        drinkLogDao.insert(
            DrinkLog(
                wineId = wine.id.takeIf { it != 0L },
                title = wine.displayTitle,
                winery = wine.winery,
                vintage = wine.vintage,
                grapeType = wine.grapeType,
                rating = rating,
                note = note?.trim()?.ifBlank { null },
            ),
        )
        dao.update(wine.copy(quantity = (wine.quantity - 1).coerceAtLeast(0)))
    }

    suspend fun deleteLog(entry: DrinkLog) = drinkLogDao.delete(entry)

    suspend fun seedIfEmpty(context: Context) = SeedLoader.seedIfEmpty(context, dao)

    companion object {
        fun from(context: Context): WineRepository {
            val db = WineDatabase.get(context)
            return WineRepository(db.wineDao(), db.drinkLogDao())
        }
    }
}
