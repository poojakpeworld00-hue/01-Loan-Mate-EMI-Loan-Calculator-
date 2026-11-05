package com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import androidx.core.content.edit
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import kotlin.concurrent.thread

/**
 * Organic (track_a) or paid (track_b), decided like LightHouse: the Play install referrer carries a Google Spots gclid
 * of at least `gclid_min_length` that is not a known test value, and the device is not on Google's own network
 * (review / test installs). Organic until decided; once saved the verdict never changes. A referrer that can't be
 * read leaves it undecided and is retried on the next launch.
 */
object ShipOrigin {
    private const val TAG = "InstallAttribution"
    private const val PREFS = "install_attribution"
    private const val KEY_TRACK = "track"
    private const val IP_TIMEOUT_MS = 3000

    private val decided = CompletableDeferred<Unit>()
    private val main = Handler(Looper.getMainLooper())

    /** Until the splash ends a new verdict applies at once; after that it applies from the next launch. */
    @Volatile private var live = true

    fun saved(context: Context): Follow? =
        prefs(context).getString(KEY_TRACK, null)?.let { key -> Follow.entries.firstOrNull { it.key == key } }

    fun track(context: Context): Follow = saved(context) ?: Follow.A

    /** Reads the referrer when no verdict is saved yet; [onLiveVerdict] runs on the main thread if it lands before [lock]. */
    fun start(context: Context, spec: () -> OriginPlan, onLiveVerdict: () -> Unit) {
        val app = context.applicationContext
        if (saved(app) != null) return done()
        val client = runCatching { InstallReferrerClient.newBuilder(app).build() }.getOrElse { return done() }
        runCatching {
            client.startConnection(object : InstallReferrerStateListener {
                override fun onInstallReferrerSetupFinished(code: Int) {
                    thread(name = TAG) {
                        // A blocking call into the Play Store process: a known ANR source on the main thread.
                        val referrer = if (code == InstallReferrerClient.InstallReferrerResponse.OK) {
                            runCatching { client.installReferrer.installReferrer }.getOrNull()
                        } else null
                        runCatching { client.endConnection() }
                        if (referrer == null) return@thread done() // undecided: organic now, retried next launch
                        // First install: the rules come from the live config, so wait for its first fetch (offline: code defaults after 5s).
                        runBlocking { withTimeoutOrNull(5_000) { LoanSpecHolder.ready.first { it } } }
                        val s = spec()
                        val paidGclid = OriginChecks.paidGclid(referrer, s)
                        val googleIp = paidGclid && s.googleIpCheck && kitOnGoogleNet(s)
                        val reason = OriginChecks.reason(referrer, s, googleIp)
                        val track = if (reason == "gclid") Follow.B else Follow.A
                        main.post {
                            prefs(app).edit { putString(KEY_TRACK, track.key) }
                            Log.d(TAG, "verdict ${track.key} ($reason)")
                            MetricsTwo.log(MetricsTwo.INSTALL_SOURCE, "reason" to reason)
                            if (live) onLiveVerdict()
                            decided.complete(Unit)
                        }
                    }
                }

                override fun onInstallReferrerServiceDisconnected() = done()
            })
        }.onFailure { done() }
    }

    /** Waits up to [timeoutMs] for this launch's verdict (the splash calls it after its own hold). */
    suspend fun await(timeoutMs: Long) {
        withTimeoutOrNull(timeoutMs) { decided.await() }
    }

    /** The splash is over: a verdict landing later must not switch the config mid-flow. */
    fun lock() {
        live = false
    }

    private fun done() {
        main.post { decided.complete(Unit) }
    }

    /** A failed lookup counts as "not Google": the gclid alone decides. */
    private fun kitOnGoogleNet(s: OriginPlan): Boolean = runCatching {
        val conn = URL(s.ipEndpoint).openConnection() as HttpURLConnection
        conn.connectTimeout = IP_TIMEOUT_MS
        conn.readTimeout = IP_TIMEOUT_MS
        try {
            OriginChecks.kitGoogleNet(conn.inputStream.bufferedReader().use { it.readText() }, s)
        } finally {
            conn.disconnect()
        }
    }.getOrDefault(false)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** Pure rules of [ShipOrigin]. */
internal object OriginChecks {
    /** The `gclid` value of a Play install referrer ("utm_source=…&gclid=…"), URL-decoded and trimmed; null when absent. */
    fun gclid(referrer: String?): String? = referrer.orEmpty().split('&').firstNotNullOfOrNull { part ->
        if (!part.substringBefore('=').trim().equals("gclid", ignoreCase = true)) return@firstNotNullOfOrNull null
        runCatching { URLDecoder.decode(part.substringAfter('=', ""), "UTF-8") }.getOrNull()?.trim()?.ifEmpty { null }
    }

    /** `install_source` reason: no_referrer / no_gclid / fake_gclid / google_ip, or gclid (= paid). */
    fun reason(referrer: String?, spec: OriginPlan, kitOnGoogleNet: Boolean): String = when {
        referrer.isNullOrBlank() -> "no_referrer"
        gclid(referrer) == null -> "no_gclid"
        !paidGclid(referrer, spec) -> "fake_gclid"
        kitOnGoogleNet -> "google_ip"
        else -> "gclid"
    }

    /** Paid referrer: a gclid of at least `gclid_min_length` characters that is not in `fake_gclids`. */
    fun paidGclid(referrer: String?, spec: OriginPlan): Boolean {
        val g = gclid(referrer) ?: return false
        return g.length >= spec.gclidMinLength && g !in spec.fakeGclids
    }

    /** The IP service's [OriginPlan.ipField] equals one of `match` (case-insensitive). */
    fun kitGoogleNet(ipJson: String, spec: OriginPlan): Boolean {
        val value = runCatching { JSONObject(ipJson).optString(spec.ipField) }.getOrDefault("").trim()
        return value.isNotEmpty() && spec.ipMatch.any { it.trim().equals(value, ignoreCase = true) }
    }
}
