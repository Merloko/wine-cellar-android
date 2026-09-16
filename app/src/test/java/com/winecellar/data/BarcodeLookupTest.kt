package com.winecellar.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// parse() uses org.json, provided by the Android runtime, so run under Robolectric.
@RunWith(RobolectricTestRunner::class)
class BarcodeLookupTest {

    @Test fun parses_a_found_product_taking_the_first_brand() {
        val json = """
            {"status":1,"product":{
                "product_name":"Grand Cordon Rosé",
                "brands":"G.H. Mumm, Mumm",
                "countries":"France, United Kingdom"
            }}
        """.trimIndent()
        val r = BarcodeLookup.parse(json)!!
        assertEquals("G.H. Mumm", r.winery)
        assertEquals("Grand Cordon Rosé", r.name)
    }

    @Test fun returns_null_when_not_found() {
        assertNull(BarcodeLookup.parse("""{"status":0,"status_verbose":"product not found"}"""))
    }

    @Test fun returns_null_when_product_has_no_useful_fields() {
        assertNull(BarcodeLookup.parse("""{"status":1,"product":{}}"""))
    }
}
