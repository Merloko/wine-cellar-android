package com.winecellar.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.winecellar.WineCellarApp
import com.winecellar.data.BarcodeLookup
import com.winecellar.data.CellarExporter
import com.winecellar.data.CellarImporter
import com.winecellar.data.DrinkLog
import com.winecellar.data.LookupOutcome
import com.winecellar.data.SyncPrefs
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
import kotlinx.coroutines.flow.flowOn
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

/** Imports are read up to this many characters, then rejected, to avoid OOM. */
private const val MAX_IMPORT_CHARS = 10L * 1024 * 1024

/** Buckets for the "what to drink now" screen. */
data class DrinkNowState(
    val pastPeak: List<Wine> = emptyList(),
    val ready: List<Wine> = emptyList(),
    val soon: List<Wine> = emptyList(),
)

/**
 * State of the opt-in online barcode lookup. Held on the ViewModel so an
 * in-flight request and its result survive a configuration change (e.g. a
 * rotation) instead of being lost with the composition that launched it.
 */
sealed interface BarcodeLookupState {
    data object Idle : BarcodeLookupState
    data object InFlight : BarcodeLookupState
    data class Complete(val outcome: LookupOutcome) : BarcodeLookupState
}

/** The linked CSV "sync file", if any, shown in the cellar's overflow menu. */
data class SyncState(
    val linked: Boolean = false,
    val fileName: String? = null,
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
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CellarUiState())

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
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DrinkNowState())

    val history: StateFlow<List<DrinkLog>> =
        repository.drinkLog.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<CellarStats> =
        repository.wines.map { CellarStatsCalculator.compute(it, currentYearNow()) }
            .flowOn(Dispatchers.Default)
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

    private val _barcodeLookup = MutableStateFlow<BarcodeLookupState>(BarcodeLookupState.Idle)
    val barcodeLookup: StateFlow<BarcodeLookupState> = _barcodeLookup

    /**
     * Opt-in online lookup of product info for a barcode (Open Food Facts).
     * Only ever called from an explicit "Look up online" tap. The result is
     * published on [barcodeLookup] rather than a callback, so it survives a
     * rotation mid-request; the screen applies it and then calls [consumeBarcodeLookup].
     */
    fun lookupBarcodeOnline(barcode: String) {
        if (_barcodeLookup.value is BarcodeLookupState.InFlight) return
        _barcodeLookup.value = BarcodeLookupState.InFlight
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) { BarcodeLookup.fetch(barcode) }
            _barcodeLookup.value = BarcodeLookupState.Complete(outcome)
        }
    }

    /** Reset the lookup state once the screen has applied a completed result. */
    fun consumeBarcodeLookup() {
        _barcodeLookup.value = BarcodeLookupState.Idle
    }

    // ---- sync file (linked CSV) -------------------------------------------

    private val syncPrefs = SyncPrefs(app)
    private val _syncState = MutableStateFlow(SyncState())
    val syncState: StateFlow<SyncState> = _syncState

    init { refreshSyncState() }

    private fun refreshSyncState() {
        val uri = syncPrefs.linkedUri
        if (uri == null) {
            _syncState.value = SyncState()
            return
        }
        _syncState.value = SyncState(linked = true, fileName = _syncState.value.fileName)
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) { displayName(Uri.parse(uri)) }
            _syncState.value = SyncState(linked = true, fileName = name)
        }
    }

    private fun displayName(uri: Uri): String? = try {
        getApplication<Application>().contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
    } catch (e: Exception) {
        null
    }

    /** Persist the CSV [uri] the user picked as the sync file (read + write). */
    fun linkSyncFile(uri: Uri) {
        try {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // Some providers don't offer a *persistable* grant; keep the URI and
            // rely on the transient grant for this session.
        }
        syncPrefs.linkedUri = uri.toString()
        refreshSyncState()
    }

    fun unlinkSyncFile() {
        syncPrefs.linkedUri?.let { uriStr ->
            try {
                getApplication<Application>().contentResolver.releasePersistableUriPermission(
                    Uri.parse(uriStr),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (e: SecurityException) {
                // Nothing persisted to release; ignore.
            }
        }
        syncPrefs.linkedUri = null
        refreshSyncState()
    }

    /** Overwrite the linked file with the current cellar as CSV (Back up). */
    fun backupToLinkedFile(onResult: (ok: Boolean) -> Unit) {
        val uriStr = syncPrefs.linkedUri
        if (uriStr == null) { onResult(false); return }
        viewModelScope.launch {
            val ok = try {
                val wines = repository.allWinesOnce()
                val csv = withContext(Dispatchers.Default) { CellarExporter.toCsv(wines) }
                withContext(Dispatchers.IO) {
                    // "wt" = truncate, so a shorter cellar can't leave stale bytes.
                    getApplication<Application>().contentResolver
                        .openOutputStream(Uri.parse(uriStr), "wt")?.use { out ->
                            out.write(csv.toByteArray()); true
                        } ?: false
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                false
            }
            onResult(ok)
        }
    }

    /**
     * Replace the whole cellar with the linked file's contents (Restore).
     * [onResult] gets the new cellar size, or null if the file couldn't be
     * read/parsed — in which case the existing cellar is left untouched.
     */
    fun restoreFromLinkedFile(onResult: (count: Int?) -> Unit) {
        val uriStr = syncPrefs.linkedUri
        if (uriStr == null) { onResult(null); return }
        viewModelScope.launch {
            val result: Int? = try {
                val text = withContext(Dispatchers.IO) { readTextCapped(Uri.parse(uriStr)) }
                val wines = parseCellar(text)
                if (wines == null) null else repository.replaceAllWines(wines)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            onResult(result)
        }
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
                val text = withContext(Dispatchers.IO) { readTextCapped(uri) }
                val wines = parseCellar(text)
                if (wines == null) null else repository.importWines(wines)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            onResult(result)
        }
    }

    /**
     * Read a document's text with a hard character cap so a huge/unexpected pick
     * can't OOM us, regardless of whether the provider reports a size. Returns
     * null if the stream can't be opened or the cap is exceeded. Runs on IO.
     */
    private fun readTextCapped(uri: Uri): String? =
        getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
            val reader = stream.bufferedReader()
            val builder = StringBuilder()
            val buffer = CharArray(8192)
            var total = 0L
            while (true) {
                val n = reader.read(buffer)
                if (n < 0) break
                total += n
                if (total > MAX_IMPORT_CHARS) return null
                builder.append(buffer, 0, n)
            }
            builder.toString()
        }

    /** Parse cellar text (CSV or, if it starts with '[', JSON). Null if blank. */
    private suspend fun parseCellar(raw: String?): List<Wine>? {
        // Strip a leading UTF-8 BOM (Excel/Sheets add one) so it doesn't break
        // format detection or the first CSV header cell.
        val text = raw?.removePrefix("﻿")
        if (text.isNullOrBlank()) return null
        return withContext(Dispatchers.Default) {
            if (text.trimStart().startsWith("[")) CellarImporter.parseJson(text)
            else CellarImporter.parseCsv(text)
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
            // Millisecond-stamped so rapid re-exports never write the same file.
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
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
