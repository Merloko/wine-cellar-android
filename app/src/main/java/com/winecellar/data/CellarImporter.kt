package com.winecellar.data

/**
 * Parses a previously exported cellar file back into [Wine] rows. JSON reuses
 * [SeedLoader]'s schema; CSV reads the header written by [CellarExporter].
 * Imported rows have id 0, so they are inserted as new bottles (import appends;
 * it does not de-duplicate against the existing cellar).
 */
object CellarImporter {

    // minQuantity = 0 so a wine drunk down to zero bottles round-trips as zero,
    // rather than being floored back to 1 the way the bundled seed is.
    fun parseJson(text: String): List<Wine> = SeedLoader.parse(text, minQuantity = 0)

    fun parseCsv(text: String): List<Wine> {
        val rows = parseCsvRows(text)
        if (rows.size < 2) return emptyList()

        val header = rows.first().map { it.trim().lowercase() }
        fun col(vararg names: String): Int? =
            names.firstNotNullOfOrNull { n -> header.indexOf(n).takeIf { it >= 0 } }

        val iWinery = col("winery")
        val iVintage = col("vintage")
        val iName = col("name")
        val iGrape = col("grape type", "grapetype", "grape")
        val iLocation = col("location")
        val iShelf = col("shelf")
        val iRackCol = col("rack column", "rackcolumn")
        val iRackRow = col("rack row", "rackrow")
        val iCountry = col("country")
        val iRegion = col("region")
        val iQty = col("quantity", "bottles")
        val iFrom = col("drink from", "drinkfrom")
        val iTo = col("drink to", "drinkto")
        val iWindowNote = col("drink window note", "drinkwindownote")
        val iFavorite = col("favorite", "favourite")
        val iBarcode = col("barcode")
        val iNotes = col("notes")

        return rows.drop(1)
            .filter { row -> row.any { it.isNotBlank() } }
            .mapNotNull { row ->
                fun cell(i: Int?): String? = i?.let { row.getOrNull(it) }?.trim()?.ifBlank { null }
                val winery = cell(iWinery) ?: return@mapNotNull null // a row with no winery is skipped
                Wine(
                    winery = winery,
                    vintage = cell(iVintage)?.toIntOrNull(),
                    name = cell(iName),
                    grapeType = cell(iGrape),
                    location = cell(iLocation),
                    shelf = cell(iShelf),
                    rackColumn = cell(iRackCol)?.toIntOrNull(),
                    rackRow = cell(iRackRow)?.toIntOrNull(),
                    country = cell(iCountry),
                    region = cell(iRegion),
                    // A present-but-zero count is kept; a missing/blank cell defaults to 1.
                    quantity = cell(iQty)?.toIntOrNull()?.coerceAtLeast(0) ?: 1,
                    drinkFrom = cell(iFrom)?.toIntOrNull(),
                    drinkTo = cell(iTo)?.toIntOrNull(),
                    drinkWindowNote = cell(iWindowNote),
                    favorite = cell(iFavorite).isTruthy(),
                    barcode = cell(iBarcode),
                    notes = cell(iNotes),
                )
            }
    }

    private fun String?.isTruthy(): Boolean =
        this != null && this.lowercase() in setOf("yes", "true", "1", "y")

    /** RFC-4180-ish tokenizer: handles quoted fields, doubled quotes, and quoted newlines. */
    private fun parseCsvRows(text: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' -> inQuotes = true
                c == ',' -> { row.add(field.toString()); field.setLength(0) }
                c == '\r' -> Unit
                c == '\n' -> { row.add(field.toString()); rows.add(row); row = ArrayList(); field.setLength(0) }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }
}
