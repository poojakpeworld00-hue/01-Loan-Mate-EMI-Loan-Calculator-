package com.loanmate.loan.emicalculator.carloan.homepanel.onboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.PartnerProg
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetCenter

/** Sponsored drawer tiles (settings `sponsoredApps`), one after every `every` apps. */
object FeaturedCards {

    private const val PACKAGE_PREFIX = "com.loanmate.loan.emicalculator.carloan.homepanel.sponsored."

    private val tiles = HashMap<String, PartnerProg>()

    fun isSponsored(item: WidgetCenter) = item.packageName.startsWith(PACKAGE_PREFIX)

    fun logo(item: WidgetCenter): String = tiles[item.packageName]?.logo.orEmpty()

    fun interleave(apps: List<WidgetCenter>): List<WidgetCenter> {
        val setup = Launcher.settings().sponsoredApps
        val feed = setup.list.filter { it.landingUrl.isNotBlank() }
        if (!setup.on || feed.isEmpty() || apps.isEmpty()) return apps
        tiles.clear()
        val every = setup.every.coerceAtLeast(1)
        val out = ArrayList<WidgetCenter>(apps.size + feed.size)
        var fed = 0
        apps.forEachIndexed { i, app ->
            out.add(app)
            if ((i + 1) % every == 0 && fed < feed.size) out.add(marker(feed[fed], fed++))
        }
        return out
    }

    private fun marker(ad: PartnerProg, index: Int): WidgetCenter {
        tiles["$PACKAGE_PREFIX$index"] = ad // per tile: several tiles may share one landing_url
        return WidgetCenter(
            id = null,
            title = ad.label,
            packageName = "$PACKAGE_PREFIX$index",
            activityName = ad.landingUrl,
            order = 0,
            thumbnailColor = 0,
            drawable = null,
        )
    }

    /** Tap: host moment first, then the landing URL; only a link that really opened counts as "away". */
    fun open(activity: Activity, item: WidgetCenter) {
        val tile = tiles[item.packageName] ?: PartnerProg(logo = "", landingUrl = item.activityName, label = item.title)
        var done = false
        val proceed = {
            if (!done) {
                done = true
                val opened = runCatching {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(tile.landingUrl)))
                }.onFailure { Log.w("LAUNCHER_PROMO", "could not open sponsored link", it) }.isSuccess
                if (opened) CenterDealBroker.away = CenterDealBroker.Away.SPONSORED
            }
        }
        runCatching { Launcher.events.onSponsoredTileTapped(activity, tile, proceed) }.onFailure { proceed() }
    }
}
