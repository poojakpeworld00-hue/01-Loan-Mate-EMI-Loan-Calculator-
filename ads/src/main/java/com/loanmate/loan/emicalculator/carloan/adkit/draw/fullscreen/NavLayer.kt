package com.loanmate.loan.emicalculator.carloan.adkit.draw.fullscreen

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.loanmate.loan.emicalculator.carloan.adkit.model.PromoBayType
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoEra
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.PromoSignals
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoSettingsBox
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoFetchStyle
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoDelays
import com.loanmate.loan.emicalculator.carloan.adkit.rules.PromoPieceKeys
import com.loanmate.loan.emicalculator.carloan.adkit.draw.kitHasNetwork
import com.loanmate.loan.emicalculator.carloan.adkit.draw.linksOpenIn
import com.loanmate.loan.emicalculator.carloan.adkit.draw.enterInBrowserTool
import com.loanmate.loan.emicalculator.carloan.adkit.draw.AdvancePromoLead
import com.loanmate.loan.emicalculator.carloan.adkit.draw.RewardPromoRenderer
import com.loanmate.loan.emicalculator.carloan.adkit.bridge.kitOnceWindowFocused
import com.loanmate.loan.emicalculator.carloan.adkit.R
import com.loanmate.loan.emicalculator.carloan.adkit.draw.SponsorBrowserActivity
class NavLayer {
    companion object {
        private const val LINK_TAG = "LinkFirst"

        /** A link that has not covered the app by then never will (blocked / no browser). */
        private const val NOT_COVERED_MS = 3_000L
        /** How long the links wait for Chrome's Custom Tabs service before opening without a session. */
        private const val SESSION_WAIT_MS = 1_000L

        private var _isInterShow: Boolean = false
        var isInterShow: Boolean
            get() = _isInterShow
            set(value) {
                _isInterShow = value
            }
        private var googleInterAd: InterstitialAd? = null
        private var googleInterAt = 0L
        /** True while the global unit is loading too, so the chain does not request it twice. */
        @Volatile var googleInterLoading = false
        val hasPreloadedGoogle: Boolean get() = kitFreshGoogle() != null

        private fun kitFreshGoogle(): InterstitialAd? {
            if (googleInterAd != null && PromoEra.stale(googleInterAt, PromoEra.FULL_SCREEN_MS)) googleInterAd = null
            return googleInterAd
        }

        // Hands the preloaded Google inter to one caller: it leaves the cache before show(), so it can't show twice.
        fun kitTakePreloadedGoogle(): InterstitialAd? = kitFreshGoogle().also { googleInterAd = null }
        var isOpened = false
        var onTabClosed: (() -> Unit)? = null
        /**
         * A screen of ours stopped since the links opened. Only then is a resume the user coming back: a resume before the
         * tabs cover us (our screen returning over Recents) ended the step early and left the tabs open under the next ad.
         */
        private var covered = false
        /** The links have been started: only a stop after this is them covering us (not a Recents press while opening). */
        private var launched = false
        fun onToolCovered() { if (isOpened && launched) covered = true }
        fun onReturnFromOptions(context: Context, force: Boolean = false) {
            if (!isOpened || (!covered && !force)) return

            if (context is SponsorBrowserActivity) return

            Log.d(LINK_TAG, "tab stack closed → app resumed (${context.javaClass.simpleName}), firing onClosed")
            // Clear state before the callback: it can open the next link, which registers its own onTabClosed.
            val closed = onTabClosed
            isOpened = false
            covered = false
            launched = false
            onTabClosed = null
            closeSession(context)
            closed?.invoke()
        }
        private var customTabsClient: CustomTabsClient? = null
        private var customTabsSession: CustomTabsSession? = null
        private var serviceConnection: CustomTabsServiceConnection? = null

        fun closeSession(context: Context) {
            serviceConnection?.let {
                try {
                    context.unbindService(it)
                } catch (_: Exception) {}
            }
            serviceConnection = null
            customTabsClient = null
            customTabsSession = null
        }

        fun enterPatronLink(
            context: Activity,
            placement: String? = null,
            urls: List<String> = PromoSettingsBox.getInstance(context).patronLinks(placement),
            onClosed: () -> Unit,
        ) {
            if (urls.isEmpty()) {
                Log.d(LINK_TAG, "no DirectLink for placement=$placement → skip")
                onClosed()
                return
            }

            val openIn = PromoSettingsBox.getInstance(context).linksOpenIn(placement)
            Log.d(LINK_TAG, "opening ${urls.size} link(s) in $openIn for placement=$placement: $urls")
            isOpened = true
            covered = false
            launched = false
            onTabClosed = onClosed
            // A link that never covered us (launch blocked, no browser bound) must not leave the exit ad waiting for a
            // return that can't come: that kept the runner busy, so every onboarding button did nothing.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isOpened && onTabClosed === onClosed && context.hasWindowFocus()) {
                    Log.w(LINK_TAG, "link never covered the app → treating it as closed")
                    onReturnFromOptions(context, force = true)
                }
            }, NOT_COVERED_MS)

            if (openIn == "browser") {
                // The phone's browser app; onReturnFromSettings fires onClosed when the user is back in the app.
                val opened = urls.asReversed().count { context.enterInBrowserTool(Uri.parse(it)) }
                Log.d(LINK_TAG, "browser app: $opened/${urls.size} launched")
                launched = true
                if (opened == 0) {
                    isOpened = false
                    onTabClosed = null
                    onClosed()
                }
                return
            }

            if (openIn == "webview") {
                for ((i, url) in urls.asReversed().withIndex()) {
                    try {
                        context.startActivity(SponsorBrowserActivity.kitIntentFor(context, url))
                        Log.d(LINK_TAG, "webview ${i + 1}/${urls.size} launched: $url")
                        launched = true
                    } catch (e: Exception) {
                        Log.w(LINK_TAG, "webview ${i + 1}/${urls.size} failed: $url — ${e.message}")
                        if (i == 0) return kitViewInBrowser(context, Uri.parse(url), onClosed)
                    }
                }
                return
            }

            getSession(context) { session ->
                Log.d(LINK_TAG, "custom tabs session=${if (session == null) "null (no Chrome bind)" else "ok"}")

                val customTab = CustomTabsIntent.Builder(session)
                    .setShowTitle(true)
                    .setToolbarColor(ContextCompat.getColor(context, R.color.adk_black))
                    .build()

                for ((i, url) in urls.asReversed().withIndex()) {
                    try {
                        customTab.launchUrl(context, Uri.parse(url))
                        Log.d(LINK_TAG, "tab ${i + 1}/${urls.size} launched: $url")
                        launched = true
                    } catch (e: Exception) {
                        Log.w(LINK_TAG, "tab ${i + 1}/${urls.size} failed: $url — ${e.message}")

                        if (i == 0) return@getSession kitViewInBrowser(context, Uri.parse(url), onClosed)
                    }
                }
            }
        }

        private fun getSession(
            context: Context,
            onReady: (CustomTabsSession?) -> Unit
        ) {
            if (customTabsSession != null) {
                onReady(customTabsSession)
                return
            }

            // A disconnect (or a reconnect) after the first callback would open the links again.
            var delivered = false
            val deliver = { session: CustomTabsSession? -> if (!delivered) { delivered = true; onReady(session) } }
            serviceConnection = object : CustomTabsServiceConnection() {
                override fun onCustomTabsServiceConnected(
                    name: ComponentName,
                    client: CustomTabsClient
                ) {
                    customTabsClient = client
                    customTabsSession = client.newSession(null)
                    deliver(customTabsSession)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    customTabsClient = null
                    customTabsSession = null
                    deliver(null)
                }
            }

            val bound = CustomTabsClient.bindCustomTabsService(
                context,
                "com.android.chrome",
                serviceConnection as CustomTabsServiceConnection
            )

            if (!bound) deliver(null)
            // Chrome's service can stay unbound (its process not starting): open the tabs without a session after 1s.
            else android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ deliver(null) }, SESSION_WAIT_MS)
        }

        private fun kitViewInBrowser(
            context: Activity,
            uri: Uri,
            onClosed: () -> Unit
        ) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
            } finally {
                // onTabClosed is this same onClosed, so fire it once.
                isOpened = false
                onTabClosed = null
                onClosed()
            }
        }
    }

    fun obtainInterstitials(activity: Activity) {
        val pref = PromoSettingsBox.getInstance(activity)
        if (!pref.getBoolean("IsAdsON")) return

        if (!pref.getBoolean("InterAds")) return

        // Places set to preload are warmed even when interstitials are on-load overall. With
        // preload on overall the shared unit is warmed below, so only their own units go here.
        val globalPreload = PromoFetchStyle.preload(activity, PromoFetchStyle.PackShape.INTER)
        PromoFetchStyle.kitInterPreloadPlaces(activity).forEach { place ->
            val unit = PromoPieceKeys.resolve(pref, place, "googleInter")
            if (!globalPreload || unit != pref.getString("googleInter")) LinkLayer.preload(activity, unit, place)
        }
        if (!globalPreload) return

        val adType = PromoBayType.fromString(pref.getString("IsAdType"))

        if (adType == PromoBayType.GOOGLE) {
            if (hasPreloadedGoogle || googleInterLoading) return
            activity.quietWrite("google_inter_load_start")
            val id = pref.getString("googleInter") ?: return
            googleInterLoading = true
            PromoSignals.send(PromoSignals.INTER, "request", "") // a warm-up preload has no place
            InterstitialAd.load(
                activity, id, AdRequest.Builder().build(),
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        PromoSignals.loadFailed(PromoSignals.INTER, error.code, "")
                        googleInterAd = null
                        googleInterLoading = false
                        activity.quietWrite("google_inter_load_failed_${error.code}")
                        Log.e("NavigationInterstitial", "Load Failed: ${error.message}")
                    }
                    override fun onAdLoaded(ad: InterstitialAd) {
                        PromoSignals.send(PromoSignals.INTER, "loaded", "")
                        googleInterAd = ad
                        googleInterAt = PromoEra.now()
                        googleInterLoading = false
                        activity.quietWrite("google_inter_loaded")
                        Log.d("NavigationInterstitial", "Google inter loaded")
                    }
                })
        }

    }


    private var fallbackPlacement: String? = null









    private fun Context.quietWrite(event: String) {
        Log.d("InterADsLog", event)
    }
}
