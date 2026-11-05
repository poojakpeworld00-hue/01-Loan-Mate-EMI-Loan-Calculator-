package com.loanmate.loan.emicalculator.carloan.homepanel.pages

import timber.log.Timber
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.app.WallpaperManager.OnColorsChangedListener
import android.app.admin.DevicePolicyManager
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.util.Log
import android.util.Property
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import com.loanmate.loan.emicalculator.carloan.homepanel.overlays.ItemMovesPanel
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding
import kotlinx.collections.immutable.toImmutableList
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.getContrastColor
import org.fossify.commons.extensions.getProperBackgroundColor
import org.fossify.commons.extensions.hasPermission
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.insetsController
import org.fossify.commons.extensions.onGlobalLayout
import org.fossify.commons.extensions.performHapticFeedback
import org.fossify.commons.extensions.realScreenSize
import org.fossify.commons.extensions.showKeyboard
import org.fossify.commons.extensions.viewBinding
import org.fossify.commons.helpers.DARK_GREY
import org.fossify.commons.helpers.PERMISSION_READ_CONTACTS
import org.fossify.commons.helpers.PERMISSION_READ_SMS
import org.fossify.commons.helpers.ensureBackgroundThread
import org.fossify.commons.helpers.isOreoMr1Plus
import com.loanmate.loan.emicalculator.carloan.homepanel.R
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.HpDisplayBaseBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.HpAllAppsZoneBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.HpGadgetsZoneBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.Launcher
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterLobbyAsRear
import java.lang.ref.WeakReference
import com.loanmate.loan.emicalculator.carloan.homepanel.overlays.RetitleItemPanel
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.launcherConfig
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.getLabel
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.processGridCellOverlayTweaks
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.hiddenIconsDB
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.homeScreenGridItemsDB
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.isPrimaryLaunchpad
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.isDefaultSmsApp
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.fireApplet
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.fireAppletInfo
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.launchersDB
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.supportsDarkCaption
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.deleteApplet
import com.loanmate.loan.emicalculator.carloan.homepanel.sheets.CenterPanel
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.ITEM_TYPE_FOLDER
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.ITEM_TYPE_ICON
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.ITEM_TYPE_SHORTCUT
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.ITEM_TYPE_WIDGET
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.IconStore
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.AppScan
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.PSEUDO_WIDGET_SEARCH
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.REQUEST_ALLOW_BINDING_WIDGET
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.REQUEST_CONFIGURE_WIDGET
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.REQUEST_CREATE_SHORTCUT
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.REQUEST_DEFAULT_SMS
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.PACKAGE_REFRESH_DELAY_MS
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.UNINSTALL_WATCH_MS
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.UNINSTALL_WATCH_STEP_MS
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.UNINSTALL_APP_REQUEST_CODE
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterAdsToggles
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterTourStop
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterDealBroker
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.FlickKeeper
import com.loanmate.loan.emicalculator.carloan.homepanel.theming.DeskWidgetsFiller
import com.loanmate.loan.emicalculator.carloan.homepanel.theming.DeskPlacer
import com.loanmate.loan.emicalculator.carloan.homepanel.theming.DeskGadgetPlacer
import com.loanmate.loan.emicalculator.carloan.homepanel.theming.DeskThemeSetter
import com.loanmate.loan.emicalculator.carloan.homepanel.theming.CenterSeal
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.ItemSettingsKeeper
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetCenter
import com.loanmate.loan.emicalculator.carloan.homepanel.models.MaskedIcon
import com.loanmate.loan.emicalculator.carloan.homepanel.models.DeskViewerGridItem
import com.loanmate.loan.emicalculator.carloan.homepanel.hooks.SafeDialerManagerListener
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class XCenterScene : CenterBaseScene(), FlickKeeper {

    private var mTouchDownX = -1
    private var mTouchDownY = -1
    private var mAllAppsFragmentY = 0
    private var mWidgetsFragmentY = 0
    private var mScreenHeight = 0
    private var mScreenWidth = 0
    private var mMoveGestureThreshold = 0
    private var mIgnoreUpEvent = false
    private var mIgnoreMoveEvents = false
    private var mIgnoreXMoveEvents = false
    private var mIgnoreYMoveEvents = false
    // Where this gesture started and the leftmost / rightmost x it reached, to tell a pull-back
    // from a lift wobble and a quick swipe from a slow drag.
    private var mGestureDownX = 0f
    private var mGestureMinX = 0f
    private var mGestureMaxX = 0f
    private val mTouchSlop by lazy { ViewConfiguration.get(this).scaledTouchSlop }
    // A page swipe faster than this on average (px per ms, ~400dp/s) turns the page on release.
    private val mQuickSwipeSpeed by lazy { 0.4f * resources.displayMetrics.density }

    private var mDrawerEnabled = true
    private var mLongPressedIcon: DeskViewerGridItem? = null
    private var mOpenPopupMenu: ItemMovesPanel? = null
    private var mLastTouchCoords = Pair(-1f, -1f)
    private var mActionOnCanBindWidget: ((granted: Boolean) -> Unit)? = null
    private var mActionOnWidgetConfiguredWidget: ((granted: Boolean) -> Unit)? = null
    private var mActionOnAddShortcut:
            ((shortcutId: String, label: String, icon: Drawable) -> Unit)? = null
    private var mActionOnDefaultSmsResult: (() -> Unit)? = null
    private var wasJustPaused: Boolean = false

    /** Set by onNewIntent once it has closed every surface; consumed by the next onResume. */
    private var mWorkspaceUnwound = false

    private var mSwipeHintAnimator: ObjectAnimator? = null

    private var mOpenSidePanel: View? = null

    /**
     * A panel that `launcher_config.panels` has just switched off must not stay on screen: the
     * gesture gates only stop it opening, so one already open would otherwise be stuck there.
     */
    private fun closeDisabledPanels() {
        if (isLeftStageExpanded() && !CenterAdsToggles.deckEnabled(CenterAdsToggles.LEFT_SWIPE)) {
            concealLeftDeck()
        }
        if (isMsgsStageExpanded() && !CenterAdsToggles.deckEnabled(CenterAdsToggles.RIGHT_SWIPE)) {
            concealMsgsDeck()
        }
    }

    /** [Launcher.refresh]: a Remote Config refresh reached the running launcher. */
    fun onSettingsChanged() {
        if (isFinishing || isDestroyed) {
            return
        }
        if (reapplyHomebaseStyle()) return

        closeDisabledPanels()

        if (!isAllProgramsSectionExpanded() && IconStore.launchers.isNotEmpty()) {
            // Same apps, but the config may have moved rows (the ad row): resubmit regardless.
            binding.allAppsBlockIxVw.root.gotLaunchers(IconStore.launchers, force = true)
            binding.startPageIxVw.root.gotLaunchers(IconStore.launchers)
        }
        // time / search widget and our own icon on home
        ensureBackgroundThread {
            runCatching { renewHomebases() }.onFailure { Timber.w(it, "StarterTray: renewHomebases failed") }
        }
        perhapsSurfaceGuide()
    }

    private var wallpaperColorChangeListener: OnColorsChangedListener? = null
    private var wallpaperSupportsDarkText: Boolean? = null

    private lateinit var mDetector: GestureDetectorCompat
    private val binding by viewBinding(HpDisplayBaseBinding::inflate)

    companion object {
        const val EXTRA_OPEN_MESSAGES = "open_messages"

        private var running: WeakReference<XCenterScene>? = null

        /** The live launcher home, for [Launcher.refresh]. */
        val current: XCenterScene? get() = running?.get()?.takeIf { !it.isFinishing && !it.isDestroyed }

        private const val DOCK_SMS_SLOT = 1
        private const val PLAY_STORE_PACKAGE = "com.android.vending"

        private var mLastUpEvent = 0L
        private const val ANIMATION_DURATION = 150L
        private const val APP_DRAWER_CLOSE_DELAY = 300L
        private const val APP_DRAWER_STATE = "app_drawer_state"
        private const val SWIPE_HINT_ANIMATION_DURATION = 900L

        private const val PERMISSION_SHEET_DELAY = 500L

        private const val INBOX_WARM_UP_DELAY = 250L

        private const val SCREEN_HOME = "launcher"
        private const val SCREEN_DRAWER = "app_drawer"
        private const val SCREEN_HOST_APP = "host_app_panel"
        private const val SCREEN_SUGGESTED = "suggested_panel"

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false

        super.onCreate(savedInstanceState)

        // Host onboarding unfinished (e.g. the HOME role was just granted and the system started us): hand back.
        if (CenterLobbyAsRear.onHome(this)) {
            finish()
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            return
        }
        running = WeakReference(this)

        DeskThemeSetter.commitStyle(this)
        DeskThemeSetter.renewGlyphSizes(this)
        mDrawerEnabled = launcherConfig.isDrawerEnabled

        setContentView(binding.root)

        getSystemService(LauncherApps::class.java)?.registerCallback(packageCallback, Handler(Looper.getMainLooper()))
        setupEdgeToEdge(
            padTopSystem = listOf(
                binding.allAppsBlockIxVw.root,
                binding.widgetsBlockIxVw.root,
                binding.startPageIxVw.root,
                // not msgsPageIx: the host panel fragment applies its own insets
            ),
            padBottomImeAndSystem = listOf(
                binding.allAppsBlockIxVw.allAppsGridIxVw,
                binding.allAppsBlockIxVw.allAppsLeafPipsIxVw,
                binding.widgetsBlockIxVw.widgetsRollIxVw,
                binding.startPageIxVw.pageScrollIxVw,
            ),

            padBottomSystem = listOf(
                binding.mainShowGridIxVw.root,
                binding.startPageIxVw.pageEmbeddedCasingIxVw
            )
        )

        mDetector = GestureDetectorCompat(this, OwnTouchListener(this))

        mScreenHeight = realScreenSize.y
        mScreenWidth = realScreenSize.x
        mAllAppsFragmentY = mScreenHeight
        mWidgetsFragmentY = mScreenHeight
        mMoveGestureThreshold = resources.getDimensionPixelSize(R.dimen.launcher_move_gesture_threshold)

        arrayOf(
            binding.allAppsBlockIxVw.root as CenterPanel<*>,
            binding.widgetsBlockIxVw.root as CenterPanel<*>
        ).forEach { fragment ->
            fragment.setupFragment(this)
            fragment.y = mScreenHeight.toFloat()
            fragment.beVisible()
        }

        binding.startPageIxVw.root.apply {
            setupFragment(this@XCenterScene)
            x = mScreenWidth.toFloat()
            beVisible()
        }

        binding.msgsPageIxVw.root.apply {
            setupFragment(this@XCenterScene)
            x = -mScreenWidth.toFloat()
            beVisible()
        }

        treatIntentMove(intent)

        binding.mainShowGridIxVw.root.itemClickListener = {
            performElementTap(it)
        }

        binding.mainShowGridIxVw.root.itemLongClickListener = {
            performCellLongTap(
                x = binding.mainShowGridIxVw.root.getClickableRect(it).left.toFloat(),
                clickedGridItem = it
            )
        }

        prepWallpaperTintSentry()

        if (savedInstanceState == null) revealMsgsIfRequested(intent)

        binding.baseHolderIxVw.post { perhapsSurfaceGuide() }

        binding.baseHolderIxVw.postDelayed({
            Looper.myQueue().addIdleHandler {
                // A messages panel switched off by launcher_config is not warmed up; if the config
                // turns it on later, opening it builds it.
                if (!isFinishing && !isDestroyed &&
                    CenterAdsToggles.deckEnabled(CenterAdsToggles.RIGHT_SWIPE)
                ) binding.msgsPageIxVw.root.warmUp()
                false
            }
        }, INBOX_WARM_UP_DELAY)
    }

    private var mGuideStep: CenterTourStop? = null

    private fun nextGuidePhase(): CenterTourStop? = CenterTourStop.entries.firstOrNull {
        CenterAdsToggles.guidePhaseEnabled(it) && !launcherConfig.isTutorialPhaseDone(it) &&

                (it != CenterTourStop.SWIPE_UP_DRAWER || mDrawerEnabled)
    }

    private fun perhapsSurfaceGuide() {
        if (isAllProgramsSectionExpanded() || isGadgetsSectionExpanded()) return
        if (isLeftStageExpanded() || isMsgsStageExpanded()) return

        val step = nextGuidePhase()
        if (step == null) {
            concealSwipeHint()
        } else if (mGuideStep != step || !binding.swipeHintIxVw.isVisible) {
            surfaceGuidePhase(step)
        }
    }

    private fun surfaceGuidePhase(step: CenterTourStop) {
        abortGuideTween()
        mGuideStep = step

        val chevrons = listOf(
            binding.swipeHintChevron1IxVw,
            binding.swipeHintChevron2IxVw,
            binding.swipeHintChevron3IxVw,
        )
        val travel = resources.getDimension(R.dimen.launcher_swipe_hint_travel)

        val icon: Int
        val label: Int
        val property: Property<View, Float>
        val distance: Float
        val vertical: Boolean
        when (step) {
            CenterTourStop.SWIPE_RIGHT_MESSAGES -> {
                icon = org.fossify.commons.R.drawable.ic_chevron_right_vector
                label = R.string.launcher_swipe_right_hint
                property = View.TRANSLATION_X
                distance = travel
                vertical = false
            }

            CenterTourStop.SWIPE_LEFT_APPS -> {
                icon = org.fossify.commons.R.drawable.ic_chevron_left_vector
                label = R.string.launcher_swipe_left_hint
                property = View.TRANSLATION_X
                distance = -travel
                vertical = false
            }

            CenterTourStop.SWIPE_UP_DRAWER -> {
                icon = org.fossify.commons.R.drawable.ic_chevron_up_vector
                label = R.string.launcher_swipe_up_hint
                property = View.TRANSLATION_Y
                distance = -travel
                vertical = true
            }
        }

        binding.swipeHintChevronsIxVw.orientation =
            if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL

        val alphas = if (distance < 0f) listOf(1f, 0.6f, 0.3f) else listOf(0.3f, 0.6f, 1f)
        chevrons.forEachIndexed { index, view ->
            view.setImageResource(icon)
            view.alpha = alphas[index]

            view.translationX = 0f
            view.translationY = 0f
        }
        binding.swipeHintChevronsIxVw.translationX = 0f
        binding.swipeHintChevronsIxVw.translationY = 0f
        binding.swipeHintTextIxVw.setText(label)
        binding.swipeHintIxVw.beVisible()

        mSwipeHintAnimator = ObjectAnimator.ofFloat(
            binding.swipeHintChevronsIxVw,
            property,
            0f,
            distance,
        ).apply {
            duration = SWIPE_HINT_ANIMATION_DURATION
            repeatCount = ValueAnimator.INFINITE

            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun doneGuidePhase(step: CenterTourStop) {
        if (launcherConfig.isTutorialPhaseDone(step)) return
        launcherConfig.flagGuidePhaseDone(step)
        if (mGuideStep == step) {
            concealSwipeHint()
        }
    }

    private fun performGuidePhase(): Boolean {
        val step = mGuideStep ?: return false
        if (!binding.swipeHintIxVw.isVisible) return false

        concealSwipeHint()
        Launcher.events.onClick("swipe_guide", step.name.lowercase())

        when (step) {
            CenterTourStop.SWIPE_RIGHT_MESSAGES -> surfaceMsgsDeck()
            CenterTourStop.SWIPE_LEFT_APPS -> surfaceLeftDeck()
            CenterTourStop.SWIPE_UP_DRAWER -> displayFragment(binding.allAppsBlockIxVw)
        }
        return true
    }

    private fun abortGuideTween() {
        mSwipeHintAnimator?.cancel()
        mSwipeHintAnimator = null
    }

    private fun concealSwipeHint() {
        if (!binding.swipeHintIxVw.isVisible) {
            return
        }

        abortGuideTween()
        binding.swipeHintIxVw.beGone()
    }

    private fun prepWallpaperTintSentry() {
        if (isOreoMr1Plus()) {
            val wallpaperManager = WallpaperManager.getInstance(this)
            wallpaperColorChangeListener = OnColorsChangedListener { colors, which ->
                if (which and WallpaperManager.FLAG_SYSTEM != 0) {
                    wallpaperSupportsDarkText = colors?.wantsDarkCaption() ?: run {
                        renewWallpaperSupportsDarkCaption()
                        wallpaperSupportsDarkText
                    }

                    if (livePanelTint() == null) {
                        runOnUiThread {
                            reloadStatusBandGlyphs()
                        }
                    }
                }
            }
            wallpaperManager.addOnColorsChangedListener(
                wallpaperColorChangeListener!!,
                Handler(Looper.getMainLooper())
            )

            renewWallpaperSupportsDarkCaption()
        }
    }

    private fun renewWallpaperSupportsDarkCaption() {
        if (!isOreoMr1Plus()) return
        wallpaperSupportsDarkText = WallpaperManager.getInstance(this)
            .getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
            ?.wantsDarkCaption()
    }

    private fun isNightSkin(): Boolean =
        resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    private fun android.app.WallpaperColors.wantsDarkCaption(): Boolean = supportsDarkCaption()

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME) && CenterLobbyAsRear.onHome(this)) {
            setIntent(intent)
            return
        }
        // Home pressed: the long-press card goes with the rest. It is a window of its own, so
        // closing the drawer under it left it floating over the workspace.
        closeElementActions()

        val wasAnyFragmentOpen = isAllProgramsSectionExpanded() || isGadgetsSectionExpanded()
        if (wasJustPaused) {
            if (isAllProgramsSectionExpanded()) {
                concealFragment(binding.allAppsBlockIxVw)
            }
            if (isGadgetsSectionExpanded()) {
                concealFragment(binding.widgetsBlockIxVw)
            }
        } else {
            closeAppletRack()
            closeWidgetsBlock()
        }

        if (isLeftStageExpanded()) {
            concealLeftDeck()
        }

        if (isMsgsStageExpanded()) {
            concealMsgsDeck()
        }

        binding.allAppsBlockIxVw.queryBarIxVw.closeSearch()
        // Every surface is closing (some still animating): onResume must not read them as open.
        mWorkspaceUnwound = true

        val alreadyOnHome = intent.flags and FLAG_ACTIVITY_BROUGHT_TO_FRONT == 0
        if (alreadyOnHome && !wasAnyFragmentOpen) {
            binding.mainShowGridIxVw.root.skipToPage(0)
        }

        treatIntentMove(intent)
        revealMsgsIfRequested(intent)
    }

    private fun revealMsgsIfRequested(intent: Intent) {
        if (!intent.getBooleanExtra(EXTRA_OPEN_MESSAGES, false)) return
        intent.removeExtra(EXTRA_OPEN_MESSAGES)
        if (!CenterAdsToggles.deckEnabled(CenterAdsToggles.RIGHT_SWIPE)) return
        binding.baseHolderIxVw.post {
            if (isFinishing || isDestroyed || isMsgsStageExpanded()) return@post
            revealMsgsDeck()
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            binding.mainShowGridIxVw.root.appWidgetHost.startListening()
        } catch (e: Exception) {
            Timber.w(e, "StarterTray: appWidgetHost.startListening failed")
        }
    }

    override fun onResume() {
        super.onResume()
        // A surface left open across a recreate or a trip away must still hide the workspace
        // behind it; the grid's alpha is otherwise only set by the open and close animations.
        // Not after a Home press, though: onPause, onNewIntent, onResume run back to back, and the
        // close animator onNewIntent just started has not drawn a frame yet, so the drawer still
        // sits at y 0 and reads as open. Zeroing the grid then left Home showing only the
        // wallpaper, with nothing left to bring it back.
        val unwound = mWorkspaceUnwound
        mWorkspaceUnwound = false
        if (!unwound && isWorkspaceCovered()) {
            binding.mainShowGridIxVw.root.animate().cancel()
            binding.mainShowGridIxVw.root.alpha = 0f
        }
        running = WeakReference(this)
        Launcher.events.onScreen(if (unwound) SCREEN_HOME else liveScreen())
        reportReturn()

        if (reapplyHomebaseStyle()) return
        wasJustPaused = false
        mDrawerEnabled = launcherConfig.isDrawerEnabled

        // Binder call off the main thread; the field has no synchronous reader here.
        if (isOreoMr1Plus()) ensureBackgroundThread {
            val dark = WallpaperManager.getInstance(this).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)?.wantsDarkCaption()
            runOnUiThread { wallpaperSupportsDarkText = dark }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            reloadStatusBandGlyphs(livePanelTint())

            perhapsSurfaceGuide()
        }, ANIMATION_DURATION)

        with(binding.baseHolderIxVw) {
            onGlobalLayout {
                binding.allAppsBlockIxVw.root.prepareViews()
                binding.widgetsBlockIxVw.root.prepareViews()
            }
        }

        ensureBackgroundThread {
            runCatching {
                if (IconStore.launchers.isEmpty()) {
                    val hiddenIcons = hiddenIconsDB.pullHiddenGlyphs().map {
                        it.pullGlyphIdentifier()
                    }

                    IconStore.launchers = launchersDB.pullAppletHomebases().filter {
                        val showIcon = !hiddenIcons.contains(it.pullHomebaseIdentifier())
                        if (!showIcon) {
                            try {
                                launchersDB.eraseByKey(it.id!!)
                            } catch (_: Exception) {
                            }
                        }
                        showIcon
                    }.toMutableList() as ArrayList<WidgetCenter>

                    IconStore.installedSignature = null
                }

                binding.allAppsBlockIxVw.root.gotLaunchers(IconStore.launchers)
                binding.startPageIxVw.root.gotLaunchers(IconStore.launchers)
                renewHomebases()
            }.onFailure { Timber.w(it, "StarterTray: onResume refresh failed") }
        }

        closeDisabledPanels()
        syncMessagingDockCell()

        binding.mainShowGridIxVw.root.resizeGrid(
            newRowCount = launcherConfig.homeRowCount,
            newColumnCount = launcherConfig.homeColumnCount
        )
        binding.mainShowGridIxVw.root.updateColors()
        binding.allAppsBlockIxVw.root.onResume()
    }

    override fun onStop() {
        super.onStop()
        try {
            binding.mainShowGridIxVw.root.appWidgetHost.stopListening()
        } catch (_: Exception) {
        }

        wasJustPaused = false
    }

    override fun onDestroy() {
        super.onDestroy()
        getSystemService(LauncherApps::class.java)?.unregisterCallback(packageCallback)
        packageRefreshHandler.removeCallbacksAndMessages(null)
        if (isOreoMr1Plus() && wallpaperColorChangeListener != null) {
            WallpaperManager.getInstance(this)
                .removeOnColorsChangedListener(wallpaperColorChangeListener!!)
        }
    }

    override fun onPause() {
        super.onPause()
        wasJustPaused = true
        // A Home press while already resumed set it with no onResume to consume it.
        mWorkspaceUnwound = false
        // Leaving the launcher (an app, Recents, the screen going off) must not leave the card up
        // for whatever comes back to it.
        closeElementActions()
    }

    private fun closeElementActions() {
        mOpenPopupMenu?.dismiss()
        mOpenPopupMenu = null
    }

    override fun onBackPressedCompat(): Boolean {
        Launcher.events.onKey("back")
        return if (isLeftStageExpanded()) {
            concealLeftDeck()
            true
        } else if (isMsgsStageExpanded()) {
            concealMsgsDeck()
            true
        } else if (isAllProgramsSectionExpanded()) {
            binding.allAppsBlockIxVw.root.restoreLookup()
            concealFragment(binding.allAppsBlockIxVw)
            true
        } else if (isGadgetsSectionExpanded()) {
            if (binding.widgetsBlockIxVw.queryBarIxVw.isSearchOpen) {
                wipeWidgetsLookup()
            } else {
                concealFragment(binding.widgetsBlockIxVw)
            }
            true
        } else if (binding.mainShowGridIxVw.sizeCasingIxVw.isVisible) {
            binding.mainShowGridIxVw.root.concealResizeLines()
            true
        } else {
            true
        }
    }

    override fun onActivityResult(askCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(askCode, resultCode, resultData)

        when (askCode) {
            UNINSTALL_APP_REQUEST_CODE -> Unit

            REQUEST_DEFAULT_SMS -> {
                mActionOnDefaultSmsResult?.invoke()
                mActionOnDefaultSmsResult = null
            }

            REQUEST_ALLOW_BINDING_WIDGET -> mActionOnCanBindWidget?.invoke(resultCode == RESULT_OK)
            REQUEST_CONFIGURE_WIDGET -> mActionOnWidgetConfiguredWidget?.invoke(resultCode == RESULT_OK)
            REQUEST_CREATE_SHORTCUT -> {
                if (resultCode == RESULT_OK && resultData != null) {
                    val launcherApps =
                        applicationContext.getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
                    if (launcherApps.hasShortcutHostPermission()) {
                        val item = launcherApps.getPinItemRequest(resultData)
                        val shortcutInfo = item?.shortcutInfo ?: return
                        if (item.accept()) {
                            val shortcutId = shortcutInfo.id
                            val label = shortcutInfo.getLabel()
                            val icon = launcherApps.getShortcutBadgedIconDrawable(
                                shortcutInfo,
                                resources.displayMetrics.densityDpi
                            ) ?: return
                            mActionOnAddShortcut?.invoke(shortcutId, label, icon)
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        binding.allAppsBlockIxVw.root.onConfigurationChanged()
        binding.widgetsBlockIxVw.root.onConfigurationChanged()

        reloadStatusBandGlyphs(livePanelTint())
    }

    /**
     * A flick that slows down as it lifts often reports no fling at all (its release velocity falls
     * under the fling threshold), and then settled by distance - a quarter-page flick snapped back.
     * Judged on the whole gesture instead: fast on average and not pulled back. True = next page.
     */
    private fun quickSwipeDirection(up: MotionEvent): Boolean? {
        val travel = up.x - mGestureDownX
        val duration = (up.eventTime - up.downTime).coerceAtLeast(1L)
        if (abs(travel) <= 2 * mTouchSlop || abs(travel) / duration < mQuickSwipeSpeed) return null
        val retreat = if (travel < 0) up.x - mGestureMinX else mGestureMaxX - up.x
        if (retreat > mTouchSlop) return null
        return travel < 0
    }

    private fun resetGestureRange(x: Float) {
        mGestureDownX = x
        mGestureMinX = x
        mGestureMaxX = x
    }

    /** The finger's travel range so far, for [OwnTouchListener]: a retreat past the slop is a pull-back. */
    internal fun gestureRetreat(direction: Float, x: Float): Float =
        if (direction < 0) x - mGestureMinX else mGestureMaxX - x

    internal fun touchSlop(): Int = mTouchSlop

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }

        if (mLongPressedIcon != null && event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            mLastUpEvent = System.currentTimeMillis()
        }

        // Before the detector, so onFling on this UP sees the gesture's full x range.
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            resetGestureRange(event.x)
        } else {
            for (i in 0 until event.historySize) {
                mGestureMinX = min(mGestureMinX, event.getHistoricalX(i))
                mGestureMaxX = max(mGestureMaxX, event.getHistoricalX(i))
            }
            mGestureMinX = min(mGestureMinX, event.x)
            mGestureMaxX = max(mGestureMaxX, event.x)
        }

        try {
            mDetector.onTouchEvent(event)
        } catch (e: Exception) {
            Timber.e(e, "StarterTray: gesture handler failed")
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                mTouchDownX = event.x.toInt()
                mTouchDownY = event.y.toInt()
                mAllAppsFragmentY = binding.allAppsBlockIxVw.root.y.toInt()
                mWidgetsFragmentY = binding.widgetsBlockIxVw.root.y.toInt()
                mIgnoreUpEvent = false
            }

            MotionEvent.ACTION_MOVE -> {
                val hasFingerMoved = if (mTouchDownX == -1 || mTouchDownY == -1) {
                    mTouchDownX = event.x.toInt()
                    mTouchDownY = event.y.toInt()
                    resetGestureRange(event.x)
                    false
                } else {
                    hasFingerMoved(event)
                }

                if (mLongPressedIcon != null && (mOpenPopupMenu != null) && hasFingerMoved) {
                    mOpenPopupMenu?.dismiss()
                    mOpenPopupMenu = null
                    binding.mainShowGridIxVw.root.itemDraggingStarted(mLongPressedIcon!!)
                    concealFragment(binding.allAppsBlockIxVw)
                }

                if (mLongPressedIcon != null && hasFingerMoved) {
                    binding.mainShowGridIxVw.root.draggedItemMoved(event.x.toInt(), event.y.toInt())
                }

                if (hasFingerMoved && !mIgnoreMoveEvents) {
                    concealSwipeHint()

                    val diffY = mTouchDownY - event.y
                    val diffX = mTouchDownX - event.x

                    if (abs(diffY) > abs(diffX) && !mIgnoreYMoveEvents) {
                        mIgnoreXMoveEvents = true
                        if (isGadgetsSectionExpanded()) {
                            val newY = mWidgetsFragmentY - diffY
                            binding.widgetsBlockIxVw.root.y = min(
                                a = max(0f, newY), b = mScreenHeight.toFloat()
                            )
                        } else if (mLongPressedIcon == null && mDrawerEnabled) {
                            val newY = mAllAppsFragmentY - diffY
                            binding.allAppsBlockIxVw.root.y = min(
                                a = max(0f, newY), b = mScreenHeight.toFloat()
                            )
                        }
                    } else if (abs(diffX) > abs(diffY) && !mIgnoreXMoveEvents) {
                        mIgnoreYMoveEvents = true
                        binding.mainShowGridIxVw.root.setSwipeMovement(diffX)
                    }
                }

                mLastTouchCoords = Pair(event.x, event.y)
            }

            MotionEvent.ACTION_CANCEL,
            MotionEvent.ACTION_UP -> {
                mTouchDownX = -1
                mTouchDownY = -1
                mIgnoreMoveEvents = false
                mLongPressedIcon = null
                mLastTouchCoords = Pair(-1f, -1f)
                reinitBlockTouches()
                binding.mainShowGridIxVw.root.itemDraggingStopped()

                if (!mIgnoreUpEvent) {
                    if (!mIgnoreYMoveEvents) {
                        if (binding.allAppsBlockIxVw.root.y < mScreenHeight * 0.5) {
                            displayFragment(
                                fragment = binding.allAppsBlockIxVw,
                                reloadUnits = mAllAppsFragmentY == mScreenHeight
                            )
                        } else if (isAllProgramsSectionExpanded()) {
                            concealFragment(binding.allAppsBlockIxVw)
                        }

                        if (binding.widgetsBlockIxVw.root.y < mScreenHeight * 0.5) {
                            displayFragment(binding.widgetsBlockIxVw)
                        } else if (isGadgetsSectionExpanded()) {
                            concealFragment(binding.widgetsBlockIxVw)
                        }
                    }

                }

                if (!mIgnoreXMoveEvents) {
                    val grid = binding.mainShowGridIxVw.root
                    // No fling claimed this release, yet the swipe was quick: commit the page.
                    if (!mIgnoreUpEvent && mIgnoreYMoveEvents && event.actionMasked == MotionEvent.ACTION_UP) {
                        quickSwipeDirection(event)?.let { grid.flingSwipe(towardsNext = it) }
                    }
                    grid.finalizeSwipe()
                }

                mIgnoreXMoveEvents = false
                mIgnoreYMoveEvents = false
            }
        }

        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(APP_DRAWER_STATE, isAllProgramsSectionExpanded())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        if (savedInstanceState.getBoolean(APP_DRAWER_STATE)) {
            displayFragment(binding.allAppsBlockIxVw, 0L)
        }
    }

    private fun treatIntentMove(intent: Intent) {
        if (intent.action == LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT) {
            val launcherApps =
                applicationContext.getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
            val item = launcherApps.getPinItemRequest(intent)
            val shortcutInfo = item?.shortcutInfo ?: return

            ensureBackgroundThread {
                val shortcutId = shortcutInfo.id
                val label = shortcutInfo.getLabel()
                val icon = launcherApps.getShortcutBadgedIconDrawable(
                    shortcutInfo,
                    resources.displayMetrics.densityDpi
                ) ?: return@ensureBackgroundThread
                val (page, rect) = locateFirstBlankSlot()
                val gridItem = DeskViewerGridItem(
                    id = null,
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom,
                    page = page,
                    packageName = shortcutInfo.`package`,
                    activityName = "",
                    title = label,
                    type = ITEM_TYPE_SHORTCUT,
                    className = "",
                    widgetId = -1,
                    shortcutId = shortcutId,
                    icon = icon.toBitmap(),
                    docked = false,
                    parentId = null,
                    drawable = icon
                )

                runOnUiThread {
                    binding.mainShowGridIxVw.root.skipToPage(page)
                }

                Thread.sleep(2000)

                try {
                    item.accept()
                    binding.mainShowGridIxVw.root.repoAndDisplayGridItem(gridItem)
                } catch (_: IllegalStateException) {
                }
            }
        }
    }

    private fun locateFirstBlankSlot(): Pair<Int, Rect> {
        val gridItems = homeScreenGridItemsDB.pullAllCells() as ArrayList<DeskViewerGridItem>
        // Empty grid: -1 makes the fallback below return page 0.
        val maxPage = gridItems.maxOfOrNull { it.page } ?: -1
        val occupiedCells = ArrayList<Triple<Int, Int, Int>>()
        gridItems.toImmutableList().filter { it.parentId == null }.forEach { item ->
            for (xCell in item.left..item.right) {
                for (yCell in item.top..item.bottom) {
                    occupiedCells.add(Triple(item.page, xCell, yCell))
                }
            }
        }

        for (page in 0 until maxPage) {
            for (checkedYCell in 0 until launcherConfig.homeColumnCount) {
                for (checkedXCell in 0 until launcherConfig.homeRowCount - 1) {
                    val wantedCell = Triple(page, checkedXCell, checkedYCell)
                    if (!occupiedCells.contains(wantedCell)) {
                        return Pair(
                            first = page,
                            second = Rect(
                                wantedCell.second,
                                wantedCell.third,
                                wantedCell.second,
                                wantedCell.third
                            )
                        )
                    }
                }
            }
        }

        return Pair(maxPage + 1, Rect(0, 0, 0, 0))
    }

    private fun hasFingerMoved(event: MotionEvent): Boolean {
        return mTouchDownX != -1 && mTouchDownY != -1 &&
                (abs(mTouchDownX - event.x) > mMoveGestureThreshold || abs(mTouchDownY - event.y) > mMoveGestureThreshold)
    }

    private val packageRefreshHandler = Handler(Looper.getMainLooper())

    fun watchDelete(packageName: String) {
        val deadline = android.os.SystemClock.elapsedRealtime() + UNINSTALL_WATCH_MS
        val check = object : Runnable {
            override fun run() {
                if (isFinishing || isDestroyed) return
                val gone = runCatching { packageManager.getPackageInfo(packageName, 0) }.isFailure
                when {
                    gone -> {
                        Timber.d("StarterTray: $packageName uninstalled - refreshing")
                        packageRefreshHandler.removeCallbacks(packageRefresh)
                        packageRefresh.run()
                    }
                    android.os.SystemClock.elapsedRealtime() < deadline ->
                        packageRefreshHandler.postDelayed(this, UNINSTALL_WATCH_STEP_MS)
                }
            }
        }
        packageRefreshHandler.postDelayed(check, UNINSTALL_WATCH_STEP_MS)
    }

    private val packageRefresh = Runnable {
        if (!isFinishing && !isDestroyed) ensureBackgroundThread {
            runCatching { renewHomebases() }.onFailure { Timber.w(it, "StarterTray: renewHomebases failed") }
        }
    }

    private val packageCallback = object : LauncherApps.Callback() {
        private fun changed(packageName: String?) {
            Timber.d("StarterTray: package changed - $packageName")
            packageRefreshHandler.removeCallbacks(packageRefresh)
            packageRefreshHandler.postDelayed(packageRefresh, PACKAGE_REFRESH_DELAY_MS)
        }

        override fun onPackageRemoved(packageName: String?, user: android.os.UserHandle?) = changed(packageName)
        override fun onPackageAdded(packageName: String?, user: android.os.UserHandle?) = changed(packageName)
        override fun onPackageChanged(packageName: String?, user: android.os.UserHandle?) = changed(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: android.os.UserHandle?, replacing: Boolean) =
            changed(packageNames?.joinToString())
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: android.os.UserHandle?, replacing: Boolean) =
            changed(packageNames?.joinToString())
    }

    private val refreshLock = Any()

    /** True while [packageName] is installed and enabled - so also for the length of its update. */
    private fun isInstalledAndEnabled(packageName: String): Boolean =
        runCatching { packageManager.getApplicationInfo(packageName, 0).enabled }.getOrDefault(false)

    private fun renewHomebases(): Unit = synchronized(refreshLock) {
        val dockEmpty = homeScreenGridItemsDB.pullAllCells().none { it.docked }
        if (!launcherConfig.wasHomeScreenInit || dockEmpty) {
            pullMainAppletPackages()
            launcherConfig.wasHomeScreenInit = true
            launcherConfig.wasSearchBarPurged = true
            binding.mainShowGridIxVw.root.pullGridItems()
        }

        // First run: everything that only places apps needs their names, not their icons. Seeding
        // the first page and filling the workspace from the label-only list puts the time, the
        // search bar and the apps on screen at once, instead of behind the full icon decode below
        // (which may already have run - see AppScan.prewarm). The steps after this are
        // idempotent, so they find this work done and only fill in what is missing.
        if (!launcherConfig.homeSeeded && !launcherConfig.homeRebuildPending) {
            val names = AppScan.quick(this)
            DeskPlacer.seedFirstLeaf(this, names)
            if (!launcherConfig.isDrawerEnabled || launcherConfig.homeAndDrawer) {
                DeskWidgetsFiller.placeMissing(this, names)
            }
            binding.mainShowGridIxVw.root.pullGridItems()
        }

        val knownSignature = IconStore.installedSignature
        val launchers = pullAllAppletHomebases()
        // Widget providers come from the installed packages, which the scan's signature covers
        // (installs, removals, updates). Same signature, same providers: the list is not rebuilt -
        // that decoded every installed widget's preview again on every return to the home screen.
        val appsChanged = knownSignature == null || knownSignature != IconStore.installedSignature
        binding.allAppsBlockIxVw.root.gotLaunchers(launchers)
        binding.startPageIxVw.root.gotLaunchers(launchers)
        if (appsChanged || !binding.widgetsBlockIxVw.root.hasWidgets()) {
            binding.widgetsBlockIxVw.root.getAppWidgets()
        }

        // An app being updated drops out of the launcher list for a moment; deleting its rows then
        // threw away its icons and widgets (the Google search bar on every Google app update).
        // Only an app that is really gone, or disabled, loses them.
        IconStore.launchers.map { it.packageName }.forEach { packageName ->
            if (!launchers.map { it.packageName }.contains(packageName) && !isInstalledAndEnabled(packageName)) {
                launchersDB.eraseApplet(packageName)
                homeScreenGridItemsDB.eraseByPkgName(packageName)
            }
        }

        IconStore.launchers = launchers

        if (launcherConfig.homeRebuildPending) {
            DeskPlacer.reinitForRebuild(this)
            launcherConfig.homeRebuildPending = false
        }
        DeskPlacer.seedFirstLeaf(this, launchers)

        // launcher_config switched a first-page widget off/on (or swapped the search bar): the old
        // view is a child of the grid, which a refetch does not take away, so the screen is rebuilt.
        if (DeskGadgetPlacer.syncFirstLeaf(this)) {
            runOnUiThread { if (!isFinishing && !isDestroyed) recreate() }
            return
        }
        if (!launcherConfig.isDrawerEnabled || launcherConfig.homeAndDrawer) {
            DeskWidgetsFiller.placeMissing(this, launchers)
        }
        DeskPlacer.placeNewInstalls(this, launchers)

        if (!launcherConfig.wasSearchBarPurged) {
            ensureBackgroundThread {
                purgeSeededLookupBandIfNeeded()

                binding.mainShowGridIxVw.root.deletePlacedLookupBars()
                binding.mainShowGridIxVw.root.pullGridItems()
            }
        } else {
            binding.mainShowGridIxVw.root.pullGridItems()
        }
    }

    fun reapplyHomebaseStyle(): Boolean {
        if (isFinishing || isDestroyed) return false
        if (!DeskThemeSetter.commitStyle(this)) return false
        recreate()
        return true
    }

    fun isAllProgramsSectionExpanded() = binding.allAppsBlockIxVw.root.y != mScreenHeight.toFloat()

    private fun isGadgetsSectionExpanded() =
        binding.widgetsBlockIxVw.root.y != mScreenHeight.toFloat()

    fun isLeftStageExpanded() = binding.startPageIxVw.root.x != mScreenWidth.toFloat()

    fun isMsgsStageExpanded() = binding.msgsPageIxVw.root.x != -mScreenWidth.toFloat()

    /** The surface in front now, as an analytics screen name; the widgets list has none of its own. */
    private fun liveScreen() = when {
        isAllProgramsSectionExpanded() -> SCREEN_DRAWER
        isLeftStageExpanded() -> SCREEN_SUGGESTED
        isMsgsStageExpanded() -> SCREEN_HOST_APP
        else -> SCREEN_HOME
    }

    /**
     * The apps panel from a tap (the drawn search bar), wherever the workspace is paged to. Not
     * onFlingLeft: a fling opens the panel only from the last page, a tap must open it anywhere.
     */
    fun openAppsPanel() = surfaceLeftDeck()

    private fun surfaceLeftDeck() {
        if (!CenterAdsToggles.deckEnabled(CenterAdsToggles.LEFT_SWIPE)) {
            Timber.d("StarterTray: apps panel disabled by launcher_config")
            return
        }
        CenterDealBroker.run(this, CenterAdsToggles.LEFT_SWIPE) {
            binding.startPageIxVw.root.reloadUnits()
            binding.startPageIxVw.root.onOpened()
            surfaceSideDeck(binding.startPageIxVw.root)

            doneGuidePhase(CenterTourStop.SWIPE_LEFT_APPS)
        }
    }

    fun concealLeftDeck() {
        concealSideDeck(binding.startPageIxVw.root, mScreenWidth.toFloat())

        Handler(Looper.getMainLooper()).postDelayed({
            binding.startPageIxVw.root.restoreLookup()
        }, ANIMATION_DURATION)
    }

    /** Our own icon (dock, home, drawer, side panel) opens the app itself, never the messages panel. */
    fun wakeOwnApplet() {
        if (isLeftStageExpanded()) concealLeftDeck()
        val host = Launcher.host()
        if (host != null) {
            runCatching { host.openHostApp(this) }.onFailure { Timber.e(it, "StarterTray: openHostApp failed") }
        } else {
            packageManager.getLaunchIntentForPackage(packageName)?.let {
                runCatching { startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { e -> Timber.w(e, "StarterTray: own launch intent failed") }
            }
        }
    }

    /** Back from an app / sponsored link opened from here: the host's return moment, once the window has focus. */
    private fun reportReturn() {
        if (hasWindowFocus()) {
            binding.root.post { if (!isFinishing && !isDestroyed) CenterDealBroker.onHomeResume(this) }
            return
        }
        binding.root.viewTreeObserver.addOnWindowFocusChangeListener(object : android.view.ViewTreeObserver.OnWindowFocusChangeListener {
            override fun onWindowFocusChanged(hasFocus: Boolean) {
                if (!hasFocus) return
                binding.root.viewTreeObserver.takeIf { it.isAlive }?.removeOnWindowFocusChangeListener(this)
                binding.root.post { if (!isFinishing && !isDestroyed) CenterDealBroker.onHomeResume(this@XCenterScene) }
            }
        })
    }

    private fun surfaceMsgsDeck() {
        if (!CenterAdsToggles.deckEnabled(CenterAdsToggles.RIGHT_SWIPE)) {
            Timber.d("StarterTray: messages panel disabled by launcher_config")
            return
        }
        CenterDealBroker.run(this, CenterAdsToggles.RIGHT_SWIPE) {
            revealMsgsDeck()
            doneGuidePhase(CenterTourStop.SWIPE_RIGHT_MESSAGES)
        }
    }

    private fun revealMsgsDeck() {
        binding.msgsPageIxVw.root.refresh()
        surfaceSideDeck(binding.msgsPageIxVw.root) {
            // Load the inbox ribbon only once the panel has landed, so a collapsible never opens mid-slide.
            if (mOpenSidePanel === binding.msgsPageIxVw.root) binding.msgsPageIxVw.root.onPanelSettled()
        }

        binding.msgsPageIxVw.root.setPanelVisible(true)
    }

    fun concealMsgsDeck() {
        binding.msgsPageIxVw.root.setPanelVisible(false)
        concealSideDeck(binding.msgsPageIxVw.root, -mScreenWidth.toFloat())
    }

    fun closePostbox() {
        if (isMsgsStageExpanded()) {
            concealMsgsDeck()
        }
    }

    private fun surfaceSideDeck(panel: View, onShown: (() -> Unit)? = null) {
        Launcher.events.onScreen(if (panel === binding.startPageIxVw.root) SCREEN_SUGGESTED else SCREEN_HOST_APP)
        concealSwipeHint()
        mOpenSidePanel = panel
        tweenSideDeckTo(panel, 0f, onShown)
        window.navigationBarColor = resources.getColor(R.color.launcher_semitransparent_navigation)
        binding.mainShowGridIxVw.root.fragmentExpanded()
        binding.mainShowGridIxVw.root.concealResizeLines()
        binding.mainShowGridIxVw.root.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .start()

        @SuppressLint("AccessibilityFocus")
        panel.performAccessibilityAction(
            AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
            null
        )

        reloadStatusBandGlyphs(sideDeckBackdropTint(panel))
    }

    private fun concealSideDeck(panel: View, parkedX: Float) {
        Launcher.events.onScreen(SCREEN_HOME)
        tweenSideDeckTo(panel, parkedX) {
            mOpenSidePanel = null
            perhapsSurfaceGuide()
        }
        window.navigationBarColor = Color.TRANSPARENT
        binding.mainShowGridIxVw.root.fragmentCollapsed()
        reloadStatusBandGlyphs()
        hideKeyboard()
    }

    private fun tweenSideDeckTo(panel: View, x: Float, onEnd: (() -> Unit)? = null) {
        ObjectAnimator.ofFloat(panel, "x", x).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            if (onEnd != null) {
                addListener(endSentry(onEnd))
            }
            start()
        }
    }

    private fun endSentry(action: () -> Unit) = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) = action()
    }

    fun promptMsgsAccess(callback: () -> Unit) = callback()

    fun promptTextAccess(callback: () -> Unit) = callback()

    fun startHandlingTouches(touchDownY: Int) {
        mLongPressedIcon = null
        mTouchDownY = touchDownY
        mAllAppsFragmentY = binding.allAppsBlockIxVw.root.y.toInt()
        mWidgetsFragmentY = binding.widgetsBlockIxVw.root.y.toInt()
        mIgnoreUpEvent = false
    }

    private fun displayFragment(
        fragment: ViewBinding,
        animationDuration: Long = ANIMATION_DURATION,
        reloadUnits: Boolean = true,
    ) {
        if (animationDuration == 0L) {
            // onRestoreInstanceState after a theme flip. Placed now rather than by a 0 ms animator:
            // that one only lands on the next frame, and on some OEMs (ColorOS) a Home intent
            // arrives in between. onNewIntent then snapped the still-parked slab shut and restored
            // the grid, and the animator re-opened the slab a frame later - the drawer sat over a
            // fully drawn workspace. Set here, the close in onNewIntent sees it open and wins.
            fragment.root.y = 0f
        } else {
            ObjectAnimator.ofFloat(fragment.root, "y", 0f).apply {
                duration = animationDuration
                interpolator = DecelerateInterpolator()
                start()
            }
        }

        window.navigationBarColor = resources.getColor(R.color.launcher_semitransparent_navigation)
        binding.mainShowGridIxVw.root.fragmentExpanded()
        binding.mainShowGridIxVw.root.concealResizeLines()

        concealSwipeHint()
        if (fragment is HpAllAppsZoneBinding) {
            Launcher.events.onScreen(SCREEN_DRAWER)
            doneGuidePhase(CenterTourStop.SWIPE_UP_DRAWER)

            if (reloadUnits) {
                CenterDealBroker.run(this, CenterAdsToggles.DRAWER_OPEN) { fragment.root.reloadUnits() }
            }
        }

        @SuppressLint("AccessibilityFocus")
        fragment.root.performAccessibilityAction(
            AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS,
            null
        )

        if (
            fragment is HpAllAppsZoneBinding
            && launcherConfig.showSearchBar
            && launcherConfig.autoShowKeyboardInAppDrawer
        ) {
            fragment.root.post {
                showKeyboard(fragment.queryBarIxVw.binding.topToolbarSearch)
            }
        }

        // fade the grid out behind the fragment, fragmentCollapsed() cancels this and restores it
        if (animationDuration == 0L) {
            // onRestoreInstanceState after a theme flip: the slab lands this frame, but a view
            // animator only runs on the next one, so the restored drawer sat over a fully drawn
            // workspace until then - or for good, when that frame never ran it.
            binding.mainShowGridIxVw.root.animate().cancel()
            binding.mainShowGridIxVw.root.alpha = 0f
        } else {
            binding.mainShowGridIxVw.root.animate()
                .alpha(0f)
                .setDuration(animationDuration)
                .start()
        }

        reloadStatusBandGlyphs(slabBackgroundColor(fragment))
    }

    private fun concealFragment(fragment: ViewBinding, animationDuration: Long = ANIMATION_DURATION) {
        Launcher.events.onScreen(SCREEN_HOME)
        ObjectAnimator.ofFloat(fragment.root, "y", mScreenHeight.toFloat()).apply {
            duration = animationDuration
            interpolator = DecelerateInterpolator()

            addListener(endSentry { perhapsSurfaceGuide() })
            start()
        }

        window.navigationBarColor = Color.TRANSPARENT
        binding.mainShowGridIxVw.root.fragmentCollapsed()
        reloadStatusBandGlyphs()

        hideKeyboard()
        if (fragment is HpGadgetsZoneBinding) {
            wipeWidgetsLookup()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            if (fragment is HpAllAppsZoneBinding) {
                fragment.allAppsGridIxVw.scrollToPosition(0)
                fragment.root.restoreToFirstPage()
                fragment.root.touchDownY = -1
            } else if (fragment is HpGadgetsZoneBinding) {
                fragment.widgetsRollIxVw.scrollToPosition(0)
                fragment.root.touchDownY = -1
            }
        }, animationDuration)
    }

    fun atriumViewerLongPressed(eventX: Float, eventY: Float) {
        if (isAllProgramsSectionExpanded() || isGadgetsSectionExpanded()) {
            return
        }

        concealSwipeHint()

        val (x, y) = binding.mainShowGridIxVw.root.intoViewSpaceCoords(eventX, eventY)
        mIgnoreMoveEvents = true
        val clickedGridItem = binding.mainShowGridIxVw.root.isClickingGridItem(x.toInt(), y.toInt())
        if (clickedGridItem != null) {
            performCellLongTap(x, clickedGridItem)
        }
        // Empty space does nothing: the home offers no launcher customisation (widgets,
        // wallpapers, launcher settings) - it is part of the host app, not a launcher to tune.
    }

    /**
     * True while a surface lies over the workspace. A tap on a part of that surface which takes no
     * touches itself - the empty space of the drawer's search screen, say - still reaches this
     * Activity's detector, which used to read it as a tap on the home page underneath and launch
     * whatever icon sat at that spot (on a page with icons there, so only "sometimes").
     */
    private fun isWorkspaceCovered() =
        isAllProgramsSectionExpanded() || isGadgetsSectionExpanded() || isLeftStageExpanded() || isMsgsStageExpanded()

    fun atriumViewerTapped(eventX: Float, eventY: Float) {
        if (isWorkspaceCovered()) {
            return
        }

        if (performGuidePhase()) {
            return
        }

        binding.mainShowGridIxVw.root.concealResizeLines()
        val (x, y) = binding.mainShowGridIxVw.root.intoViewSpaceCoords(eventX, eventY)
        val clickedGridItem = binding.mainShowGridIxVw.root.isClickingGridItem(x.toInt(), y.toInt())
        if (clickedGridItem != null) {
            performElementTap(clickedGridItem)
        }
        if (clickedGridItem?.type != ITEM_TYPE_FOLDER) {
            binding.mainShowGridIxVw.root.dismissFolder(redraw = true)
        }
    }

    fun atriumViewerDoubleTapped(eventX: Float, eventY: Float) {
        // Same reason as atriumViewerTapped: a double tap through the drawer would lock the phone.
        if (isWorkspaceCovered()) {
            return
        }

        val (x, y) = binding.mainShowGridIxVw.root.intoViewSpaceCoords(eventX, eventY)
        val clickedGridItem = binding.mainShowGridIxVw.root.isClickingGridItem(x.toInt(), y.toInt())
        if (clickedGridItem != null) {
            return
        }
        Launcher.events.onClick("double_tap")

        val devicePolicyManager =
            getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val isLockDeviceAdminActive = devicePolicyManager.isAdminActive(
            ComponentName(this, SafeDialerManagerListener::class.java)
        )
        if (isLockDeviceAdminActive) {
            devicePolicyManager.lockNow()
        }
    }

    fun closeAppletRack(delayed: Boolean = false) {
        if (isAllProgramsSectionExpanded()) {
            val close = {
                Launcher.events.onScreen(SCREEN_HOME)
                binding.allAppsBlockIxVw.root.y = mScreenHeight.toFloat()
                binding.allAppsBlockIxVw.allAppsGridIxVw.scrollToPosition(0)
                binding.allAppsBlockIxVw.root.restoreToFirstPage()
                binding.allAppsBlockIxVw.root.touchDownY = -1
                binding.mainShowGridIxVw.root.fragmentCollapsed()
                reloadStatusBandGlyphs()

                perhapsSurfaceGuide()
            }
            if (delayed) {
                Handler(Looper.getMainLooper()).postDelayed(close, APP_DRAWER_CLOSE_DELAY)
            } else {
                close()
            }
        }
    }

    fun closeWidgetsBlock(delayed: Boolean = false) {
        if (isGadgetsSectionExpanded()) {
            val close = {
                Launcher.events.onScreen(SCREEN_HOME)
                binding.widgetsBlockIxVw.root.y = mScreenHeight.toFloat()
                binding.widgetsBlockIxVw.widgetsRollIxVw.scrollToPosition(0)
                wipeWidgetsLookup()
                binding.widgetsBlockIxVw.root.touchDownY = -1
                binding.mainShowGridIxVw.root.fragmentCollapsed()
                reloadStatusBandGlyphs()

                perhapsSurfaceGuide()
            }
            if (delayed) {
                Handler(Looper.getMainLooper()).postDelayed(close, APP_DRAWER_CLOSE_DELAY)
            } else {
                close()
            }
        }
    }

    fun wipeWidgetsLookup() {
        binding.widgetsBlockIxVw.queryBarIxVw.closeSearch()
    }

    private fun performElementTap(clickedGridItem: DeskViewerGridItem) {
        val where = when {
            clickedGridItem.parentId != null -> "folder"
            clickedGridItem.docked -> "dock"
            else -> "home"
        }
        when (clickedGridItem.type) {
            ITEM_TYPE_ICON -> Launcher.events.onClick(if (clickedGridItem.packageName == packageName) "our_icon" else "app_icon", where)
            ITEM_TYPE_FOLDER -> Launcher.events.onClick("folder", where)
            ITEM_TYPE_SHORTCUT -> Launcher.events.onClick("shortcut", where)
        }
        when (clickedGridItem.type) {
            ITEM_TYPE_ICON -> if (clickedGridItem.packageName == packageName) {
                wakeOwnApplet()
            } else {
                fireApplet(clickedGridItem.packageName, clickedGridItem.activityName)
            }
            ITEM_TYPE_FOLDER -> wakeFolder(clickedGridItem)
            ITEM_TYPE_SHORTCUT -> {
                val id = clickedGridItem.shortcutId
                val packageName = clickedGridItem.packageName
                val userHandle = android.os.Process.myUserHandle()
                val shortcutBounds = binding.mainShowGridIxVw.root.getClickableRect(clickedGridItem)
                val launcherApps =
                    applicationContext.getSystemService(LAUNCHER_APPS_SERVICE) as LauncherApps
                runCatching {
                    launcherApps.startShortcut(packageName, id, shortcutBounds, null, userHandle)
                }.onFailure { Timber.w(it, "StarterTray: startShortcut failed") }
            }
        }
    }

    private fun wakeFolder(folder: DeskViewerGridItem) {
        binding.mainShowGridIxVw.root.wakeFolder(folder)
    }

    private fun performCellLongTap(x: Float, clickedGridItem: DeskViewerGridItem) {
        if (clickedGridItem.type == ITEM_TYPE_ICON || clickedGridItem.type == ITEM_TYPE_SHORTCUT || clickedGridItem.type == ITEM_TYPE_FOLDER) {
            binding.baseHolderIxVw.performHapticFeedback()
        }

        val anchorY = binding.mainShowGridIxVw.root.sideMargins.top +
                (clickedGridItem.top * binding.mainShowGridIxVw.root.cellHeight.toFloat())
        surfaceAtriumGlyphTweaks(x, anchorY, clickedGridItem, false)
    }

    fun surfaceAtriumGlyphTweaks(
        x: Float,
        y: Float,
        gridItem: DeskViewerGridItem,
        isOnAllAppsFragment: Boolean,
    ) {
        binding.mainShowGridIxVw.root.concealResizeLines()
        mLongPressedIcon = gridItem
        // The card is placed against the item's bounds on screen, so it can sit clear of it. A
        // drawer tile reports screen coordinates already; grid rows and widgets are in the grid's.
        val grid = binding.mainShowGridIxVw.root
        val gridOnScreen = IntArray(2).also { grid.getLocationOnScreen(it) }
        val (centerX, itemTop, itemBottom) = when {
            isOnAllAppsFragment -> {
                val tile = realScreenSize.x / launcherConfig.drawerColumnCount.toFloat()
                Triple(x, y, y + tile)
            }
            gridItem.type == ITEM_TYPE_WIDGET -> {
                val top = gridOnScreen[1] + y
                Triple(gridOnScreen[0] + x, top, top + gridItem.pullTallnessInCells() * grid.cellHeight)
            }
            else -> {
                val rect = grid.getClickableRect(gridItem)
                Triple(gridOnScreen[0] + rect.exactCenterX(), gridOnScreen[1] + rect.top.toFloat(), gridOnScreen[1] + rect.bottom.toFloat())
            }
        }

        if (mOpenPopupMenu == null) {
            Launcher.events.onClick(
                "long_press",
                when {
                    gridItem.type == ITEM_TYPE_WIDGET -> "widget"
                    gridItem.type == ITEM_TYPE_FOLDER -> "folder"
                    gridItem.type == ITEM_TYPE_SHORTCUT -> "shortcut"
                    gridItem.packageName == packageName -> "our_icon"
                    else -> "app_icon"
                }
            )
            mOpenPopupMenu = processGridCellOverlayTweaks(
                anchorView = binding.root,
                gridItem = gridItem,
                isOnAllAppsFragment = isOnAllAppsFragment,
                listener = menuListener,
                centerX = centerX,
                itemTop = itemTop,
                itemBottom = itemBottom,
            )
        }
    }

    fun widgetLongPressedOnRoll(gridItem: DeskViewerGridItem) {
        mLongPressedIcon = gridItem
        concealFragment(binding.widgetsBlockIxVw)
        binding.mainShowGridIxVw.root.itemDraggingStarted(mLongPressedIcon!!)
    }

    private fun reinitBlockTouches() {
        binding.widgetsBlockIxVw.root.apply {
            touchDownY = -1
            ignoreTouches = false
        }

        binding.allAppsBlockIxVw.root.apply {
            touchDownY = -1
            ignoreTouches = false
        }
    }

    private fun concealGlyphDisplay(item: DeskViewerGridItem) {
        ensureBackgroundThread {
            val hiddenIconUi = MaskedIcon(null, item.packageName, item.activityName, item.title, null)
            hiddenIconsDB.insert(hiddenIconUi)

            runOnUiThread {
                binding.allAppsBlockIxVw.root.onIconHidden(item)
            }
        }
    }

    private fun renameCell(homeScreenGridItem: DeskViewerGridItem) {
        RetitleItemPanel(this, homeScreenGridItem) {
            binding.mainShowGridIxVw.root.pullGridItems()
        }
    }

    val menuListener: ItemSettingsKeeper = object : ItemSettingsKeeper {
        override fun onAnyClick() {
            reinitBlockTouches()
        }

        override fun hide(gridItem: DeskViewerGridItem) {
            Launcher.events.onClick("menu_hide")
            // Our own icon: the host hides it from home and drawer at once (StarterBridge.settings icon flags).
            if (gridItem.packageName == packageName && Launcher.events.onHideHostApp(this@XCenterScene)) return
            concealGlyphDisplay(gridItem)
        }

        override fun rename(gridItem: DeskViewerGridItem) {
            Launcher.events.onClick("menu_rename")
            renameCell(gridItem)
        }

        override fun resize(gridItem: DeskViewerGridItem) {
            Launcher.events.onClick("menu_resize")
            binding.mainShowGridIxVw.root.widgetLongPressed(gridItem)
        }

        override fun appInfoUi(gridItem: DeskViewerGridItem) {
            Launcher.events.onClick("menu_app_info")
            fireAppletInfo(gridItem.packageName)
        }

        override fun remove(gridItem: DeskViewerGridItem) {
            Launcher.events.onClick("menu_remove")
            binding.mainShowGridIxVw.root.deleteAppIcon(gridItem)
        }

        override fun uninstall(gridItem: DeskViewerGridItem) {
            Launcher.events.onClick("menu_uninstall")
            deleteApplet(gridItem.packageName)
        }

        override fun onDismiss() {
            mOpenPopupMenu = null
            reinitBlockTouches()
        }
    }

    private class OwnTouchListener(
        private val flingListener: FlickKeeper,
    ) : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            (flingListener as XCenterScene).atriumViewerTapped(event.x, event.y)
            return super.onSingleTapUp(event)
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            (flingListener as XCenterScene).atriumViewerDoubleTapped(event.x, event.y)
            return super.onDoubleTap(event)
        }

        override fun onFling(
            event1: MotionEvent?,
            event2: MotionEvent,
            velocityX: Float,
            velocityY: Float,
        ): Boolean {
            if (System.currentTimeMillis() - mLastUpEvent < 500L) {
                return true
            }

            if (abs(velocityY) > abs(velocityX)) {
                if (velocityY > 0) {
                    flingListener.onFlingDown()
                } else {
                    flingListener.onFlingUp()
                }
            } else if (abs(velocityX) > abs(velocityY)) {
                // The finger's whole travel decides the direction, not its last instant: a flick
                // that slows down or wobbles as it lifts often reports a velocity against its own
                // drag, which read as a fling the other way and opened the opposite panel. Only a
                // real pull-back - the finger retreating past the touch slop from the farthest
                // point it reached - is a change of mind; that release is left to ACTION_UP,
                // which settles the page by distance.
                val travelX = if (event1 != null) event2.x - event1.x else velocityX
                val direction = if (travelX != 0f) travelX else velocityX
                if (sign(direction) != sign(velocityX)) {
                    val panel = flingListener as XCenterScene
                    if (panel.gestureRetreat(direction, event2.x) > panel.touchSlop()) {
                        return true
                    }
                }
                if (direction > 0) {
                    flingListener.onFlingRight()
                } else {
                    flingListener.onFlingLeft()
                }
            }

            return true
        }

        override fun onLongPress(event: MotionEvent) {
            (flingListener as XCenterScene).atriumViewerLongPressed(event.x, event.y)
        }
    }

    override fun onFlingUp() {
        if (mIgnoreYMoveEvents || !mDrawerEnabled) {
            return
        }

        if (!isGadgetsSectionExpanded()) {
            mIgnoreUpEvent = true
            displayFragment(binding.allAppsBlockIxVw)
        }
    }

    @SuppressLint("WrongConstant")
    override fun onFlingDown() {
        if (mIgnoreYMoveEvents) {
            return
        }

        mIgnoreUpEvent = true
        if (isAllProgramsSectionExpanded()) {
            concealFragment(binding.allAppsBlockIxVw)
        } else if (isGadgetsSectionExpanded()) {
            concealFragment(binding.widgetsBlockIxVw)
        } else {
            try {
                Class.forName("android.app.StatusBarManager")
                    .getMethod("expandNotificationsPanel")
                    .invoke(getSystemService("statusbar"))
            } catch (_: Exception) {
            }
        }
    }

    override fun onFlingRight() {
        if (mIgnoreXMoveEvents) {
            return
        }

        mIgnoreUpEvent = true

        // Paging owns the fling while a page is dragged or still settling (see flingSwipe). The
        // messages panel sits left of the home page, so it opens only from there, at rest;
        // anywhere else a rightward fling is just the previous page.
        if (binding.mainShowGridIxVw.root.flingSwipe(towardsNext = false)) {
            return
        }
        if (!isAllProgramsSectionExpanded() && !isGadgetsSectionExpanded() &&
            binding.mainShowGridIxVw.root.isOnFirstPage()
        ) {
            surfaceMsgsDeck()
        } else {
            binding.mainShowGridIxVw.root.prevPage(redraw = true)
        }
    }

    override fun onFlingLeft() {
        if (mIgnoreXMoveEvents) {
            return
        }

        mIgnoreUpEvent = true

        // see onFlingRight: the apps panel sits right of the last page and opens only from it
        if (binding.mainShowGridIxVw.root.flingSwipe(towardsNext = true)) {
            return
        }
        if (!isAllProgramsSectionExpanded() && !isGadgetsSectionExpanded() &&
            binding.mainShowGridIxVw.root.isOnLastPage()
        ) {
            surfaceLeftDeck()
        } else {
            binding.mainShowGridIxVw.root.nextPage(redraw = true)
        }
    }

    /** The launcher list, built by [AppScan]; the drawer and apps panel get it in stages on a cold start. */
    fun pullAllAppletHomebases(): ArrayList<WidgetCenter> = AppScan.scan(
        this,
        onQuick = { quick ->
            binding.allAppsBlockIxVw.root.gotLaunchers(quick)
            binding.startPageIxVw.root.gotLaunchers(quick)
        },
        onProgress = { partial ->
            binding.allAppsBlockIxVw.root.gotLaunchers(partial)
            binding.startPageIxVw.root.gotLaunchers(partial)
        },
    )

    private fun purgeSeededLookupBandIfNeeded() {
        if (launcherConfig.wasSearchBarPurged) {
            return
        }

        launcherConfig.wasSearchBarPurged = true
        try {
            homeScreenGridItemsDB.eraseCellsByClassName(PSEUDO_WIDGET_SEARCH)
        } catch (e: Exception) {
            Log.e("StarterTray", "Failed to remove the seeded search bar", e)
        }
    }

    private fun liveCopyPkg(): String? =
        Telephony.Sms.getDefaultSmsPackage(this) ?: packageName.takeIf { isDefaultSmsApp() }

    private fun dockMessagingPkg(): String? = liveCopyPkg()

    private fun dockableTitle(packageName: String?): String? {
        if (packageName.isNullOrEmpty()) return null
        return try {
            packageManager.getLaunchIntentForPackage(packageName) ?: return null
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
        } catch (_: Exception) {
            null
        }
    }

    private fun messagingDockCell(packageName: String?): DeskViewerGridItem? {
        if (packageName.isNullOrEmpty()) return null
        val ours = packageName == this.packageName
        val title = if (ours) applicationInfo.loadLabel(this.packageManager).toString() else dockableTitle(packageName) ?: return null
        return DeskViewerGridItem(
            id = null,
            left = DOCK_SMS_SLOT,
            top = launcherConfig.homeRowCount - 1,
            right = DOCK_SMS_SLOT,
            bottom = launcherConfig.homeRowCount - 1,
            page = 0,
            packageName = packageName,
            activityName = "",
            title = title,
            type = ITEM_TYPE_ICON,
            className = "",
            widgetId = -1,
            shortcutId = "",
            icon = null,
            docked = true,
            parentId = null
        )
    }

    private fun syncMessagingDockCell() {
        if (!launcherConfig.wasHomeScreenInit) return
        val wanted = dockMessagingPkg() ?: return

        val placed = launcherConfig.dockSmsPackage.ifEmpty { packageName }
        if (wanted == placed && launcherConfig.dockSmsPackage.isNotEmpty()) return
        ensureBackgroundThread {
            val items = homeScreenGridItemsDB.pullAllCells()
            if (wanted == placed) {
                val slotFilled = items.any { it.docked && it.left == DOCK_SMS_SLOT && it.parentId == null }
                if (!slotFilled) {
                    messagingDockCell(wanted)?.let { homeScreenGridItemsDB.insert(it) }
                    binding.mainShowGridIxVw.root.pullGridItems()
                }
                launcherConfig.dockSmsPackage = placed
                return@ensureBackgroundThread
            }
            items.filter { it.docked && it.type == ITEM_TYPE_ICON && it.packageName == placed }
                .forEach { homeScreenGridItemsDB.eraseByKey(it.id!!) }
            val slotTaken = items.any {
                it.docked && it.left == DOCK_SMS_SLOT && it.packageName != placed && it.parentId == null
            }
            if (!slotTaken) {
                messagingDockCell(wanted)?.let { homeScreenGridItemsDB.insert(it) }
            }
            launcherConfig.dockSmsPackage = wanted
            Timber.d("StarterTray: dock messaging slot $placed -> $wanted (slotTaken=$slotTaken)")
            binding.mainShowGridIxVw.root.pullGridItems()
        }
    }

    private fun pullMainAppletPackages() {
        val homeScreenGridItems = ArrayList<DeskViewerGridItem>()

        try {
            val defaultDialerPackage =
                (getSystemService(TELECOM_SERVICE) as TelecomManager).defaultDialerPackage
            dockableTitle(defaultDialerPackage)?.let { title ->
                val dialerIcon =
                    DeskViewerGridItem(
                        id = null,
                        left = 0,
                        top = launcherConfig.homeRowCount - 1,
                        right = 0,
                        bottom = launcherConfig.homeRowCount - 1,
                        page = 0,
                        packageName = defaultDialerPackage,
                        activityName = "",
                        title = title,
                        type = ITEM_TYPE_ICON,
                        className = "",
                        widgetId = -1,
                        shortcutId = "",
                        icon = null,
                        docked = true,
                        parentId = null
                    )
                homeScreenGridItems.add(dialerIcon)
            }
        } catch (_: Exception) {
        }

        messagingDockCell(dockMessagingPkg())?.let { item ->
            homeScreenGridItems.add(item)
            launcherConfig.dockSmsPackage = item.packageName
        }

        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, "http://".toUri())
            val resolveInfo =
                packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val defaultBrowserPackage = resolveInfo!!.activityInfo.packageName
            dockableTitle(defaultBrowserPackage)?.let { title ->
                val browserIcon =
                    DeskViewerGridItem(
                        id = null,
                        left = 2,
                        top = launcherConfig.homeRowCount - 1,
                        right = 2,
                        bottom = launcherConfig.homeRowCount - 1,
                        page = 0,
                        packageName = defaultBrowserPackage,
                        activityName = "",
                        title = title,
                        type = ITEM_TYPE_ICON,
                        className = "",
                        widgetId = -1,
                        shortcutId = "",
                        icon = null,
                        docked = true,
                        parentId = null
                    )
                homeScreenGridItems.add(browserIcon)
            }
        } catch (_: Exception) {
        }

        try {
            val cameraIntent = Intent("android.media.action.IMAGE_CAPTURE")
            val resolveInfo =
                packageManager.resolveActivity(cameraIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val defaultCameraPackage = resolveInfo!!.activityInfo.packageName
            dockableTitle(defaultCameraPackage)?.let { title ->
                val cameraIcon =
                    DeskViewerGridItem(
                        id = null,
                        left = 3,
                        top = launcherConfig.homeRowCount - 1,
                        right = 3,
                        bottom = launcherConfig.homeRowCount - 1,
                        page = 0,
                        packageName = defaultCameraPackage,
                        activityName = "",
                        title = title,
                        type = ITEM_TYPE_ICON,
                        className = "",
                        widgetId = -1,
                        shortcutId = "",
                        icon = null,
                        docked = true,
                        parentId = null
                    )
                homeScreenGridItems.add(cameraIcon)
            }
        } catch (_: Exception) {
        }

        if (launcherConfig.dockColumnCount >= 5) {
            dockableTitle(PLAY_STORE_PACKAGE)?.let { title ->
                homeScreenGridItems.add(
                    DeskViewerGridItem(
                        id = null,
                        left = 4,
                        top = launcherConfig.homeRowCount - 1,
                        right = 4,
                        bottom = launcherConfig.homeRowCount - 1,
                        page = 0,
                        packageName = PLAY_STORE_PACKAGE,
                        activityName = "",
                        title = title,
                        type = ITEM_TYPE_ICON,
                        className = "",
                        widgetId = -1,
                        shortcutId = "",
                        icon = null,
                        docked = true,
                        parentId = null
                    )
                )
            }
        }

        homeScreenGridItemsDB.insertAll(homeScreenGridItems)
    }

    fun processWidgetBinding(
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        appWidgetInfo: AppWidgetProviderInfo,
        callback: (canBind: Boolean) -> Unit,
    ) {
        mActionOnCanBindWidget = null
        val canCreateWidget =
            appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, appWidgetInfo.provider)
        if (canCreateWidget) {
            callback(true)
        } else {
            mActionOnCanBindWidget = callback
            Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider)
                startActivityForResult(this, REQUEST_ALLOW_BINDING_WIDGET)
            }
        }
    }

    fun processWidgetTuneViewer(
        appWidgetHost: AppWidgetHost,
        appWidgetId: Int,
        callback: (canBind: Boolean) -> Unit,
    ) {
        mActionOnWidgetConfiguredWidget = callback
        try {
            appWidgetHost.startAppWidgetConfigureActivityForResult(
                this,
                appWidgetId,
                0,
                REQUEST_CONFIGURE_WIDGET,
                null
            )
        } catch (e: RuntimeException) {
            Timber.w(e, "StarterTray: widget configure activity failed to start")
            mActionOnWidgetConfiguredWidget = null
            callback(false)
        }
    }

    fun processShorcutCreation(
        activityInfo: ActivityInfo,
        callback: (shortcutId: String, label: String, icon: Drawable) -> Unit,
    ) {
        mActionOnAddShortcut = callback
        val componentName = ComponentName(activityInfo.packageName, activityInfo.name)
        Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
            component = componentName
            runCatching { startActivityForResult(this, REQUEST_CREATE_SHORTCUT) }
                .onFailure { Timber.w(it, "StarterTray: create-shortcut activity failed") }
        }
    }

    private fun slabBackgroundColor(fragment: ViewBinding): Int =
        if (fragment is HpAllAppsZoneBinding) {
            getColor(R.color.launcher_all_app_bg)
        } else {
            getProperBackgroundColor()
        }

    private fun sideDeckBackdropTint(panel: View): Int =
        (panel.background as? ColorDrawable)?.color ?: getProperBackgroundColor()

    private fun livePanelTint(): Int? = when {
        isAllProgramsSectionExpanded() -> slabBackgroundColor(binding.allAppsBlockIxVw)
        isGadgetsSectionExpanded() -> slabBackgroundColor(binding.widgetsBlockIxVw)
        isLeftStageExpanded() -> sideDeckBackdropTint(binding.startPageIxVw.root)
        isMsgsStageExpanded() -> sideDeckBackdropTint(binding.msgsPageIxVw.root)
        else -> null
    }

    private fun reloadStatusBandGlyphs(backgroundColor: Int? = null) {
        val isLightBackground = when {
            backgroundColor != null -> backgroundColor.getContrastColor() == DARK_GREY
            else -> false
        }
        window.insetsController().apply {
            isAppearanceLightStatusBars = isLightBackground
            isAppearanceLightNavigationBars = isLightBackground
        }
    }

}
