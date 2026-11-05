package com.loanmate.loan.emicalculator.carloan.homepanel.theming

import android.content.Context
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.launcherConfig
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.Config
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.IconMaker
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.MAX_COLUMN_COUNT
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.MAX_DOCK_SLOTS
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.MAX_ROW_COUNT
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.MIN_COLUMN_COUNT
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.MIN_ROW_COUNT
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import org.json.JSONObject
import timber.log.Timber

object DeskThemeSetter {
    private const val APPLIED_NONE = "{}"

    fun renewGlyphSizes(context: Context) {
        val config = context.launcherConfig
        val json = config.homeProfileJson.takeIf { it.isNotEmpty() && it != APPLIED_NONE } ?: return
        runCatching {
            val saved = DeskTheme.fromJson(JSONObject(json), source = ThemeRoot.STANDARD) ?: return

            val row = CenterSeal.stored(context)?.let { fp ->
                val remote = DeskThemeRules.parse(Launcher.settings().homeProfileRulesJson, ThemeRoot.RC_RULE)
                (remote?.select(fp) ?: DeskThemeRules.builtIn.select(fp))
            }?.takeIf { it.id == saved.id }
            val stored = row?.let {
                saved.copy(
                    iconWidthFraction = it.iconWidthFraction,
                    iconWidthFractionByDpi = it.iconWidthFractionByDpi,
                    dockIconWidthFraction = it.dockIconWidthFraction,
                    drawerIconWidthFraction = it.drawerIconWidthFraction,
                    iconInset = it.iconInset,
                )
            } ?: saved
            if (stored.iconWidthFraction <= 0f && stored.dockIconWidthFraction <= 0f && stored.drawerIconWidthFraction <= 0f) return
            val metrics = context.resources.displayMetrics
            val widthDp = (minOf(metrics.widthPixels, metrics.heightPixels) / metrics.density).toInt()
            val profile = stored.settleFractions(widthDp, metrics.densityDpi)
            if (config.iconSizeDp != profile.homeArtDp) config.iconSizeDp = profile.homeArtDp
            if (config.dockIconSizeDp != profile.dockArtDp) config.dockIconSizeDp = profile.dockArtDp
            if (config.drawerIconSizeDp != profile.drawerArtDp) config.drawerIconSizeDp = profile.drawerArtDp
        }.onFailure { Timber.w(it, "HomeProfile: icon refresh failed") }
    }

    const val STYLE_OS = "os"
    const val STYLE_DEFAULT = "default"
    const val DEFAULT_ICON_DP = 62

    val DEFAULT_SETUP: DeskTheme = DeskTheme.AOSP.copy(
        id = STYLE_DEFAULT,
        homeCols = 4,
        homeRows = 6,
        dockSize = 4,
        drawerCols = 4,
        iconDp = DEFAULT_ICON_DP,
        dockIconDp = DEFAULT_ICON_DP,
        drawerIconDp = DEFAULT_ICON_DP,
        rackLookupPoint = SheetFindSpot.BOTTOM,
    )

    fun styleFor(osStyle: Boolean): String = if (osStyle) STYLE_OS else STYLE_DEFAULT

    fun commitStyle(context: Context): Boolean {
        val config = context.launcherConfig
        val want = styleFor(Launcher.settings().osStyle)
        val built = config.wasHomeScreenInit || config.homeProfileJson.isNotEmpty()

        val have = config.launcherStyle.ifEmpty { if (built) STYLE_OS else "" }
        if (have == want) {
            if (config.launcherStyle.isEmpty()) config.launcherStyle = have
            return want == STYLE_DEFAULT && renewMain(config)
        }

        if (want == STYLE_OS) {
            val profile = settleSys(context)
            if (profile == null) {
                config.homeProfileJson = APPLIED_NONE
                config.homeAndDrawer = false
            } else {
                write(config, profile, homeAndDrawer = profile.rackStyle != SheetLook.NONE &&
                    profile.baseOf(DeskTheme.KEY_DRAWER_MODE) == ThemeRoot.APK)
            }
        } else {
            write(config, DEFAULT_SETUP, homeAndDrawer = true)
        }
        config.launcherStyle = want
        Timber.i("HomeProfile: launcher style $have -> $want")
        if (!built) return false
        config.homeSeeded = false
        config.homeRebuildPending = true
        return true
    }

    private fun renewMain(config: Config): Boolean {
        val json = DEFAULT_SETUP.toJson().toString()
        if (config.homeProfileJson == json) return false
        val saved = runCatching { DeskTheme.fromJson(JSONObject(config.homeProfileJson), source = ThemeRoot.STANDARD) }.getOrNull()
        write(config, DEFAULT_SETUP, homeAndDrawer = true)
        val reshaped = saved == null || saved.homeCols != DEFAULT_SETUP.homeCols ||
            saved.homeRows != DEFAULT_SETUP.homeRows || saved.dockSize != DEFAULT_SETUP.dockSize
        if (reshaped) {
            config.homeSeeded = false
            config.homeRebuildPending = true
        }
        return true
    }

    private fun settleSys(context: Context): DeskTheme? {
        return runCatching {
            val fp = CenterSeal.stored(context)?.takeIf { it.homePackage.isNotEmpty() }
                ?: CenterSeal.grabAndStash(context)
            DeskThemeFinder.resolve(context, fp)
        }.onFailure { Timber.w(it, "HomeProfile: resolve failed, keeping defaults") }.getOrNull()
    }

    private fun write(config: Config, profile: DeskTheme, homeAndDrawer: Boolean) {
        config.homeRowCount = (profile.homeRows + 1).coerceIn(MIN_ROW_COUNT, MAX_ROW_COUNT)
        config.homeColumnCount = profile.homeCols.coerceIn(MIN_COLUMN_COUNT, MAX_COLUMN_COUNT)
        config.drawerColumnCount = profile.drawerCols.coerceIn(MIN_COLUMN_COUNT, MAX_COLUMN_COUNT)
        config.showSearchBar = profile.drawerSearch
        config.showHomeAppLabels = profile.homeLabels
        config.showDrawerAppLabels = profile.drawerLabels
        config.rackStyle = profile.rackStyle
        if (profile.rackStyle != SheetLook.NONE) config.drawerStyleWhenOn = profile.rackStyle
        if (profile.drawerRows > 0) config.drawerRowCount = profile.drawerRows.coerceIn(MIN_ROW_COUNT, MAX_ROW_COUNT)

        config.dockColumnCount = profile.dockSize.coerceIn(1, MAX_DOCK_SLOTS)
        config.drawerSearchAtBottom = profile.rackLookupPoint == SheetFindSpot.BOTTOM
        config.iconScale = profile.iconScale

        config.iconSizeDp = profile.homeArtDp
        config.dockIconSizeDp = profile.dockArtDp
        config.drawerIconSizeDp = profile.drawerArtDp
        config.labelTextSp = profile.labelSp
        config.legacyIconTray = IconMaker.Tray.parse(profile.legacyIconTray) ?: IconMaker.Tray.DOMINANT
        config.homeAndDrawer = homeAndDrawer
        config.homeProfileJson = profile.toJson().toString()
        Timber.i("HubPersona: applied ${profile.id} — home ${profile.homeCols}x${profile.homeRows}+dock, drawer ${profile.drawerCols} cols")
    }
}
