package com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OriginChecksTest {
    private val spec = OriginPlan(
        gclidMinLength = 20,
        fakeGclids = listOf("123456789"),
        googleIpCheck = true,
        ipField = "asn_org",
        ipMatch = listOf("Google LLC"),
    )
    private val realGclid = "Cj0KCQjw-test-gclid-value-1234567890"

    @Test fun kitOrganicPlayReferrer_isNotPaid() {
        assertFalse(OriginChecks.paidGclid("utm_source=google-play&utm_medium=organic", spec))
        assertFalse(OriginChecks.paidGclid("", spec))
        assertFalse(OriginChecks.paidGclid(null, spec))
    }

    @Test fun kitRealGclid_isPaid_anyPositionAndEncoded() {
        assertTrue(OriginChecks.paidGclid("gclid=$realGclid&utm_source=google-play", spec))
        assertTrue(OriginChecks.paidGclid("utm_source=google-play&GCLID=Cj0KCQjw%2Dtest%2Dgclid%2Dvalue%2D12345", spec))
        assertEquals("Cj0KCQjw-test-gclid-value-12345", OriginChecks.gclid("a=1&gclid=Cj0KCQjw%2Dtest%2Dgclid%2Dvalue%2D12345"))
    }

    @Test fun kitShortOrFakeGclid_isOrganic() {
        assertFalse(OriginChecks.paidGclid("gclid=123456789", spec.copy(gclidMinLength = 1))) // fake signature
        assertFalse(OriginChecks.paidGclid("gclid=short", spec))
        assertNull(OriginChecks.gclid("gclid=&utm_medium=cpc"))
        assertFalse(OriginChecks.paidGclid("utm_medium=cpc", spec)) // like LightHouse, only a gclid counts
    }

    @Test fun kitGoogleNetwork_matchesField() {
        assertTrue(OriginChecks.kitGoogleNet("""{"ip":"1.2.3.4","asn_org":"Google LLC"}""", spec))
        assertTrue(OriginChecks.kitGoogleNet("""{"asn_org":" google llc "}""", spec))
        assertFalse(OriginChecks.kitGoogleNet("""{"asn_org":"Reliance Jio"}""", spec))
        assertFalse(OriginChecks.kitGoogleNet("not json", spec))
        assertFalse(OriginChecks.kitGoogleNet("""{"asn_org":""}""", spec.copy(ipMatch = listOf(""))))
    }

    @Test fun deployFeedReason() {
        assertEquals("no_referrer", OriginChecks.reason("", spec, false))
        assertEquals("no_gclid", OriginChecks.reason("utm_source=google-play&utm_medium=organic", spec, false))
        assertEquals("fake_gclid", OriginChecks.reason("gclid=123456789", spec, false))
        assertEquals("google_ip", OriginChecks.reason("gclid=$realGclid", spec, true))
        assertEquals("gclid", OriginChecks.reason("gclid=$realGclid", spec, false))
    }
}
