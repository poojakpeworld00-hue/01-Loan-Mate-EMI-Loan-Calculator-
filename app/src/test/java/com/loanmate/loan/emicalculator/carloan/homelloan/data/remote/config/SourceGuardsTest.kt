package com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceGuardsTest {
    private val spec = SourceSpec(
        gclidMinLength = 20,
        fakeGclids = listOf("123456789"),
        googleIpCheck = true,
        ipField = "asn_org",
        ipMatch = listOf("Google LLC"),
    )
    private val realGclid = "Cj0KCQjw-test-gclid-value-1234567890"

    @Test fun kitOrganicPlayReferrer_isNotPaid() {
        assertFalse(SourceGuards.paidGclid("utm_source=google-play&utm_medium=organic", spec))
        assertFalse(SourceGuards.paidGclid("", spec))
        assertFalse(SourceGuards.paidGclid(null, spec))
    }

    @Test fun kitRealGclid_isPaid_anyPositionAndEncoded() {
        assertTrue(SourceGuards.paidGclid("gclid=$realGclid&utm_source=google-play", spec))
        assertTrue(SourceGuards.paidGclid("utm_source=google-play&GCLID=Cj0KCQjw%2Dtest%2Dgclid%2Dvalue%2D12345", spec))
        assertEquals("Cj0KCQjw-test-gclid-value-12345", SourceGuards.gclid("a=1&gclid=Cj0KCQjw%2Dtest%2Dgclid%2Dvalue%2D12345"))
    }

    @Test fun kitShortOrFakeGclid_isOrganic() {
        assertFalse(SourceGuards.paidGclid("gclid=123456789", spec.copy(gclidMinLength = 1))) // fake signature
        assertFalse(SourceGuards.paidGclid("gclid=short", spec))
        assertNull(SourceGuards.gclid("gclid=&utm_medium=cpc"))
        assertFalse(SourceGuards.paidGclid("utm_medium=cpc", spec)) // like LightHouse, only a gclid counts
    }

    @Test fun kitGoogleNetwork_matchesField() {
        assertTrue(SourceGuards.kitGoogleNetwork("""{"ip":"1.2.3.4","asn_org":"Google LLC"}""", spec))
        assertTrue(SourceGuards.kitGoogleNetwork("""{"asn_org":" google llc "}""", spec))
        assertFalse(SourceGuards.kitGoogleNetwork("""{"asn_org":"Reliance Jio"}""", spec))
        assertFalse(SourceGuards.kitGoogleNetwork("not json", spec))
        assertFalse(SourceGuards.kitGoogleNetwork("""{"asn_org":""}""", spec.copy(ipMatch = listOf(""))))
    }

    @Test fun deployFeedReason() {
        assertEquals("no_referrer", SourceGuards.reason("", spec, false))
        assertEquals("no_gclid", SourceGuards.reason("utm_source=google-play&utm_medium=organic", spec, false))
        assertEquals("fake_gclid", SourceGuards.reason("gclid=123456789", spec, false))
        assertEquals("google_ip", SourceGuards.reason("gclid=$realGclid", spec, true))
        assertEquals("gclid", SourceGuards.reason("gclid=$realGclid", spec, false))
    }
}
