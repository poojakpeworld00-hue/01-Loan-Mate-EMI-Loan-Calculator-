package com.fffskintools.skin.tools.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.widget.FrameLayout
import com.fffskintools.skin.tools.ads.events.charging.JuiceCueMonitor
import com.fffskintools.skin.tools.ads.events.pkgresult.ParcelCueMonitor
import com.fffskintools.skin.tools.ads.events.recent.RecentSpotMonitor
import com.fffskintools.skin.tools.ads.host.SpotPreload
import com.fffskintools.skin.tools.ads.host.kitOnceWindowFocused
import com.fffskintools.skin.tools.ads.launcher.OpenSpotMonitor
import com.fffskintools.skin.tools.ads.render.ProceedSpotLeader

/**
 * The one entry point for every ad kind. Each method takes the JSON place key ("<group>.<name>"), applies the
 * RC rules and calls the ported engine; callbacks run exactly once. `ads.on` false or a missing / off place →
 * straight through. Views never call the engine directly.
 */
object Spots {
    const val SPLASH = "screens.splash"
    const val BACK_BUTTON = "launcher.back_button"
    const val APP_RESUME = "launcher.app_resume"
    const val APP_OPEN = "launcher.app_open"
    const val APP_EXIT = "launcher.app_exit"
    const val APP_DRAWER = "launcher.app_drawer"
    const val SPONSORED_APP_OPEN = "launcher.sponsored_app_open"
    const val SPONSORED_APP_EXIT = "launcher.sponsored_app_exit"
    const val PHONE_UNLOCK = "launcher.phone_unlock"
    const val CHARGER_CONNECTED = "launcher.charger_connected"
    const val CHARGER_DISCONNECTED = "launcher.charger_disconnected"
    const val APP_INSTALLED = "launcher.app_installed"
    const val APP_UNINSTALLED = "launcher.app_uninstalled"
    const val PLAY_STORE_RETURN = "launcher.play_store_return"

    private enum class KitReturn { NONE, APP, SPONSORED }

    private var pendingReturn = KitReturn.NONE
    private var returnHandledAt = 0L

    // ---- screens -------------------------------------------------------------------------------------------

    /** Any `exit_ad` (screens, onboarding, uninstall_flow, launcher places). */
    fun exit(activity: Activity, placeKey: String, onDone: () -> Unit) = OutSpotDriver.run(activity, placeKey, onDone)

    /** The place's `bottom_ad` slot. */
    fun bottom(activity: Activity, container: FrameLayout, placeKey: String) {
        slots[container] = { bottom(activity, container, placeKey) }
        BottomSpotPuller.load(activity, container, SpotKit.config.places[placeKey]?.bottomAd ?: OFF, placeKey)
    }

    /** The place's `list_ad` will show: ads are on and the slot is on. */
    fun rosterOn(placeKey: String): Boolean = SpotKit.spotsOn() && SpotKit.config.places[placeKey]?.listAd?.on == true

    /** The place's `list_ad` slot (suggested_panel). */
    fun list(activity: Activity, container: FrameLayout, placeKey: String) {
        slots[container] = { list(activity, container, placeKey) }
        BottomSpotPuller.load(activity, container, SpotKit.config.places[placeKey]?.listAd ?: OFF, placeKey)
    }

    /** Every bottom / list slot asked for, with how to load it again (weak: a gone screen's slots drop out). */
    private val slots = java.util.WeakHashMap<FrameLayout, () -> Unit>()

    /** Back online: [activity]'s slots that came up empty (offline) load again; filled or closed ones are left alone. */
    fun kitRefillEmptySlots(activity: Activity) {
        slots.entries.toList().forEach { (slot, load) ->
            if (slot.context === activity && slot.isAttachedToWindow && slot.childCount == 0 &&
                com.fffskintools.skin.tools.ads.render.SpotSlotBarrier.isOpen(slot)) load()
        }
    }

    /** `screens.splash` exit_ad (inter uses `ad_units.inter_splash`); never waits past loader_wait_sec per ad. */
    fun splash(activity: Activity, onDone: () -> Unit) = exit(activity, SPLASH, onDone)

    /** `launcher.back_button` (inter uses `ad_units.inter_back`). */
    fun backButton(activity: Activity, onDone: () -> Unit) = exit(activity, BACK_BUTTON, onDone)

    /** `launcher.back_button` / `home_button` / `recents_button` with `during_onboarding: true`: its ad also runs during onboarding. */
    fun hotkeySpotDuringSetup(key: String): Boolean =
        outOn("launcher.${key}_button") && SpotKit.config.places["launcher.${key}_button"]?.bool("during_onboarding") == true

    /** `launcher.recents_button` is on (after onboarding: Recents on our screen shows it). */
    fun switcherSpotOn(): Boolean = outOn("launcher.recents_button")

    /** `launcher.home_button` / `launcher.recents_button`; [key] = "home" | "recents". */
    fun systemKey(activity: Activity, key: String, onDone: () -> Unit) = exit(activity, "launcher.${key}_button", onDone)

    fun preload(activity: Activity) = SpotPreload.all(activity)

    // ---- app resume ----------------------------------------------------------------------------------------

