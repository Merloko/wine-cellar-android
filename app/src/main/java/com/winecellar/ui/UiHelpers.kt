package com.winecellar.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.winecellar.R
import com.winecellar.domain.DrinkStatus
import com.winecellar.domain.SortOrder
import com.winecellar.domain.WineStyle
import com.winecellar.ui.theme.Claret
import com.winecellar.ui.theme.Gold
import com.winecellar.ui.theme.StatusPast
import com.winecellar.ui.theme.StatusReady
import com.winecellar.ui.theme.StatusSoon
import com.winecellar.ui.theme.StatusYoung

/** Colour used for the small accent bar / dot that signals a wine's style. */
fun WineStyle.accentColor(): Color = when (this) {
    WineStyle.SPARKLING -> Color(0xFFE9C46A)
    WineStyle.WHITE -> Color(0xFFC9B458)
    WineStyle.ROSE -> Color(0xFFE59BB0)
    WineStyle.RED_LIGHT -> Color(0xFFB5566E)
    WineStyle.RED_MEDIUM -> Claret
    WineStyle.RED_BOLD -> Color(0xFF6A0F2B)
    WineStyle.UNKNOWN -> Gold
}

@StringRes
fun WineStyle.labelRes(): Int = when (this) {
    WineStyle.SPARKLING -> R.string.style_sparkling
    WineStyle.WHITE -> R.string.style_white
    WineStyle.ROSE -> R.string.style_rose
    WineStyle.RED_LIGHT -> R.string.style_red_light
    WineStyle.RED_MEDIUM -> R.string.style_red_medium
    WineStyle.RED_BOLD -> R.string.style_red_bold
    WineStyle.UNKNOWN -> R.string.style_unknown
}

@StringRes
fun SortOrder.labelRes(): Int = when (this) {
    SortOrder.WINERY -> R.string.sort_winery
    SortOrder.VINTAGE_NEWEST -> R.string.sort_vintage_newest
    SortOrder.VINTAGE_OLDEST -> R.string.sort_vintage_oldest
    SortOrder.RECENTLY_ADDED -> R.string.sort_recently_added
    SortOrder.DRINK_URGENCY -> R.string.sort_drink_urgency
}

data class StatusVisual(@StringRes val labelRes: Int, val color: Color)

fun DrinkStatus.visual(): StatusVisual = when (this) {
    DrinkStatus.PAST_PEAK -> StatusVisual(R.string.status_past_peak, StatusPast)
    DrinkStatus.READY -> StatusVisual(R.string.status_ready, StatusReady)
    DrinkStatus.TOO_YOUNG -> StatusVisual(R.string.status_cellaring, StatusYoung)
    DrinkStatus.UNKNOWN -> StatusVisual(R.string.status_unknown, StatusSoon)
}
