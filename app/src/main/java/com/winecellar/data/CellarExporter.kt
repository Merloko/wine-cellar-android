package com.winecellar.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Serialises the cellar to CSV or JSON for user-initiated export. Pure string
 * builders (no I/O) so they're easy to unit test; the JSON shape matches
 * [SeedLoader]'s schema so an export can be re-imported later.
 */
object CellarExporter {

    private val CSV_HEADER = listOf(
        "Winery", "Vintage", "Name", "Grape Type", "Location", "Shelf",
        "Rack Column", "Rack Row", "Country", "Region", "Quantity",
        "Drink From", "Drink To", "Drink Window Note", "Favorite", "Barcode", "Notes",
    )

    fun toCsv(wines: List<Wine>): String {
        val sb = StringBuilder()
        sb.append(CSV_HEADER.joinToString(",") { csvCell(it) }).append("\r\n")
        for (w in wines) {
            val row = listOf(
                w.winery,
                w.vintage?.toString().orEmpty(),
                w.name.orEmpty(),
                w.grapeType.orEmpty(),
                w.location.orEmpty(),
                w.shelf.orEmpty(),
                w.rackColumn?.toString().orEmpty(),
                w.rackRow?.toString().orEmpty(),
                w.country.orEmpty(),
                w.region.orEmpty(),
                w.quantity.toString(),
                w.drinkFrom?.toString().orEmpty(),
                w.drinkTo?.toString().orEmpty(),
                w.drinkWindowNote.orEmpty(),
                if (w.favorite) "yes" else "",
                w.barcode.orEmpty(),
                w.notes.orEmpty(),
            )
            sb.append(row.joinToString(",") { csvCell(it) }).append("\r\n")
        }
        return sb.toString()
    }

    fun toJson(wines: List<Wine>): String {
        val arr = JSONArray()
        for (w in wines) {
            val o = JSONObject()
            o.put("winery", w.winery)
            o.put("vintage", w.vintage ?: JSONObject.NULL)
            o.put("name", w.name ?: JSONObject.NULL)
            o.put("grapeType", w.grapeType ?: JSONObject.NULL)
            o.put("location", w.location ?: JSONObject.NULL)
            o.put("shelf", w.shelf ?: JSONObject.NULL)
            o.put("rackColumn", w.rackColumn ?: JSONObject.NULL)
            o.put("rackRow", w.rackRow ?: JSONObject.NULL)
            o.put("country", w.country ?: JSONObject.NULL)
            o.put("region", w.region ?: JSONObject.NULL)
            o.put("quantity", w.quantity)
            o.put("drinkFrom", w.drinkFrom ?: JSONObject.NULL)
            o.put("drinkTo", w.drinkTo ?: JSONObject.NULL)
            o.put("drinkWindowNote", w.drinkWindowNote ?: JSONObject.NULL)
            o.put("favorite", w.favorite)
            o.put("barcode", w.barcode ?: JSONObject.NULL)
            o.put("notes", w.notes ?: JSONObject.NULL)
            arr.put(o)
        }
        return arr.toString(2)
    }

    private fun csvCell(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }
}
