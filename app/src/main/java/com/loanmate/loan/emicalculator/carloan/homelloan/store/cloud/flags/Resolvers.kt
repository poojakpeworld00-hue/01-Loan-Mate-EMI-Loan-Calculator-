package com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/** Picks the audience: track_a (organic) or track_b (paid). */
fun interface TrailFinder {
    fun track(): Follow

    companion object {
        /** Organic until a resolver is set; the app uses [ShipOrigin]. */
        val Default = TrailFinder { Follow.A }
    }
}

/** Country for `countries` overlays: SIM, then network, then locale. Upper-case 2 letters, or "" when unknown. */
object NationFinder {
    fun countryCode(context: Context): String {
        val tm = runCatching { context.getSystemService(TelephonyManager::class.java) }.getOrNull()
        return sequenceOf({ tm?.simCountryIso }, { tm?.networkCountryIso }, { Locale.getDefault().country })
            .mapNotNull { runCatching(it).getOrNull()?.trim()?.uppercase(Locale.ROOT) }
            .firstOrNull { it.length == 2 && it.all { c -> c in 'A'..'Z' } } ?: ""
    }
}
