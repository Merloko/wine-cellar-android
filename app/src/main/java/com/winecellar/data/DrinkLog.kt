package com.winecellar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A record that a bottle was drunk. Wine details are denormalised (copied in)
 * so the history survives even if the source wine is later edited or removed
 * from the cellar.
 */
@Entity(tableName = "drink_log")
data class DrinkLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wineId: Long?,          // link back to the cellar wine, if it still exists
    val title: String,          // e.g. "Woody Nook 2021 G&T"
    val winery: String,
    val vintage: Int? = null,
    val grapeType: String? = null,
    val rating: Int? = null,    // 1–5 stars, optional
    val note: String? = null,
    val drunkAt: Long = System.currentTimeMillis(),
)
