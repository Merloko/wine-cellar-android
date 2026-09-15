package com.winecellar.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WineStyleTest {

    @Test fun champagne_is_sparkling() {
        assertEquals(WineStyle.SPARKLING, WineStyle.classify("Champagne"))
        assertEquals(WineStyle.SPARKLING, WineStyle.classify(null, "Grand Cordon Brut"))
    }

    @Test fun rose_detected_before_red() {
        assertEquals(WineStyle.ROSE, WineStyle.classify("Rose"))
        assertEquals(WineStyle.ROSE, WineStyle.classify("Syrah Rose"))
        assertEquals(WineStyle.ROSE, WineStyle.classify("Cabernet D'Anjou"))
    }

    @Test fun bold_reds() {
        assertEquals(WineStyle.RED_BOLD, WineStyle.classify("Cabernet Sauvignon"))
        assertEquals(WineStyle.RED_BOLD, WineStyle.classify("Malbec;Cabernet;Petit Verdot"))
        assertEquals(WineStyle.RED_BOLD, WineStyle.classify("Bordeaux"))
    }

    @Test fun medium_reds() {
        assertEquals(WineStyle.RED_MEDIUM, WineStyle.classify("Grenache;Tempranillo"))
        assertEquals(WineStyle.RED_MEDIUM, WineStyle.classify("Tempranillo"))
    }

    @Test fun whites() {
        assertEquals(WineStyle.WHITE, WineStyle.classify("Sauvignon Blanc"))
        assertEquals(WineStyle.WHITE, WineStyle.classify("Chardonnay"))
        assertEquals(WineStyle.WHITE, WineStyle.classify("Reisling")) // owner's spelling
    }

    @Test fun light_red_pinot() {
        assertEquals(WineStyle.RED_LIGHT, WineStyle.classify(null, "Pinot Noir"))
    }

    @Test fun unknown_when_blank() {
        assertEquals(WineStyle.UNKNOWN, WineStyle.classify(null))
        assertEquals(WineStyle.UNKNOWN, WineStyle.classify(""))
    }
}
