package com.loanmate.loan.emicalculator.carloan.adkit.draw

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.view.View
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import com.loanmate.loan.emicalculator.carloan.adkit.R
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoPieceKeys

fun kitHasNet(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun View.visible() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.invisible() {
    visibility = View.GONE
}

/** `links_open_in` for [placement]: "webview", "in_app_browser" (default) or "browser". */
fun PromoSettingsBox.linksLaunchIn(placement: String? = null): String =
    PromoPieceKeys.resolve(this, placement, "link_open_in").trim().lowercase().takeIf { it == "webview" || it == "browser" } ?: "in_app_browser"

/** Opens [uri] in the phone's browser app (outside our app); false when nothing can open it. */
fun Activity.enterInBrowserLoan(uri: Uri): Boolean =
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)) }.isSuccess

/** House-ad CTA: opens the first sponsor link (ads.links). */
fun pushSponsorUrl(context: Context?) {
    val activity = context as? Activity ?: return
    if (activity.isFinishing || activity.isDestroyed) return

    val adsPref = PromoSettingsBox.getInstance(activity)
    if (!adsPref.getBoolean("IsCustomADS")) return

    val url = adsPref.patronUrls().firstOrNull() ?: return

    val uri = runCatching { Uri.parse(url).takeIf { it.scheme != null } }.getOrNull() ?: return

    val openIn = adsPref.linksLaunchIn()
    if (openIn == "webview" &&
        runCatching { activity.startActivity(SponsorBrowserActivity.kitIntentFor(activity, url)) }.isSuccess
    ) return

    // Coming back from the browser is our own return: no app-open ad on it.
    AdvancePromoLead.skipNextAppOpenAd = true
    if (openIn == "browser" && activity.enterInBrowserLoan(uri)) return
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setToolbarColor(ContextCompat.getColor(activity, R.color.adk_black))
            .build()
            .launchUrl(activity, uri)
    } catch (e: Exception) {
        runCatching { activity.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
    }
}
