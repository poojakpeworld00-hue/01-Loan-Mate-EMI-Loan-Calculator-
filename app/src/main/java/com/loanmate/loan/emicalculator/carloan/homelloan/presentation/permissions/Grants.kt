package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.permissions

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
import com.loanmate.loan.emicalculator.carloan.homelloan.ads.analytics.Stats2
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.ToolProfileKeeper
import com.loanmate.loan.emicalculator.carloan.homelloan.data.remote.config.ToolGrant

/** Icon and name shown for [this] in the Home permission sheet. */
val ToolGrant.display: Pair<Int, Int>
    get() = when (this) {
        ToolGrant.NOTIFICATION -> R.drawable.ic_bell to R.string.ph_notif
        // FFF declares only the notification permission; the others are never pending, so they share a generic row.
        else -> R.drawable.ic_shield to R.string.perm_title
    }

/** Remote Config `permissions.<screen>`: which runtime permissions a screen asks, in order (reference app rules). */
object Grants {
    /** A permission prompt (or the overlay settings page) is in front: the screen visit goes on. */
    @Volatile var asking = false

    /** The runtime permission behind [p], or null when none is needed on this device (photos use the system picker). */
    fun kitManifestName(p: ToolGrant): String? = when (p) {
        ToolGrant.CAMERA -> Manifest.permission.CAMERA
        ToolGrant.PHOTOS -> null
        ToolGrant.NOTIFICATION -> if (Build.VERSION.SDK_INT >= 33) Manifest.permission.POST_NOTIFICATIONS else null
        ToolGrant.CONTACTS -> Manifest.permission.READ_CONTACTS
        ToolGrant.PHONE -> Manifest.permission.READ_PHONE_STATE
        ToolGrant.OVERLAY -> Manifest.permission.SYSTEM_ALERT_WINDOW
    }

    fun kitIsGranted(context: Context, p: ToolGrant): Boolean = when {
        p == ToolGrant.OVERLAY -> Settings.canDrawOverlays(context)
        else -> kitManifestName(p)?.let { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED } ?: true
    }

    /** False when granted, not needed here, or not declared in the manifest (asking would be auto-denied). */
    fun kitNeedsAsking(context: Context, p: ToolGrant): Boolean {
        val name = kitManifestName(p) ?: return false
        return !kitIsGranted(context, p) && name in declared(context)
    }

    /** `permissions[screenKey]` still to ask on this device. */
    fun pending(context: Context, screenKey: String): List<ToolGrant> =
        ToolProfileKeeper.current.value.permissions[screenKey].orEmpty().filter { kitNeedsAsking(context, it) }

    /**
     * Asks `permissions[screenKey]` one at a time in list order. Granted / not needed ones are skipped; a denial just moves
     * on. [onDone] once the list is done.
     */
    fun queryFor(activity: ComponentActivity, screenKey: String, onDone: () -> Unit = {}) {
        val list = ToolProfileKeeper.current.value.permissions[screenKey].orEmpty()
        query2(activity, list, 0, onDone)
    }

    /** Asks just [p] (a row's Allow); [onDone] after the answer, or at once when it needs no asking. */
    fun query2(activity: ComponentActivity, p: ToolGrant, onDone: () -> Unit) = query2(activity, listOf(p), 0, onDone)

    private fun query2(activity: ComponentActivity, list: List<ToolGrant>, from: Int, onDone: () -> Unit) {
        if (activity.isFinishing || activity.isDestroyed) return
        val i = (from until list.size).firstOrNull { idx ->
            kitNeedsAsking(activity, list[idx]).also { needed -> if (!needed && kitIsGranted(activity, list[idx])) Stats2.permission(list[idx].key, "already") }
        } ?: return onDone()
        val p = list[i]
        asking = true
        Stats2.permission(p.key, "show")
        val answered = { granted: Boolean ->
            asking = false
            Stats2.permission(p.key, if (granted) "allow" else "deny")
            query2(activity, list, i + 1, onDone)
        }
        // Registered on demand (any time), unregistered once answered.
        val key = "perm_${p.key}_${System.nanoTime()}"
        if (p == ToolGrant.OVERLAY) {
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
