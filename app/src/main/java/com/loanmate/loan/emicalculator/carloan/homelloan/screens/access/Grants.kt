package com.loanmate.loan.emicalculator.carloan.homelloan.screens.access

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.loanmate.loan.emicalculator.carloan.adkit.metrics.MetricsTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanSpecHolder
import com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags.LoanAllow

/** Icon and name shown for [this] in the Home permission sheet. */
val LoanAllow.display: Pair<Int, Int>
    get() = when (this) {
        LoanAllow.NOTIFICATION -> R.drawable.glyph_bell to R.string.ph_notif
        // FFF declares only the notification permission; the others are never pending, so they share a generic row.
        else -> R.drawable.glyph_shield to R.string.perm_title
    }

/** Remote Config `permissions.<screen>`: which runtime permissions a screen asks, in order (reference app rules). */
object Grants {
    /** A permission prompt (or the overlay settings page) is in front: the screen visit goes on. */
    @Volatile var asking = false

    /** The runtime permission behind [p], or null when none is needed on this device (photos use the system picker). */
    fun kitManifestName(p: LoanAllow): String? = when (p) {
        LoanAllow.CAMERA -> Manifest.permission.CAMERA
        LoanAllow.PHOTOS -> null
        LoanAllow.NOTIFICATION -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
        LoanAllow.CONTACTS -> Manifest.permission.READ_CONTACTS
        LoanAllow.PHONE -> Manifest.permission.READ_PHONE_STATE
        LoanAllow.OVERLAY -> Manifest.permission.SYSTEM_ALERT_WINDOW
    }

    fun kitIsGranted(context: Context, p: LoanAllow): Boolean = when {
        p == LoanAllow.OVERLAY -> Settings.canDrawOverlays(context)
        else -> kitManifestName(p)?.let { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED } ?: true
    }

    /** False when granted, not needed here, or not declared in the manifest (asking would be auto-denied). */
    fun kitNeedsAsking(context: Context, p: LoanAllow): Boolean {
        val name = kitManifestName(p) ?: return false
        return !kitIsGranted(context, p) && name in declared(context)
    }

    /** `permissions[screenKey]` still to ask on this device. */
    fun pending(context: Context, screenKey: String): List<LoanAllow> =
        LoanSpecHolder.current.value.permissions[screenKey].orEmpty().filter { kitNeedsAsking(context, it) }

    /**
     * Asks `permissions[screenKey]` one at a time in list order. Granted / not needed ones are skipped; a denial just moves
     * on. [onDone] once the list is done.
     */
    fun queryFor(activity: ComponentActivity, screenKey: String, onDone: () -> Unit = {}) {
        val list = LoanSpecHolder.current.value.permissions[screenKey].orEmpty()
        query2(activity, list, 0, onDone)
    }

    /** Asks just [p] (a row's Allow); [onDone] after the answer, or at once when it needs no asking. */
    fun query2(activity: ComponentActivity, p: LoanAllow, onDone: () -> Unit) = query2(activity, listOf(p), 0, onDone)

    private fun query2(activity: ComponentActivity, list: List<LoanAllow>, from: Int, onDone: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        val i = (from until list.size).firstOrNull { idx ->
            kitNeedsAsking(activity, list[idx]).also { needed -> if (!needed && kitIsGranted(activity, list[idx])) MetricsTwo.permission(list[idx].key, "already") }
        } ?: return onDone()
        val p = list[i]
        asking = true
        MetricsTwo.permission(p.key, "show")
        val answered = { granted: Boolean ->
            asking = false
            MetricsTwo.permission(p.key, if (granted) "allow" else "deny")
            query2(activity, list, i + 1, onDone)
        }
        // Registered on demand (any time), unregistered once answered.
        val key = "perm_${p.key}_${System.nanoTime()}"
        if (p == LoanAllow.OVERLAY) {
            lateinit var launcher: androidx.activity.result.ActivityResultLauncher<Intent>
            launcher = activity.activityResultRegistry.register(key, ActivityResultContracts.StartActivityForResult()) {
                launcher.unregister(); answered(kitIsGranted(activity, p))
            }
            launcher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${activity.packageName}".toUri()))
        } else {
            lateinit var launcher: androidx.activity.result.ActivityResultLauncher<String>
            launcher = activity.activityResultRegistry.register(key, ActivityResultContracts.RequestPermission()) { granted ->
                launcher.unregister(); answered(granted)
            }
            launcher.launch(kitManifestName(p)!!)
        }
    }

    // The manifest never changes at runtime: one package-manager call, not one per permission check.
    @Volatile private var declaredCache: List<String>? = null

    private fun declared(context: Context): List<String> = declaredCache
        ?: context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions?.toList().orEmpty()
            .also { declaredCache = it }
}
