package com.loanmate.loan.emicalculator.carloan.homelloan.screens.dashboard

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import com.loanmate.loan.emicalculator.carloan.adkit.PromoPack
import com.loanmate.loan.emicalculator.carloan.adkit.Spots
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.ScreenBaseBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.databinding.RowToolBlockBinding
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.guides.ToursActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.emi.CalculatorActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.library.LineupActivity
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitAccentBold
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.shared.kitGlowPulse
import com.loanmate.loan.emicalculator.carloan.homelloan.screens.prefs.SettingsActivity
import com.intuit.sdp.R as SdpR

/**
 * The Home screen's content (Claude Design "Skin Hub Onboarding v2"): tiles, tools, animations and its ads. Shared by
 * [RootActivity] and the launcher's swipe-right panel (HostSheetFragment), as the reference app shares its HomeScreen.
 */
class RootFabric(
    private val activity: Activity,
    private val b: ScreenBaseBinding,
    /** RC place for the bottom and list ads: `screens.home`, or `launcher.host_app_panel` inside the launcher. */
    private val adPlace: String = PLACE,
) {
    private val resources = activity.resources

    private data class PackLoan(val icon: Int, val name: Int, val sub: Int, val open: () -> Intent)

    private val tools = listOf(
        PackLoan(R.drawable.glyph_bulb, R.string.tool_tips, R.string.tool_tips_sub) { ToursActivity.intent(activity, guides = false) },
        PackLoan(R.drawable.glyph_map, R.string.tool_maps, R.string.tool_maps_sub) { LineupActivity.intent(activity, ShelfOrder.MAPS) },
        PackLoan(R.drawable.glyph_diamond, R.string.tool_diamond, R.string.tool_diamond_sub) { ToursActivity.intent(activity, guides = true) },
        PackLoan(R.drawable.glyph_calc, R.string.tool_calculator, R.string.tool_calculator_sub) { Intent(activity, CalculatorActivity::class.java) },
    )

    fun bind() {
        b.settingsVw.setOnClickListener { open("settings") { Intent(activity, SettingsActivity::class.java) } }
        b.headlineVw.text = activity.getText(R.string.explore_today).kitAccentBold(b.headlineVw)
        b.glowVw.kitGlowPulse()

        // Tools as a one-column list ("Skin Hub Variants" Sticker Pop home).
        val gap = resources.getDimensionPixelSize(SdpR.dimen._8sdp)
        tools.chunked(1).forEach { pair ->
            val line = LinearLayout(activity)
            b.toolsVw.addView(line, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = gap })
            pair.forEachIndexed { i, tool ->
                val card = RowToolBlockBinding.inflate(activity.layoutInflater, line, true)
                if (i > 0) (card.root.layoutParams as LinearLayout.LayoutParams).marginStart = gap
                card.iconVw.setImageResource(tool.icon)
                card.nameVw.setText(tool.name)
                card.subVw.setText(tool.sub)
                card.root.setOnClickListener { open(resources.getResourceEntryName(tool.name)) { tool.open() } }
            }
        }
        mapOf(
            b.tileCharactersVw to ShelfOrder.CHARACTERS, b.tileBundlesVw to ShelfOrder.BUNDLES, b.tileEmotesVw to ShelfOrder.EMOTES,
            b.tileWeaponsVw to ShelfOrder.WEAPONS, b.tilePetsVw to ShelfOrder.PETS, b.tileVehiclesVw to ShelfOrder.VEHICLES,
        ).forEach { (tile, catalog) ->
            tile.setOnClickListener { open(catalog.name.lowercase()) { LineupActivity.intent(activity, catalog) } }
        }
        wireSpots()

        // Staggered entrance, as in the design's hcUp keyframes.
        listOf(b.crest, b.tileCharactersVw, b.tileBundlesVw, b.tileEmotesVw, b.tileWeaponsVw, b.tilePetsVw, b.tileVehiclesVw, b.toolsTitleVw, b.toolsVw)
            .forEachIndexed { i, v -> v.enter(i * 70L) }

        b.tileCharactersArtVw.loop("translationY", -resources.getDimension(SdpR.dimen._6sdp), 2000, 1000)
        b.tileBundlesArtVw.loop("translationY", -resources.getDimension(SdpR.dimen._4sdp), 1750, 1200)
        b.tileEmotesArtVw.loop("translationY", -resources.getDimension(SdpR.dimen._4sdp), 1300, 1400)
        b.tileWeaponsArtVw.loop("rotation", -6f, 1200, 1200)
        b.tilePetsArtVw.loop("translationY", -resources.getDimension(SdpR.dimen._5sdp), 1100, 1300)
        // Vehicle drives in from the right, then idles with a small back-and-forth.
        b.tileVehiclesArtVw.apply {
            translationX = resources.getDimension(SdpR.dimen._44sdp); alpha = 0f
            animate().translationX(0f).alpha(1f).setStartDelay(900).setDuration(900)
                .setInterpolator(DecelerateInterpolator(2f))
                .withEndAction { loop("translationX", -resources.getDimension(SdpR.dimen._4sdp), 900, 0) }.start()
        }
    }

    /** `bottom_ad` pinned under the list; `list_ad` inside the list at `list_ad_position` (after_header / after_tiles / after_tools). */
    private fun wireSpots() {
        Spots.bottom(activity, b.adSlotVw, adPlace)
        val place = PromoPack.config.places[adPlace]
        val list = b.adInlineVw.parent as ViewGroup
        list.removeView(b.adInlineVw)
        val at = when (place?.options?.get("list_ad_position") as? String) {
            "after_header" -> list.indexOfChild(b.crest) + 1
            "after_tools" -> list.childCount
            else -> list.indexOfChild(b.toolsTitleVw) // after_tiles: right above the "Tools & guides" title
        }
        list.addView(b.adInlineVw, at)
        if (place?.listAd?.on == true) Spots.list(activity, b.adInlineVw, adPlace) else b.adInlineVw.visibility = View.GONE
    }

    /** Every Home destination runs the `screens.home` exit ad first; navigation always continues. */
    private fun open(button: String, intent: () -> Intent) {
        MetricsTwo.click(button)
        Spots.exit(activity, PLACE) { activity.startActivity(intent()) }
    }

    private fun View.enter(delay: Long) {
        alpha = 0f
        translationY = resources.getDimension(SdpR.dimen._13sdp)
        animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(500)
            .setInterpolator(DecelerateInterpolator(2f)).start()
    }

    // Animators honour the system "remove animations" setting, matching the design's reduced-motion rule.
    private fun View.loop(property: String, to: Float, halfPeriodMs: Long, delayMs: Long) {
        ObjectAnimator.ofFloat(this, property, 0f, to).apply {
            duration = halfPeriodMs
            startDelay = delayMs
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    companion object {
        const val PLACE = "screens.home"
    }
}
