package com.winecellar.data

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * What a barcode lookup could fill in on the add/edit form. Country is
 * deliberately omitted: Open Food Facts' `countries` is where a product is
 * *sold*, not its origin, so it would write misleading data.
 */
data class WineLookupResult(
    val winery: String? = null,
    val name: String? = null,
) {
    val isEmpty: Boolean get() = winery.isNullOrBlank() && name.isNullOrBlank()
}

/** Outcome of an online barcode lookup, so the UI can tell apart the three cases. */
sealed interface LookupOutcome {
    data class Found(val result: WineLookupResult) : LookupOutcome
    data object NotFound : LookupOutcome
    data object Error : LookupOutcome
}

/**
 * Opt-in, user-triggered barcode → product lookup via the free, keyless
 * Open Food Facts API. This is the ONE place the app touches the network, and
 * only when the user explicitly taps "Look up online". Everything else is offline.
 */
object BarcodeLookup {

    private const val TIMEOUT_MS = 10_000
    private const val USER_AGENT = "WineCellar/1.0 (Android; offline-first wine app)"

    /** Fetch product info for [barcode]. */
    fun fetch(barcode: String): LookupOutcome {
        val code = barcode.trim()
        if (code.isEmpty()) return LookupOutcome.NotFound
        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$code.json" +
                "?fields=product_name,brands",
        )
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                LookupOutcome.Error
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                parse(body)?.let { LookupOutcome.Found(it) } ?: LookupOutcome.NotFound
            }
        } catch (e: Exception) {
            LookupOutcome.Error
        } finally {
            conn.disconnect()
        }
    }

    /** Parse an Open Food Facts product response. Pure, for unit testing. */
    fun parse(json: String): WineLookupResult? {
        val root = JSONObject(json)
        if (root.optInt("status", 0) != 1) return null
        val product = root.optJSONObject("product") ?: return null

        fun field(key: String): String? =
            product.optString(key, "").trim().ifBlank { null }

        // "brands" is a comma-separated list; take the first as the winery.
        val winery = field("brands")?.substringBefore(",")?.trim()?.ifBlank { null }
        val name = field("product_name")

        val result = WineLookupResult(winery = winery, name = name)
        return if (result.isEmpty) null else result
    }
}
