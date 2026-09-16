package com.winecellar.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Loads the bundled starter cellar (exported from the owner's spreadsheet) into
 * the database the first time the app runs. Everything is parsed on-device from
 * an asset file — no network access is involved.
 */
object SeedLoader {

    private const val ASSET = "wines_seed.json"

    /** Populate the database from the bundled asset, but only if it is empty. */
    suspend fun seedIfEmpty(context: Context, dao: WineDao) {
        if (dao.count() > 0) return
        val json = context.assets.open(ASSET).bufferedReader().use { it.readText() }
        dao.insertAll(parse(json))
    }

    /** Parse the seed JSON array into [Wine] rows. Kept pure for unit testing. */
    fun parse(json: String): List<Wine> {
        val arr = JSONArray(json)
        val out = ArrayList<Wine>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            // A row without a winery is skipped (matches CSV import).
            val winery = o.stringOrNull("winery") ?: continue
            out += Wine(
                winery = winery,
                vintage = o.intOrNull("vintage"),
                name = o.stringOrNull("name"),
                grapeType = o.stringOrNull("grapeType"),
                location = o.stringOrNull("location"),
                shelf = o.stringOrNull("shelf"),
                rackColumn = o.intOrNull("rackColumn"),
                rackRow = o.intOrNull("rackRow"),
                country = o.stringOrNull("country"),
                region = o.stringOrNull("region"),
                quantity = (o.intOrNull("quantity") ?: 1).coerceAtLeast(1),
                drinkFrom = o.intOrNull("drinkFrom"),
                drinkTo = o.intOrNull("drinkTo"),
                drinkWindowNote = o.stringOrNull("drinkWindowNote"),
                notes = o.stringOrNull("notes"),
                favorite = o.optBoolean("favorite", false),
                barcode = o.stringOrNull("barcode"),
            )
        }
        return out
    }

    private fun JSONObject.stringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key, "").trim().ifBlank { null }

    private fun JSONObject.intOrNull(key: String): Int? =
        if (has(key) && !isNull(key)) optInt(key) else null
}
