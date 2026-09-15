package com.winecellar.ui

import androidx.compose.ui.graphics.Color
import com.winecellar.domain.DrinkStatus
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

data class StatusVisual(val label: String, val color: Color)

fun DrinkStatus.visual(): StatusVisual = when (this) {
    DrinkStatus.PAST_PEAK -> StatusVisual("Drink up", StatusPast)
    DrinkStatus.READY -> StatusVisual("Drink now", StatusReady)
    DrinkStatus.TOO_YOUNG -> StatusVisual("Cellaring", StatusYoung)
    DrinkStatus.UNKNOWN -> StatusVisual("—", StatusSoon)
}
