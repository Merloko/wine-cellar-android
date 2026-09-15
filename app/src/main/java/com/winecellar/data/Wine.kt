package com.winecellar.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single wine entry in the cellar. One row per physical grouping of bottles;
 * [quantity] records how many bottles of this exact wine sit in this exact spot.
 *
 * Physical location is expressed in progressively finer detail:
 *   [location]  — which appliance / area   (Cellar, Fridge, Downstairs Fridge…)
 *   [shelf]     — which shelf within it     (Top, Second, Bottom…)
 *   [rackColumn]/[rackRow] — grid coordinate on that shelf (column 1‑n, row 1‑n)
 */
@Entity(tableName = "wines")
data class Wine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val winery: String,
    val vintage: Int? = null,
    val name: String? = null,
    val grapeType: String? = null,
    val location: String? = null,
    val shelf: String? = null,
    val rackColumn: Int? = null,
    val rackRow: Int? = null,
    val country: String? = null,
    val region: String? = null,
    val quantity: Int = 1,
    val drinkFrom: Int? = null,
    val drinkTo: Int? = null,
    val drinkWindowNote: String? = null,
    val notes: String? = null,
    val favorite: Boolean = false,
    val barcode: String? = null,
    val dateAdded: Long = System.currentTimeMillis(),
) {
    /** e.g. "Woody Nook 2021 G&T" — the winery/vintage/name headline. */
    val displayTitle: String
        get() = buildString {
            append(winery)
            vintage?.let { append(" "); append(it) }
            if (!name.isNullOrBlank()) { append(" "); append(name) }
        }.trim()

    /** e.g. "Cellar · Top · Col 3, Row 4". Null if we have no location at all. */
    val locationSummary: String?
        get() {
            val parts = mutableListOf<String>()
            location?.takeIf { it.isNotBlank() }?.let { parts += it }
            shelf?.takeIf { it.isNotBlank() }?.let { parts += it }
            if (rackColumn != null || rackRow != null) {
                val coords = buildString {
                    rackColumn?.let { append("Col $it") }
                    if (rackColumn != null && rackRow != null) append(", ")
                    rackRow?.let { append("Row $it") }
                }
                if (coords.isNotBlank()) parts += coords
            }
            return parts.joinToString(" · ").ifBlank { null }
        }

    /** "Australia · WA" style origin line. */
    val originSummary: String?
        get() = listOfNotNull(
            region?.takeIf { it.isNotBlank() },
            country?.takeIf { it.isNotBlank() },
        ).joinToString(", ").ifBlank { null }
}
