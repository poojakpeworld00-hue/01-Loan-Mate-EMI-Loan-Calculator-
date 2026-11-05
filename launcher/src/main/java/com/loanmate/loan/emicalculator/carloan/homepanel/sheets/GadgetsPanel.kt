package com.loanmate.loan.emicalculator.carloan.homepanel.sheets

import android.annotation.SuppressLint
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.max
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.getProperPrimaryColor
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.extensions.toast
import org.fossify.commons.helpers.ensureBackgroundThread
import com.loanmate.loan.emicalculator.carloan.homepanel.R
import com.loanmate.loan.emicalculator.carloan.homepanel.pages.XCenterScene
import com.loanmate.loan.emicalculator.carloan.homepanel.adapters.GadgetsLoader
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.HpGadgetsZoneBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.launcherConfig
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.pullInitialBayDims
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.prepRackBackdrop
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.ITEM_TYPE_SHORTCUT
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.ITEM_TYPE_WIDGET
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.PSEUDO_WIDGET_CLOCK
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.PSEUDO_WIDGET_PREFIX
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.PSEUDO_WIDGET_SEARCH
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.GadgetsBrickKeeper
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetGadget
import com.loanmate.loan.emicalculator.carloan.homepanel.models.DeskViewerGridItem
import com.loanmate.loan.emicalculator.carloan.homepanel.models.GadgetsStackItem
import com.loanmate.loan.emicalculator.carloan.homepanel.models.GadgetsStackLogsSaver
import com.loanmate.loan.emicalculator.carloan.homepanel.models.GadgetsStackCluster