    /**
     * `launcher.app_resume`: called by the module when the app comes to the foreground. Skipped after our own
     * launches ([kitMarkOwnLaunch], links), while a full-screen ad shows, on module screens and [SpotKit.noResumeAdOn],
     * and when `launcher.app_exit` / `sponsored_app_exit` runs for the same return.
     */
    fun toolProceed(activity: Activity) {
        val pending = pendingReturn
        pendingReturn = KitReturn.NONE
        val ownReturn = ProceedSpotLeader.skipNextAppOpenAd
        ProceedSpotLeader.skipNextAppOpenAd = false
        // A return from a launched app / sponsored tile has its own place, even when its launch marked it as ours.
        if (ownReturn && pending == KitReturn.NONE) return
        if (SystemClock.elapsedRealtime() - returnHandledAt < RETURN_HANDLED_MS) return
        if (isModuleView(activity) || SpotKit.noResumeAdOn(activity) || OutSpotDriver.busy()) return
        activity.kitOnceWindowFocused {
            if (activity.isFinishing || activity.isDestroyed) return@kitOnceWindowFocused
            when {
                pending == KitReturn.APP && outOn(APP_EXIT) -> exit(activity, APP_EXIT) {}
                pending == KitReturn.SPONSORED && outOn(SPONSORED_APP_EXIT) -> exit(activity, SPONSORED_APP_EXIT) {}
                OpenSpotMonitor.claimsProceed(activity) -> Unit // the unlock ad (inter → rewarded) shows instead
                else -> exit(activity, APP_RESUME) {}
            }
        }
    }

    /** The app is about to leave for something of its own (camera, share, settings…): no resume ad on the return. */
    fun kitMarkOwnLaunch() {
        ProceedSpotLeader.skipNextAppOpenAd = true
    }

    /** Undo [kitMarkOwnLaunch] when the launch did not happen. */
    fun resetOwnLaunch() {
        ProceedSpotLeader.skipNextAppOpenAd = false
    }

    // ---- launcher moments ----------------------------------------------------------------------------------

    /** [panel] = "host_app_panel" | "suggested_panel". */
    fun boardOpened(activity: Activity, panel: String, proceed: () -> Unit) = exit(activity, "launcher.$panel", proceed)

    fun chestOpened(activity: Activity, proceed: () -> Unit) = exit(activity, APP_DRAWER, proceed)

    /** User tapped an app icon: `launcher.app_open`, then [proceed] opens it; the return runs `app_exit`. */
    fun toolBadgeTapped(activity: Activity, proceed: () -> Unit) = exit(activity, APP_OPEN) {
        pendingReturn = KitReturn.APP
        proceed()
    }

    /** Back from an app opened from the launcher (also detected on its own after [toolBadgeTapped]). */
    fun returnedFromTool(activity: Activity) = kitRunReturn(activity, APP_EXIT)

    /** User tapped a sponsored drawer tile: `launcher.sponsored_app_open`, then [proceed] opens its link. */
    fun paidBlockTapped(activity: Activity, proceed: () -> Unit) = exit(activity, SPONSORED_APP_OPEN) {
        pendingReturn = KitReturn.SPONSORED
        proceed()
    }

    fun returnedFromPaid(activity: Activity) = kitRunReturn(activity, SPONSORED_APP_EXIT)

    private fun kitRunReturn(activity: Activity, placeKey: String) {
        pendingReturn = KitReturn.NONE
        if (outOn(placeKey)) returnHandledAt = SystemClock.elapsedRealtime()
        activity.kitOnceWindowFocused { exit(activity, placeKey) {} }
    }

    // ---- system events -------------------------------------------------------------------------------------

    /** `launcher.phone_unlock`: on, start_after_hours, gap_minutes, max_per_day; shown on [SpotKit.isHomeScreen]. */
    fun handsetUnlocked(context: Context) = OpenSpotMonitor.onOpen2(context.applicationContext)

    /** `launcher.charger_connected` / `charger_disconnected`: on, gap_sec → battery screen (bottom_ad, exit_ad on close). */
    fun kitChargerChanged(context: Context, connected: Boolean) =
        JuiceCueMonitor.onEvent(context.applicationContext, if (connected) CHARGER_CONNECTED else CHARGER_DISCONNECTED)

    /** `launcher.app_installed` / `app_uninstalled`: on, gap_sec → result screen (bottom_ad, exit_ad on close). */
    fun parcelChanged(context: Context, installed: Boolean, packageName: String) =
        ParcelCueMonitor.onEvent(context.applicationContext, installed, packageName)

    /**
     * `launcher.play_store_return`, on a Recents press: within `window_sec` after install the first press opens our
     * Play Store page (`on`); with `show_ad_screen` the user comes back to an ad screen (bottom_ad, exit_ad), `gap_sec` apart.
     */
    fun playKeeperReturn(context: Context) = RecentSpotMonitor.onSwitcher(context.applicationContext)

    // ---- helpers -------------------------------------------------------------------------------------------

    private const val RETURN_HANDLED_MS = 3_000L
    private val OFF = com.fffskintools.skin.tools.ads.config.KitSlot(on = false)

    /** Place is on and ads are on (pure check, no counters touched). */
    internal fun outOn(placeKey: String): Boolean =
        SpotKit.spotsOn() && SpotKit.config.places[placeKey]?.exitAd?.on == true

    internal fun kitPlaceOn(placeKey: String): Boolean =
        SpotKit.spotsOn() && SpotKit.config.places[placeKey]?.bool("on") == true

    /** Module screens (event screens, sponsor page) and SDK ad activities never get the resume ad. */
    private fun isModuleView(activity: Activity): Boolean {
        val name = activity.javaClass.name
        return name.startsWith("com.fffskintools.skin.tools.ads.") || name.startsWith("com.google.android.gms.ads.")
    }
}
