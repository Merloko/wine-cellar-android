package com.winecellar.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkLogDao {

    @Query("SELECT * FROM drink_log ORDER BY drunkAt DESC")
    fun observeAll(): Flow<List<DrinkLog>>

    @Insert
    suspend fun insert(entry: DrinkLog): Long

    @Delete
    suspend fun delete(entry: DrinkLog)
}
