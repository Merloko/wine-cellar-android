package com.winecellar.domain

import com.winecellar.data.Wine
import org.junit.Assert.assertEquals
import org.junit.Test

class DrinkWindowTest {

    private val year = 2026

    private fun wine(
        vintage: Int? = null,
        grape: String? = null,
        from: Int? = null,
        to: Int? = null,
    ) = Wine(winery = "Test", vintage = vintage, grapeType = grape, drinkFrom = from, drinkTo = to)

    @Test fun recorded_window_takes_precedence() {
        val w = wine(vintage = 2004, grape = "Bordeaux", to = 2030)
        val window = DrinkWindowCalculator.windowFor(w)
        assertEquals(2030, window.to)
        assertEquals(false, window.estimated)
        assertEquals(DrinkStatus.READY, DrinkWindowCalculator.statusFor(w, year))
    }

    @Test fun non_vintage_is_ready_now() {
        val w = wine(grape = "Champagne")
        assertEquals(DrinkStatus.READY, DrinkWindowCalculator.statusFor(w, year))
    }

    @Test fun young_bold_red_is_cellaring() {
        val w = wine(vintage = 2025, grape = "Cabernet Sauvignon")
        val window = DrinkWindowCalculator.windowFor(w)
        assertEquals(2028, window.from) // 2025 + 3
        assertEquals(true, window.estimated)
        assertEquals(DrinkStatus.TOO_YOUNG, DrinkWindowCalculator.statusFor(w, year))
    }

    @Test fun old_rose_is_past_peak() {
        val w = wine(vintage = 2018, grape = "Rose")
        assertEquals(DrinkStatus.PAST_PEAK, DrinkWindowCalculator.statusFor(w, year))
    }

    @Test fun white_in_window_is_ready() {
        val w = wine(vintage = 2024, grape = "Sauvignon Blanc")
        assertEquals(DrinkStatus.READY, DrinkWindowCalculator.statusFor(w, year))
    }

    @Test fun window_label_formats() {
        assertEquals("2027–2031", DrinkWindow(2027, 2031, true).label())
        assertEquals("by 2030", DrinkWindow(null, 2030, false).label())
        assertEquals("from 2026", DrinkWindow(2026, null, false).label())
    }
}
