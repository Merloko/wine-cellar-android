package com.winecellar.domain

import com.winecellar.data.Wine

enum class SortOrder {
    WINERY,
    VINTAGE_NEWEST,
    VINTAGE_OLDEST,
    RECENTLY_ADDED,
    DRINK_URGENCY,
}

/** The current search / filter / sort selection for the cellar list. */
data class FilterState(
    val query: String = "",
    val location: String? = null,
    val style: WineStyle? = null,
    val winery: String? = null,
    val sort: SortOrder = SortOrder.WINERY,
) {
    val hasActiveFilters: Boolean
        get() = location != null || style != null || winery != null
}

object WineFilters {

    /** Apply [state] to [wines] and return the filtered, sorted result. */
    fun apply(wines: List<Wine>, state: FilterState, currentYear: Int): List<Wine> {
        val terms = state.query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }

        val filtered = wines.filter { wine ->
            (state.location == null || wine.location.equals(state.location, ignoreCase = true)) &&
                (state.winery == null || wine.winery.equals(state.winery, ignoreCase = true)) &&
                (state.style == null || WineStyle.classify(wine.grapeType, wine.name) == state.style) &&
                terms.all { term -> wine.matches(term) }
        }

        return filtered.sortedWith(comparatorFor(state.sort, currentYear))
    }

    private fun Wine.matches(term: String): Boolean {
        val haystack = buildString {
            append(winery); append(' ')
            append(name.orEmpty()); append(' ')
            append(grapeType.orEmpty()); append(' ')
            append(region.orEmpty()); append(' ')
            append(country.orEmpty()); append(' ')
            append(location.orEmpty()); append(' ')
            append(shelf.orEmpty()); append(' ')
            append(vintage?.toString().orEmpty())
        }.lowercase()
        return term in haystack
    }

    private fun comparatorFor(sort: SortOrder, currentYear: Int): Comparator<Wine> = when (sort) {
        SortOrder.WINERY ->
            compareBy(String.CASE_INSENSITIVE_ORDER, Wine::winery)
                .thenByDescending { it.vintage ?: Int.MIN_VALUE }
        SortOrder.VINTAGE_NEWEST ->
            compareByDescending<Wine> { it.vintage ?: Int.MIN_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER, Wine::winery)
        SortOrder.VINTAGE_OLDEST ->
            compareBy<Wine> { it.vintage ?: Int.MAX_VALUE }
                .thenBy(String.CASE_INSENSITIVE_ORDER, Wine::winery)
        SortOrder.RECENTLY_ADDED ->
            compareByDescending { it.dateAdded }
        SortOrder.DRINK_URGENCY ->
            compareBy<Wine> { urgencyRank(it, currentYear) }
                .thenBy { it.vintage ?: Int.MAX_VALUE }
    }

    /** Lower rank = more urgent to drink. Past-peak first, then ready, then cellaring. */
    private fun urgencyRank(wine: Wine, currentYear: Int): Int =
        when (DrinkWindowCalculator.statusFor(wine, currentYear)) {
            DrinkStatus.PAST_PEAK -> 0
            DrinkStatus.READY -> 1
            DrinkStatus.UNKNOWN -> 2
            DrinkStatus.TOO_YOUNG -> 3
        }
}
