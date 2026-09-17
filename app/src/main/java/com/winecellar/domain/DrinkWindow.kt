package com.winecellar.domain

import com.winecellar.data.Wine

/** Where a wine sits relative to its ideal drinking window, right now. */
enum class DrinkStatus {
    TOO_YOUNG,   // still cellaring — lay it down
    READY,       // in its window — drink now
    PAST_PEAK,   // window has closed — drink up before it fades
    UNKNOWN,     // not enough info to judge
}

/** A resolved drinking window plus whether it was recorded or estimated. */
data class DrinkWindow(
    val from: Int?,
    val to: Int?,
    val estimated: Boolean,
) {
    fun status(currentYear: Int): DrinkStatus = when {
        from == null && to == null -> DrinkStatus.UNKNOWN
        to != null && currentYear > to -> DrinkStatus.PAST_PEAK
        from != null && currentYear < from -> DrinkStatus.TOO_YOUNG
        else -> DrinkStatus.READY
    }

    /** e.g. "2027–2031", "by 2030", "from 2026". */
    fun label(): String? = when {
        from != null && to != null -> "$from–$to"
        to != null -> "by $to"
        from != null -> "from $from"
        else -> null
    }
}

object DrinkWindowCalculator {

    /**
     * Resolve the drinking window for a wine.
     *  1. If the owner recorded [Wine.drinkFrom]/[Wine.drinkTo], trust that.
     *  2. Otherwise estimate from the wine's [WineStyle] and vintage.
     *  3. Non-vintage wines (e.g. NV Champagne) are treated as ready now.
     */
    fun windowFor(wine: Wine): DrinkWindow =
        windowFor(wine, WineStyle.classify(wine.grapeType, wine.name))

    /**
     * Same as [windowFor], but with the wine's [style] supplied by the caller so
     * a batch (e.g. the stats screen) can classify each wine once and reuse it.
     */
    fun windowFor(wine: Wine, style: WineStyle): DrinkWindow {
        if (wine.drinkFrom != null || wine.drinkTo != null) {
            return DrinkWindow(wine.drinkFrom, wine.drinkTo, estimated = false)
        }
        val vintage = wine.vintage
            ?: return DrinkWindow(null, null, estimated = true) // NV → judged READY below
        return DrinkWindow(
            from = vintage + style.minYearsAfterVintage,
            to = vintage + style.maxYearsAfterVintage,
            estimated = true,
        )
    }

    fun statusFor(wine: Wine, currentYear: Int): DrinkStatus =
        statusFor(wine, WineStyle.classify(wine.grapeType, wine.name), currentYear)

    /** [statusFor] with a caller-supplied [style] (see [windowFor]). */
    fun statusFor(wine: Wine, style: WineStyle, currentYear: Int): DrinkStatus {
        val w = windowFor(wine, style)
        // A non-vintage wine with no explicit window is ready to enjoy now.
        if (w.from == null && w.to == null) {
            return if (wine.vintage == null) DrinkStatus.READY else DrinkStatus.UNKNOWN
        }
        return w.status(currentYear)
    }
}
