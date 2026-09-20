package com.winecellar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WineDao {

    @Query("SELECT * FROM wines ORDER BY winery COLLATE NOCASE, vintage")
    fun observeAll(): Flow<List<Wine>>

    @Query("SELECT * FROM wines WHERE id = :id")
    fun observeById(id: Long): Flow<Wine?>

    @Query("SELECT * FROM wines ORDER BY winery COLLATE NOCASE, vintage")
    suspend fun getAllOnce(): List<Wine>

    @Query("SELECT * FROM wines WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Wine?

    @Query("SELECT COUNT(*) FROM wines")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(wine: Wine): Long

    @Insert
    suspend fun insertAll(wines: List<Wine>)

    @Query("DELETE FROM wines")
    suspend fun clear()

    /** Replace the entire cellar with [wines] atomically (used by Restore). */
    @Transaction
    suspend fun replaceAll(wines: List<Wine>) {
        clear()
        insertAll(wines)
    }

    @Update
    suspend fun update(wine: Wine)

    @Delete
    suspend fun delete(wine: Wine)
}
