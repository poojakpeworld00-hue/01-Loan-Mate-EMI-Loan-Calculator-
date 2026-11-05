package com.fffskintools.skin.tools.emotes.data.remote.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionGuardsTest {
    private val surat = Region2(country = "India", countryCode = "IN", state = "Gujarat", stateCode = "GJ", city = "Surat")

    @Test fun kitAnyOfTheFiveFieldsMatches() {
        listOf("India", "IN", "Gujarat", "GJ", "Surat").forEach { assertTrue(it, RegionGuards.matches(listOf(it), surat)) }
    }

    @Test fun taskAndSpacesDoNotMatter() {
        listOf("india", "InDia", " INDIA ", "gj", "surat").forEach { assertTrue(it, RegionGuards.matches(listOf(it), surat)) }
    }

    @Test fun kitOtherPlacesDoNotMatch() {
        assertFalse(RegionGuards.matches(listOf("Pakistan", "MH", "Mumbai"), surat))
    }

    @Test fun emptyRosterOrUnknownRegionNeverMatches() {
        assertFalse(RegionGuards.matches(emptyList(), surat))
        assertFalse(RegionGuards.matches(listOf("India"), null))
        assertFalse(RegionGuards.matches(listOf("", " "), Region2()))
    }
}
