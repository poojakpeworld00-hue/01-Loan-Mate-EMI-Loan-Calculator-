package com.loanmate.loan.emicalculator.carloan.homepanel.gateway

import android.app.Activity
import android.app.Application
import android.content.Context
import android.widget.FrameLayout
import androidx.fragment.app.Fragment

/** What the host app supplies to the launcher module. */
interface StarterBridge {
    /** Current launcher settings; read on every launcher resume, so a Remote Config refresh applies without a restart. */
    fun settings(): StarterConfig

    /** True once the host's onboarding is finished. Until then the launcher sends HOME presses back to the onboarding screen. */
    fun isOnboardingDone(): Boolean

    /** The host app's own home UI, shown in the right-swipe panel (`host_app`). */
    fun createHostPanel(): Fragment

    /** Opens the host app's onboarding / main screen (used when onboarding isn't done yet). */
    fun openHostApp(activity: Activity)

    /** False: HOME during onboarding shows the launcher home (the device's own behaviour) instead of going back to onboarding. */
    fun takesHome(): Boolean = true
}

enum class StarterTray { HOST_APP, SUGGESTED }

enum class StarterBay { SUGGESTED_BOTTOM, SUGGESTED_LIST, DRAWER }

/**
 * Moments the host can attach ads or analytics to. Every method has a pass-through default,
 * so a host with no ads uses [StarterSignals.None]. `proceed` must be called exactly once.
 */
interface StarterSignals {
    fun onPanelOpened(activity: Activity, panel: StarterTray, proceed: () -> Unit) = proceed()
    fun onDrawerOpened(activity: Activity, proceed: () -> Unit) = proceed()
    fun onAppIconTapped(activity: Activity, packageName: String, proceed: () -> Unit) = proceed()
    /** User came back to launcher home from an app opened from it. */
    fun onReturnedFromApp(activity: Activity) {}
    fun onSponsoredTileTapped(activity: Activity, tile: PartnerProg, proceed: () -> Unit) = proceed()
    fun onReturnedFromSponsored(activity: Activity) {}
    /** Fill an inline ad slot; default hides it. */
    fun bindSlot(activity: Activity, slot: StarterBay, container: FrameLayout) { container.removeAllViews(); container.visibility = android.view.View.GONE }
    /** User chose to uninstall the host app from the launcher. Return true if the host shows its own uninstall flow. */
    fun onUninstallHostApp(activity: Activity): Boolean = false
    /** User chose Hide on the host app's own icon. Return true if the host hid it (home and drawer). */
    fun onHideHostApp(activity: Activity): Boolean = false

    // ---- Analytics (the host sends the events; see "Launcher App Config & Events", Part 2) ----
    /** A launcher screen appeared: launcher, app_drawer, host_app_panel, suggested_panel, default_home_hint. */
    fun onScreen(name: String) {}
    /** Any tap in the launcher; [button] names what was tapped (never another app's name). */
    fun onClick(button: String, value: String? = null) {}
    /** System key while a launcher screen is in front: back / home / recents. */
    fun onKey(key: String) {}
    /** A step of a launcher flow, e.g. event "default_home", step "settings_open". */
    fun onStep(event: String, step: String) {}

    object None : StarterSignals
}

/** Entry points the launcher module exposes (implemented by the module). */
interface StarterGate {
    fun install(app: Application, host: StarterBridge, events: StarterSignals = StarterSignals.None)
    fun isDefaultHome(context: Context): Boolean
    /** Shows the system "Default home app" chooser with our coach-mark (closes itself after 3s); [onResult] gets true once we hold the HOME role. */
    fun requestDefaultHome(activity: Activity, onResult: (Boolean) -> Unit)
    /** Opens the launcher home screen (end of onboarding). */
    fun openLauncherHome(activity: Activity)
    /** Re-applies settings (grid style, panels, widgets, icons) after a config refresh. */
    fun refresh(context: Context)
    /** The host's screen took a HOME hand-back (onboarding resumed): the launcher's loop guard starts over. */
    fun hostShown()
}
