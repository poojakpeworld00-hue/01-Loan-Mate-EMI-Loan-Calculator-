package com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags

import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromoStep
import com.loanmate.loan.emicalculator.carloan.adkit.setup.BayOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LoanSpecParserTest {
    // Unit tests run with the module directory as working directory.
    private fun doc(name: String) = File("../docs/remote-config/$name.json").readText()

    @Test
    fun `release template ships with ads off and no unit ids`() {
        val c = LoanSpecParser.parse(doc("app_config"), Follow.A, "")
        assertFalse(c.ads.on)
        assertEquals("", c.ads.units.inter)
    }

    @Test
    fun `debug template parses every FFF place`() {
        val c = LoanSpecParser.parse(doc("app_config_debug"), Follow.A, "")
        assertFalse(c.ads.on) // organic track: ads off, as the reference config
        assertEquals(10, c.onboarding.order.size) // the 9 loan steps + default_home
        assertEquals(OnboardScreen.LANGUAGE, c.onboarding.order.first())
        assertTrue(c.launcher.on)
        assertEquals(listOf(PromoStep.INTER), c.ads.places["screens.splash"]!!.exitAd!!.ads)
        assertEquals(BayOrder.NATIVE_BIG, c.ads.places["onboarding.OB_1"]!!.bottomAd!!.type)
        assertFalse(c.ads.places["onboarding.OB_1"]!!.listAd!!.on)
        assertEquals(BayOrder.NATIVE_BIG, c.ads.places["onboarding.OB_2"]!!.listAd!!.type)
        assertEquals(listOf(PromoStep.LINK, PromoStep.INTER, PromoStep.APP_OPEN), c.ads.places["onboarding.OB_4"]!!.exitAd!!.ads)
        assertEquals(BayOrder.BANNER, c.ads.places["screens.home"]!!.bottomAd!!.type)
        assertEquals(BayOrder.NATIVE_BANNER, c.ads.places["screens.home"]!!.bottomAd!!.onFail)
        assertEquals(null, c.ads.places["screens.home"]!!.listAd!!.onFail)
        assertEquals(BayOrder.NATIVE_MID, c.ads.places["screens.home"]!!.listAd!!.type)
        assertEquals("after_tiles", c.ads.places["screens.home"]!!.options["list_ad_position"])
        assertEquals(BayOrder.NATIVE_BIG, c.ads.places["launcher.host_app_panel"]!!.listAd!!.type)
        assertTrue(c.ads.places.containsKey("launcher.back_button"))
        assertEquals(30, c.launcher.sponsoredApps.list.size)
    }

    @Test
    fun `missing keys fall back to defaults`() {
        val c = LoanSpecParser.parse("{}", Follow.A, "IN")
        assertFalse(c.ads.on)
        assertEquals(9, c.onboarding.order.size) // every loan step, no default_home
        assertFalse(c.launcher.on)
        assertFalse(c.onboarding.skipButton)
        assertEquals(4, c.base.splashWaitSec)
        assertTrue(c.base.noInternetDialog)
        assertTrue(c.ads.places.isEmpty())
    }

    @Test
    fun `settings rows default on and turn off per track`() {
        assertEquals(SettingsLines(), LoanSpecParser.parse("{}", Follow.A, "").settingsRows)
        val json = """{"track_a": {}, "track_b": {"settings": {"share_app": false, "update_app": false}}}"""
        assertEquals(SettingsLines(), LoanSpecParser.parse(json, Follow.A, "").settingsRows)
        assertEquals(SettingsLines(share = false, update = false), LoanSpecParser.parse(json, Follow.B, "").settingsRows)
    }

    @Test
    fun `paid track sets the launcher first, as the reference`() {
        val c = LoanSpecParser.parse(doc("app_config_debug"), Follow.B, "")
        assertEquals(OnboardScreen.DEFAULT_HOME, c.onboarding.order.first())
        assertTrue(c.uninstallFlow.premium)
        assertTrue(c.ads.on)
        assertFalse(c.ads.loaderOnMiss)
        assertEquals(true, c.ads.places["onboarding.default_home"]!!.options["loader_on_miss"])
        assertEquals(listOf(PromoStep.LINK, PromoStep.INTER, PromoStep.APP_OPEN), c.ads.places["onboarding.OB_6"]!!.exitAd!!.ads)
        assertFalse(c.ads.places["launcher.app_resume"]!!.exitAd!!.on) // reference: paid has no resume ad
        assertTrue(c.launcher.sponsoredApps.on)
        assertEquals(6, c.ads.places["screens.list_characters"]!!.int("list_ad_after"))
        assertEquals(6, c.ads.places["screens.list_characters"]!!.int("list_ad_every"))
        assertEquals(2, c.ads.places["screens.list_emotes"]!!.int("list_ad_after"))
        assertEquals(BayOrder.NATIVE_MID, c.ads.places["screens.list_maps"]!!.listAd!!.type)
        assertEquals(BayOrder.NATIVE_BIG, c.ads.places["screens.calc"]!!.listAd!!.type)
        assertEquals(BayOrder.NATIVE_BIG, c.ads.places["screens.settings"]!!.listAd!!.type)
    }

    @Test
    fun `country overlay merges into the track`() {
        val json = """{"track_a":{"ads":{"on":false},"countries":{"IN,PK":{"ads":{"on":true}}}}}"""
        assertTrue(LoanSpecParser.parse(json, Follow.A, "pk").ads.on)
        assertFalse(LoanSpecParser.parse(json, Follow.A, "US").ads.on)
    }

    @Test(expected = org.json.JSONException::class)
    fun `invalid json is rejected`() {
        LoanSpecParser.parse("not json", Follow.A, "")
    }
}
