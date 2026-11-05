package com.loanmate.loan.emicalculator.carloan.adkit.setup

/**
 * Typed form of the Remote Config `ads` block plus `base.ad_network` / `base.ad_units`.
 * KitField names follow the JSON keys; defaults are the documented "missing key" defaults.
 * The app's config layer builds this; the ad engine only reads it.
 */
data class PromosSpec(
    val on: Boolean = false,
    val loaderWaitSec: Int = 8,
    /** An exit_ad step whose ad is not cached: true = load it now behind "Please wait" (reference), false = skip it. */
    val loaderOnMiss: Boolean = true,
    val preload: PackShapeBits = PackShapeBits(inter = true, native = true, appOpen = true, rewarded = true),
    val formatsOn: PackShapesOn = PackShapesOn(),
    val ifAdNotReady: List<BackupStep> = emptyList(),
    val links: List<String> = emptyList(),
    val linksCount: Int = 0,
    val showAll: Boolean = true,
    val linksLaunchIn: UrlsJoinIn = UrlsJoinIn.IN_APP_BROWSER,
    val customAds: TailoredPromos = TailoredPromos(),
    val colors: PromoTints = PromoTints(),
    val network: PromoNet = PromoNet.GOOGLE,
    val units: PromoPieces = PromoPieces(),
    /** Every ad place, keyed "<group>.<name>": "screens.home", "onboarding.language", "launcher.app_resume", "uninstall_flow.splash". */
    val places: Map<String, PromoSlot> = emptyMap(),
) {
    fun place(group: String, name: String): PromoSlot = places["$group.$name"] ?: PromoSlot()

    /** Links an exit ad opens, after its block overrides: [ExitPromo.links] / [ExitPromo.linksCount] win over the defaults. */
    fun kitUrlsFor(exit: ExitPromo): List<String> {
        val urls = exit.links ?: links
        val count = exit.linksCount ?: linksCount
        if (urls.isEmpty() || count <= 0) return emptyList()
        return List(count) { urls[it % urls.size] }
    }

    fun presentEveryFor(exit: ExitPromo): Boolean = exit.showAll ?: showAll
}

data class PackShapeBits(
    val inter: Boolean = false,
    val native: Boolean = false,
    val appOpen: Boolean = false,
    val rewarded: Boolean = false,
)

data class PackShapesOn(
    val inter: Boolean = true,
    val native: Boolean = true,
    val banner: Boolean = true,
    val nativeBanner: Boolean = true,
    val appOpen: Boolean = true,
    val rewarded: Boolean = true,
)

/** `links_open_in`: our own page (webview), a Chrome Custom Tab over the app (in_app_browser), or the phone's browser app (browser). */
enum class UrlsJoinIn(val key: String) { WEBVIEW("webview"), IN_APP_BROWSER("in_app_browser"), BROWSER("browser") }

enum class PromoNet(val key: String) { GOOGLE("google"), CUSTOM("custom") }

data class PromoPieces(
    val inter: String = "",
    val interSplash: String = "",
    val interBack: String = "",
    val appOpen: String = "",
    val native: String = "",
    val banner: String = "",
    val rewarded: String = "",
) {
    fun interForStart() = interSplash.ifBlank { inter }
    fun interForPrior() = interBack.ifBlank { inter }
}

data class TailoredPromos(val on: Boolean = false, val list: List<TailoredPromoTile> = emptyList())

data class TailoredPromoTile(
    val icon: String = "",
    val title: String = "",
    val description: String = "",
    val bannerImage: String = "",
    val buttonText: String = "",
)

data class PromoTints(val light: PromoSwatch = PromoSwatch(), val dark: PromoSwatch = PromoSwatch())

/** Hex colours (#RRGGBB); blank = the app's built-in colour. */
data class PromoSwatch(
    val button: String = "",
    val buttonText: String = "",
    val background: String = "",
    val text: String = "",
)

/** One ad place: any of the blocks it has, plus place-specific options (on, gap_sec, window_sec, every, list, …) as raw JSON values. */
data class PromoSlot(
    val bottomAd: PackBay? = null,
    val listAd: PackBay? = null,
    val exitAd: ExitPromo? = null,
    val options: Map<String, Any?> = emptyMap(),
) {
    fun bool(key: String, default: Boolean = false) = options[key] as? Boolean ?: default
    fun int(key: String, default: Int = 0) = (options[key] as? Number)?.toInt() ?: default
}

/** [onFail]: the type shown instead when a banner slot's banner fails to load (RC `on_fail`, e.g. native_banner). */
data class PackBay(val on: Boolean = false, val type: BayOrder = BayOrder.BANNER, val onFail: BayOrder? = null)

enum class BayOrder(val key: String) {
    BANNER("banner"),
    BANNER_INLINE("banner_inline"),
    BANNER_COLLAPSIBLE("banner_collapsible"),
    NATIVE_BIG("native_big"),
    NATIVE_MID("native_mid"),
    NATIVE_MID2("native_mid2"),
    NATIVE_BANNER("native_banner"),
    NATIVE_FULL("native_full");

    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } }
}

data class ExitPromo(
    val on: Boolean = false,
    val ads: List<PromoStep> = emptyList(),
    /** null = use [PromosSpec.linksCount]. */
    val linksCount: Int? = null,
    /** null = use [PromosSpec.links]. */
    val links: List<String>? = null,
    /** null = use [PromosSpec.showAll]. */
    val showAll: Boolean? = null,
    /** 1 = every time, 2 = every 2nd time, … */
    val every: Int = 1,
)

/** [LINK] is not an ad: it marks where the block's links open in the sequence (no `link` = links open first). */
enum class PromoStep(val key: String) {
    INTER("inter"), REWARDED("rewarded"), APP_OPEN("app_open"), FULL_NATIVE("full_native"), CUSTOM("custom"), LINK("link");

    companion object { fun of(key: String?) = entries.firstOrNull { it.key == key } }
}

/** One item of `if_ad_not_ready`: an ad, the word `link` (index 0 of the block's links, else ads.links), or a URL. */
sealed interface BackupStep {
    data class Spot(val step: PromoStep) : BackupStep
    data object LaunchUrl : BackupStep
    data class PackLink(val url: String) : BackupStep
}
