package com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AreaChecksTest {
    private val surat = AreaTwo(country = "India", countryCode = "IN", state = "Gujarat", stateCode = "GJ", city = "Surat")

    @Test fun kitAnyOfTheFiveFieldsMatches() {
        listOf("India", "IN", "Gujarat", "GJ", "Surat").forEach { assertTrue(it, AreaChecks.matches(listOf(it), surat)) }
    }

    @Test fun taskAndSpacesDoNotMatter() {
        listOf("india", "InDia", " INDIA ", "gj", "surat").forEach { assertTrue(it, AreaChecks.matches(listOf(it), surat)) }
    }

    @Test fun kitOtherPlacesDoNotMatch() {
        assertFalse(AreaChecks.matches(listOf("Pakistan", "MH", "Mumbai"), surat))
    }

    @Test fun emptyRosterOrUnknownRegionNeverMatches() {
        assertFalse(AreaChecks.matches(emptyList(), surat))
        assertFalse(AreaChecks.matches(listOf("India"), null))
        assertFalse(AreaChecks.matches(listOf("", " "), AreaTwo()))
    }
}
