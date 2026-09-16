package com.winecellar.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winecellar.WineCellarApp
import com.winecellar.data.CellarExporter
import com.winecellar.data.CellarImporter
import com.winecellar.data.DrinkLog
import com.winecellar.data.Wine
import com.winecellar.data.WineRepository
import com.winecellar.domain.CellarStats
import com.winecellar.domain.CellarStatsCalculator
import com.winecellar.domain.DrinkStatus
import com.winecellar.domain.DrinkWindowCalculator
import com.winecellar.domain.FilterState
import com.winecellar.domain.SortOrder
import com.winecellar.domain.WineFilters
import com.winecellar.domain.WineStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Year
import java.util.Date
import java.util.Locale

/** Immutable snapshot the cellar screen renders from. */
data class CellarUiState(
    val wines: List<Wine> = emptyList(),
    val filter: FilterState = FilterState(),
    val locations: List<String> = emptyList(),
    val styles: List<WineStyle> = emptyList(),
    val wineries: List<String> = emptyList(),
    val totalBottles: Int = 0,
)

/** Supported cellar export formats. */
enum class ExportFormat(val extension: String, val mimeType: String) {
    CSV("csv", "text/csv"),
    JSON("json", "application/json"),
}

/** Import files larger than this are rejected to avoid OOM on an unexpected pick. */
private const val MAX_IMPORT_BYTES = 10L * 1024 * 1024

/** Buckets for the "what to drink now" screen. */
data class DrinkNowState(
    val pastPeak: List<Wine> = emptyList(),
    val ready: List<Wine> = emptyList(),
    val soon: List<Wine> = emptyList(),
)

class WineViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: WineRepository = (app as WineCellarApp).repository

    /**
     * Read fresh on each flow emission rather than pinned at ViewModel
     * construction, so drink-window status picks up the new year the next time
     * the cellar changes. (A pure midnight rollover with no data change won't
     * refresh on its own — acceptable for this app.)
     */
    private fun currentYearNow(): Int = Year.now().value

    private val filterState = MutableStateFlow(FilterState())

    val uiState: StateFlow<CellarUiState> =
        combine(repository.wines, filterState) { wines, filter ->
            CellarUiState(
                wines = WineFilters.apply(wines, filter, currentYearNow()),
                filter = filter,
                locations = wines.mapNotNull { it.location?.takeIf(String::isNotBlank) }
                    .distinct().sorted(),
                styles = wines.map { WineStyle.classify(it.grapeType, it.name) }
                    .distinct().sortedBy { it.ordinal },
                wineries = wines.map { it.winery }.distinct().sorted(),
                totalBottles = wines.sumOf { it.quantity },
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CellarUiState())

    val drinkNow: StateFlow<DrinkNowState> =
        repository.wines.map { wines ->
            val year = currentYearNow()
            val ready = ArrayList<Wine>()
            val past = ArrayList<Wine>()
            val soon = ArrayList<Wine>()
            for (wine in wines) {
                when (DrinkWindowCalculator.statusFor(wine, year)) {
                    DrinkStatus.PAST_PEAK -> past += wine
                    DrinkStatus.READY -> ready += wine
                    DrinkStatus.TOO_YOUNG -> {
                        val from = DrinkWindowCalculator.windowFor(wine).from
                        if (from != null && from <= year + 1) soon += wine
                    }
                    DrinkStatus.UNKNOWN -> Unit
                }
            }
            val byWinery = compareBy<Wine>({ it.winery.lowercase() }, { it.vintage ?: 0 })
            DrinkNowState(
                pastPeak = past.sortedWith(compareBy({ it.drinkTo ?: it.vintage ?: 0 })),
                ready = ready.sortedWith(byWinery),
                soon = soon.sortedWith(compareBy { DrinkWindowCalculator.windowFor(it).from ?: 0 }),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrinkNowState())

    val history: StateFlow<List<DrinkLog>> =
        repository.drinkLog.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<CellarStats> =
        repository.wines.map { CellarStatsCalculator.compute(it, currentYearNow()) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CellarStats())

    fun wine(id: Long) = repository.wine(id)

    fun currentYear(): Int = currentYearNow()

    // ---- drink history ----------------------------------------------------

    fun drinkBottle(wine: Wine, rating: Int?, note: String?) {
        viewModelScope.launch { repository.drinkBottle(wine, rating, note) }
    }

    fun deleteLog(entry: DrinkLog) {
        viewModelScope.launch { repository.deleteLog(entry) }
    }

    // ---- barcode ----------------------------------------------------------

    fun lookupBarcode(barcode: String, onResult: (Wine?) -> Unit) {
        viewModelScope.launch { onResult(repository.findByBarcode(barcode)) }
    }

    // ---- import -----------------------------------------------------------

    /**
     * Read the file at [uri], detect CSV vs JSON by content, parse, and append
     * the wines. [onResult] runs on the main thread with the number imported,
     * or null if the file couldn't be read/parsed.
     */
    fun importFromUri(uri: Uri, onResult: (count: Int?) -> Unit) {
        viewModelScope.launch {
            val result: Int? = try {
                val raw = withContext(Dispatchers.IO) {
                    val resolver = getApplication<Application>().contentResolver
                    val size = resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
                        ?.use { c -> if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else -1L } ?: -1L
                    if (size > MAX_IMPORT_BYTES) {
                        null
                    } else {
                        resolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    }
                }
                // Strip a leading UTF-8 BOM (Excel/Sheets add one) so it doesn't
                // break format detection or the first CSV header cell.
                val text = raw?.removePrefix("﻿")
                if (text.isNullOrBlank()) {
                    null
                } else {
                    val wines = withContext(Dispatchers.Default) {
                        // Our JSON export is always a top-level array.
                        if (text.trimStart().startsWith("[")) {
                            CellarImporter.parseJson(text)
                        } else {
                            CellarImporter.parseCsv(text)
                        }
                    }
                    repository.importWines(wines)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            onResult(result)
        }
    }

    // ---- export -----------------------------------------------------------

    /**
     * Build the export off the UI, write it on [viewModelScope] (so it survives
     * the cellar screen leaving composition), and hand back a shareable Uri.
     * [onReady] runs on the main thread with a null Uri if the write failed.
     */
    fun requestExport(format: ExportFormat, onReady: (uri: Uri?, mimeType: String) -> Unit) {
        viewModelScope.launch {
            val wines = repository.allWinesOnce()
            val content = withContext(Dispatchers.Default) {
                when (format) {
                    ExportFormat.CSV -> CellarExporter.toCsv(wines)
                    ExportFormat.JSON -> CellarExporter.toJson(wines)
                }
            }
            // Timestamped so rapid re-exports never write the same file concurrently.
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val fileName = "wine_cellar_$stamp.${format.extension}"
            val uri = ExportUtils.writeExport(getApplication(), fileName, content)
            onReady(uri, format.mimeType)
        }
    }

    // ---- filter mutations -------------------------------------------------

    fun setQuery(q: String) { filterState.value = filterState.value.copy(query = q) }
    fun setLocation(loc: String?) { filterState.value = filterState.value.copy(location = loc) }
    fun setStyle(style: WineStyle?) { filterState.value = filterState.value.copy(style = style) }
    fun setWinery(winery: String?) { filterState.value = filterState.value.copy(winery = winery) }
    fun setSort(sort: SortOrder) { filterState.value = filterState.value.copy(sort = sort) }
    fun clearFilters() { filterState.value = FilterState(query = filterState.value.query) }

    // ---- persistence ------------------------------------------------------

    fun save(wine: Wine, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.save(wine)
            onSaved(if (wine.id == 0L) id else wine.id)
        }
    }

    fun delete(wine: Wine) {
        viewModelScope.launch { repository.delete(wine) }
    }
}