class GadgetsPanel(context: Context, attributeSet: AttributeSet) :
    CenterPanel<HpGadgetsZoneBinding>(context, attributeSet), GadgetsBrickKeeper {
    private var lastTouchCoords = Pair(0f, 0f)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    var touchDownY = -1
    var ignoreTouches = false
    private var widgets = emptyList<WidgetGadget>()

    @SuppressLint("ClickableViewAccessibility")
    override fun setupFragment(activity: XCenterScene) {
        this.activity = activity
        this.binding = HpGadgetsZoneBinding.bind(this)
        obtainProgGadgets()

        binding.widgetsRollIxVw.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN && binding.queryBarIxVw.hasFocus()) {
                binding.queryBarIxVw.binding.topToolbarSearch.clearFocus()
                activity?.hideKeyboard()
            }

            return@setOnTouchListener false
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        prepRackBackdrop()
    }

    fun onConfigurationChanged() {
        binding.widgetsRollIxVw.scrollToPosition(0)
        primeScreens()

        if (widgets.isNotEmpty()) {
            splitGadgetsByApplets()
        } else {
            obtainProgGadgets()
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onInterceptTouchEvent(event)
        }

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            touchDownY = event.y.toInt()
            lastTouchCoords = Pair(event.x, event.y)
            return false
        }

        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            touchDownY = -1
            return false
        }

        if (ignoreTouches) {
            if (lastTouchCoords.first != event.x || lastTouchCoords.second != event.y) {
                touchDownY = -1
                return true
            }
        }

        lastTouchCoords = Pair(event.x, event.y)
        var shouldIntercept = false

        if (touchDownY != -1) {
            shouldIntercept =
                event.y.toInt() - touchDownY > touchSlop && binding.widgetsRollIxVw.computeVerticalScrollOffset() == 0
            if (shouldIntercept) {
                if (binding.queryBarIxVw.hasFocus()) {
                    activity?.hideKeyboard()
                }
                activity?.beginHandlingTouches(touchDownY)
                touchDownY = -1
            }
        }

        return shouldIntercept
    }

    /**
     * A widget's preview decoded no larger than [maxSide]. `loadPreviewImage` decodes the full
     * resource, and some apps ship multi-megapixel previews (one is 3780x4099, ~62 MB) - every widget
     * is loaded at start-up, so that churn ran GC pauses into the drawer's first scroll. Bitmap
     * previews are decoded straight at the bounded size; anything else (a vector, a drawable XML)
     * goes through the framework as before.
     */
    private fun retrievePreview(info: AppWidgetProviderInfo, maxSide: Int): Drawable? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && info.previewImage != 0) {
            runCatching {
                val res = context.packageManager.getResourcesForApplication(info.provider.packageName)
                return ImageDecoder.decodeDrawable(ImageDecoder.createSource(res, info.previewImage)) { decoder, image, _ ->
                    // A hardware bitmap (the default) cannot be read back, and both the list's
                    // Glide transform and the drag shadow do; such a preview drew blank.
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    val longest = max(image.size.width, image.size.height)
                    if (longest > maxSide) {
                        val scale = maxSide.toFloat() / longest
                        decoder.setTargetSize(
                            (image.size.width * scale).toInt().coerceAtLeast(1),
                            (image.size.height * scale).toInt().coerceAtLeast(1),
                        )
                    }
                }
            }
        }
        return info.loadPreviewImage(context, resources.displayMetrics.densityDpi)
    }

    /** True once the widget list has been built at least once. */
    fun hasGadgets() = binding.widgetsRollIxVw.adapter != null

    @SuppressLint("WrongConstant")
    fun obtainProgGadgets() {
        ensureBackgroundThread {
            runCatching {
                var appWidgets = ArrayList<WidgetGadget>()
                appWidgets.addAll(pullPseudoGadgets())
                val previewMaxPx = resources.getDimensionPixelSize(R.dimen.launcher_widget_preview_size) * PREVIEW_OVERSAMPLE
                val manager = AppWidgetManager.getInstance(context)
                val packageManager = context.packageManager
                val infoList = manager.installedProviders
                for (info in infoList) {
                    val appPackageName = info.provider.packageName
                    val appMetadata = pullAppletInfoFromPkg(appPackageName) ?: continue
                    val appTitle = appMetadata.appTitle
                    val appIcon = appMetadata.appIcon
                    val widgetTitleUi = info.loadLabel(packageManager)
                    val widgetPreviewImage = retrievePreview(info, previewMaxPx) ?: appIcon
                    val cellSize = context.pullInitialBayDims(info, info.minWidth, info.minHeight)
                    val widthCells = cellSize.width
                    val heightCells = cellSize.height
                    val className = info.provider.className
                    val widget =
                        WidgetGadget(
                            appPackageName = appPackageName,
                            appTitle = appTitle,
                            appIcon = appIcon,
                            widgetTitleUi = widgetTitleUi,
                            widgetPreviewImage = widgetPreviewImage,
                            widthCells = widthCells,
                            heightCells = heightCells,
                            isShortcut = false,
                            className = className,
                            providerInfo = info,
                            activityInfo = null
                        )
                    appWidgets.add(widget)
                }

                val intent = Intent(Intent.ACTION_CREATE_SHORTCUT, null)
                val list =
                    packageManager.queryIntentActivities(intent, PackageManager.PERMISSION_GRANTED)
                for (info in list) {
                    val componentInfo = info.activityInfo.applicationInfo
                    val appTitle = componentInfo.loadLabel(packageManager).toString()
                    val appPackageName = componentInfo.packageName
                    val appMetadata = pullAppletInfoFromPkg(appPackageName) ?: continue
                    val appIcon = appMetadata.appIcon
                    val widgetTitleUi = info.loadLabel(packageManager).toString()
                    val widgetPreviewImage = packageManager.getDrawable(
                        componentInfo.packageName,
                        info.iconResource,
                        componentInfo
                    )
                    val widget = WidgetGadget(
                        appPackageName = appPackageName,
                        appTitle = appTitle,
                        appIcon = appIcon,
                        widgetTitleUi = widgetTitleUi,
                        widgetPreviewImage = widgetPreviewImage,
                        widthCells = 0,
                        heightCells = 0,
                        isShortcut = true,
                        className = "",
                        providerInfo = null,
                        activityInfo = info.activityInfo
                    )
                    appWidgets.add(widget)
                }

                appWidgets = appWidgets.sortedWith(
                    compareBy({ it.appTitle },
                        { it.appPackageName },
                        { it.widgetTitleUi })
                ).toMutableList() as ArrayList<WidgetGadget>
                widgets = appWidgets
                activity?.runOnUiThread {
                    splitGadgetsByApplets()
                }
            }.onFailure { android.util.Log.w("GadgetsPanel", "getAppWidgets failed", it) }
        }
    }

    private fun pullPseudoGadgets(): List<WidgetGadget> {
        val appMetadata = pullAppletInfoFromPkg(context.packageName) ?: return emptyList()
        return listOf(
            PSEUDO_WIDGET_CLOCK to Triple(R.string.launcher_pseudo_widget_clock, 4, 2),
            PSEUDO_WIDGET_SEARCH to Triple(R.string.launcher_pseudo_widget_search_bar, 4, 1)
        ).map { (className, spec) ->
            val (titleId, widthCells, heightCells) = spec
            WidgetGadget(
                appPackageName = context.packageName,
                appTitle = appMetadata.appTitle,
                appIcon = appMetadata.appIcon,
                widgetTitleUi = context.getString(titleId),
                widgetPreviewImage = appMetadata.appIcon,
                widthCells = widthCells,
                heightCells = heightCells,
                isShortcut = false,
                className = className,
                providerInfo = null,
                activityInfo = null
            )
        }
    }

    private fun splitGadgetsByApplets() {
        val searchQuery = binding.queryBarIxVw.getCurrentQuery()
        val filteredWidgets = if (searchQuery.isNotEmpty()) {
            widgets.filter { widget ->
                widget.appTitle.normalizeString().contains(searchQuery.normalizeString(), ignoreCase = true) ||
                        widget.widgetTitleUi.toString().normalizeString()
                            .contains(searchQuery.normalizeString(), ignoreCase = true)
            }
        } else {
            widgets
        }

        var currentAppPackageName = ""
        val widgetListItems = ArrayList<GadgetsStackItem>()
        var currentAppWidgets = ArrayList<WidgetGadget>()
        filteredWidgets.forEach { appWidget ->
            if (appWidget.appPackageName != currentAppPackageName) {
                if (widgetListItems.isNotEmpty()) {
                    widgetListItems.add(GadgetsStackLogsSaver(currentAppWidgets))
                    currentAppWidgets = ArrayList()
                }

                widgetListItems.add(GadgetsStackCluster(appWidget.appTitle, appWidget.appIcon))
            }

            currentAppWidgets.add(appWidget)
            currentAppPackageName = appWidget.appPackageName
        }

        if (widgetListItems.isNotEmpty()) {
            widgetListItems.add(GadgetsStackLogsSaver(currentAppWidgets))
        }

        prepBridge(widgetListItems)
    }

    private fun prepBridge(widgetsListItems: ArrayList<GadgetsStackItem>) {
        activity?.runOnUiThread {
            val currAdapter = binding.widgetsRollIxVw.adapter
            if (currAdapter == null) {
                GadgetsLoader(activity!!, widgetsListItems, this) {
                    context.toast(R.string.launcher_touch_hold_widget)
                    ignoreTouches = false
                    touchDownY = -1
                }.apply {
                    binding.widgetsRollIxVw.adapter = this
                }
            } else {
                (currAdapter as GadgetsLoader).refreshItems(widgetsListItems)
            }
        }
    }

    fun primeScreens() {
        if (activity == null) {
            return
        }

        binding.widgetsFastscrollerIxVw.updateColors(context.getProperPrimaryColor())
        (binding.widgetsRollIxVw.adapter as? GadgetsLoader)?.reloadLabelTint(context.getProperTextColor())
        prepRackBackdrop()

        binding.queryBarIxVw.requireToolbar().beGone()
        binding.queryBarIxVw.updateColors()
        binding.queryBarIxVw.setupMenu()
        binding.queryBarIxVw.onSearchTextChangedListener = {
            splitGadgetsByApplets()
        }
    }

    private fun pullAppletInfoFromPkg(packageName: String): GadgetsStackCluster? {
        try {
            val appInfoUi = activity!!.packageManager.getApplicationInfo(packageName, 0)
            val appTitle = activity!!.packageManager.getApplicationLabel(appInfoUi).toString()

            val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val activityList = launcher.getActivityList(packageName, Process.myUserHandle())
            var appIcon = activityList.firstOrNull()?.getBadgedIcon(0)

            if (appIcon == null) {
                appIcon = context.packageManager.getApplicationIcon(packageName)
            }

            if (appTitle.isNotEmpty()) {
                return GadgetsStackCluster(appTitle, appIcon)
            }
        } catch (ignored: Exception) {
        } catch (error: Error) {
        }

        return null
    }

    override fun onWidgetLongPressed(appWidget: WidgetGadget) {
        if (appWidget.heightCells > context.launcherConfig.homeRowCount - 1 || appWidget.widthCells > context.launcherConfig.homeColumnCount) {
            context.showErrorToast(context.getString(R.string.launcher_widget_too_big))
            return
        }

        val type = if (appWidget.isShortcut) {
            ITEM_TYPE_SHORTCUT
        } else {
            ITEM_TYPE_WIDGET
        }

        val gridItem = DeskViewerGridItem(
            id = null,
            left = -1,
            top = -1,
            right = -1,
            bottom = -1,
            page = 0,
            packageName = appWidget.appPackageName,
            activityName = "",

            title = if (appWidget.className.startsWith(PSEUDO_WIDGET_PREFIX)) {
                appWidget.widgetTitleUi
            } else {
                ""
            },
            type = type,
            className = appWidget.className,
            widgetId = -1,
            shortcutId = "",
            icon = null,
            docked = false,
            parentId = null,
            drawable = appWidget.widgetPreviewImage,
            providerInfo = appWidget.providerInfo,
            activityInfo = appWidget.activityInfo,
            widthCells = appWidget.widthCells,
            heightCells = appWidget.heightCells
        )

        activity?.widgetLongPressedOnRoll(gridItem)
        ignoreTouches = true
    }

    private companion object {
        /** Previews are kept at twice the list's tile so the drag shadow on the home screen stays sharp. */
        const val PREVIEW_OVERSAMPLE = 2
    }
}
