package com.loanmate.loan.emicalculator.carloan.homelloan.store.cloud.flags

import com.loanmate.loan.emicalculator.carloan.adkit.setup.PromosSpec
import com.loanmate.loan.emicalculator.carloan.homepanel.gateway.StarterConfig

/** The resolved Remote Config for this user (audience + country already applied). Defaults = documented missing-key defaults. */
data class LoanSpec(
    val track: Follow = Follow.A,
    val base: BaseConfig = BaseConfig(),
    val ads: PromosSpec = PromosSpec(),
    val onboarding: OnboardSpec = OnboardSpec(),
    /** ScreenTwo name ("permission_hub", "home", …) → permissions asked there, in order. */
    val permissions: Map<String, List<LoanAllow>> = emptyMap(),
    val launcher: StarterConfig = StarterConfig(),
    val systemKeys: SysShortcuts = SysShortcuts(),
    val uninstallFlow: ExitPathSpec = ExitPathSpec(),
    val prompts: Asks = Asks(),
    val settingsRows: SettingsLines = SettingsLines(),
)

/** track_a = organic, track_b = paid. */
enum class Follow(val key: String) { A("track_a"), B("track_b") }

data class BaseConfig(
    val privacyLink: String = "",
    val termsLink: String = "",
    val storeLink: String = "",
    val updateLink: String = "",
    val appUpdateShow: Boolean = false,
    val appUpdateForce: Boolean = false,
    val splashWaitSec: Int = 4,
    /** `no_internet_dialog`: offline, the app's screens show a "No internet" dialog until the connection is back. */
    val noInternetDialog: Boolean = true,
    val attribution: OriginPlan = OriginPlan(),
    val exclusiveLocations: UniqueAreasPlan = UniqueAreasPlan(),
    /** `rc_sync_hours`: Remote Config syncs at most once per this many hours, also while the app keeps running; 0 = on every app start only. */
    val rcSyncHours: Int = 1,
)

/** `base.exclusive_locations`: country / state / city names or codes whose users always get track_a; see [UniqueAreas]. */
data class UniqueAreasPlan(
    /** false = no lookup and no override. */
    val on: Boolean = false,
    val endpoint: String = "",
    val list: List<String> = emptyList(),
)

/** `base.attribution`: how a new install is classed organic (track_a) or paid (track_b); see [ShipOrigin]. */
data class OriginPlan(
    val gclidMinLength: Int = 20,
    /** gclid values used by Google's test / review installs: always organic. */
    val fakeGclids: List<String> = emptyList(),
    val googleIpCheck: Boolean = false,
    val ipEndpoint: String = "",
    val ipField: String = "asn_org",
    /** [ipField] values of Google's own network: such an install is a review / test device, so organic. */
    val ipMatch: List<String> = emptyList(),
)

data class OnboardSpec(
    /** Code default for a first launch that never reached Remote Config: every loan step, so onboarding isn't skipped. */
    val order: List<OnboardScreen> = OnboardScreen.entries.filter { it !in OnboardScreen.EXTRA },
    val skipButton: Boolean = false,
    val defaultHomeSkipButton: Boolean = false,
    /** `default_home.auto_open_settings_sec`: after a cancelled request, seconds before the Settings page opens again by itself; 0 = never. */
    val defaultHomeAutoOpenSettingsSec: Int = 3,
)

/** Onboarding screen names exactly as they appear in Remote Config (loan steps + the launcher step). */
enum class OnboardScreen(val key: String) {
    // The loan questions, in the design's order. OB_2 / OB_3 keep gender / age (Settings shows those answers).
    LANGUAGE("OB_1"), CURRENCY("OB_4"), AMOUNT("OB_5"), LOAN_TYPE("OB_6"), REPAYMENT("OB_7"),
    GENDER("OB_2"), AGE("OB_3"), EMPLOYMENT("OB_8"), EXISTING_EMI("OB_9"),

    /** Not in the default order: a welcome screen after the questions. */
    WELCOME("welcome"),

    /** Not in the default order: asks `permissions.permission_hub` on Allow. */
    PERMISSION_HUB("permission_hub"),

    /** The launcher step (set as default home). */
    DEFAULT_HOME("default_home");

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }

        /** Steps outside the default order (as the reference app): only Remote Config's `onboarding.order` adds them. */
        val EXTRA = setOf(WELCOME, PERMISSION_HUB, DEFAULT_HOME)
    }
}

enum class LoanAllow(val key: String) {
    CAMERA("camera"), PHOTOS("photos"), NOTIFICATION("notification"),
    CONTACTS("contacts"), PHONE("phone"), OVERLAY("overlay");

    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } }
}

/** [DEFAULT] (`""`): the key does what Android normally does; onboarding doesn't react. */
enum class ShortcutJob(val key: String) { NEXT("next"), BACK("back"), RESET("reset"), DEFAULT("") }

/** Keys that can steer onboarding (`system_keys`). Recents' first press stays the Play Store jump (`launcher.play_store_return`). */
enum class SysShortcut(val key: String) { HOME("home"), BACK("back"), RECENTS("recents") }

data class SysShortcuts(
    val home: ShortcutJob = ShortcutJob.NEXT,
    val back: ShortcutJob = ShortcutJob.NEXT,
    /** null (key missing): Recents does nothing and the screen stays, as before the key existed. */
    val recents: ShortcutJob? = null,
    val sheetOn: Boolean = false,
    val sheetKeys: Set<SysShortcut> = emptySet(),
) {
    /** True when [key] opens the onboarding bottom sheet (`system_keys.bottom_sheet`) before doing its action. */
    fun opensDock(key: SysShortcut) = sheetOn && key in sheetKeys
    fun taskFor(key: SysShortcut): ShortcutJob? = when (key) { SysShortcut.HOME -> home; SysShortcut.BACK -> back; SysShortcut.RECENTS -> recents }
}

/**
 * `uninstall_flow`. [premium] (`flow: "premium"`): the long-press menu on our icon offers Uninstall, which opens our flow —
 * paid: splash → language → steps, ends by hiding our icon; organic: starts on step 1, ends on LoanTwo info.
 * Normal (`flow: "normal"`, or any other value): the menu offers only Hide.
 */
data class ExitPathSpec(
    val premium: Boolean = false,
    /** `system_dialog` (premium only): Uninstall opens an Android-style uninstall dialog instead of the steps. */
    val systemDialog: Boolean = false,
    val splashMs: Long = 3000,
    val progressMs: Long = 4000,
)

data class Asks(val rateAfterOpens: Int = 10, val rateRetryAfterHours: Int = 24)

/** `settings`: which About rows Settings shows (per track, so organic and paid users can differ; countries can overlay it). */
data class SettingsLines(val share: Boolean = true, val rate: Boolean = true, val feedback: Boolean = true, val update: Boolean = true)
