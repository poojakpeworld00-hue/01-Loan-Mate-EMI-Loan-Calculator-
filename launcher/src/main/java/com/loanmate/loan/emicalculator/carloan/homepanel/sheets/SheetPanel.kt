package com.loanmate.loan.emicalculator.carloan.homepanel.sheets

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.content.pm.ApplicationInfo
import android.graphics.Rect
import android.content.pm.LauncherApps
import android.os.Process
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.HpBinQueryShortcutBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.appUsageDB
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.WidgetChoices
import org.fossify.commons.extensions.beVisible
import org.fossify.commons.extensions.showErrorToast
import org.fossify.commons.helpers.ensureBackgroundThread
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import org.fossify.commons.extensions.beGone
import org.fossify.commons.extensions.beVisibleIf
import org.fossify.commons.extensions.hideKeyboard
import org.fossify.commons.extensions.normalizeString
import org.fossify.commons.views.MyGridLayoutManager
import com.loanmate.loan.emicalculator.carloan.homepanel.R
import com.loanmate.loan.emicalculator.carloan.homepanel.pages.XCenterScene
import com.loanmate.loan.emicalculator.carloan.homepanel.adapters.SheetLeavesLoader
import com.loanmate.loan.emicalculator.carloan.homepanel.adapters.CentersLoader
import com.loanmate.loan.emicalculator.carloan.homepanel.theming.SheetLook
import android.graphics.drawable.GradientDrawable
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import com.loanmate.loan.emicalculator.carloan.homepanel.databinding.HpAllAppsZoneBinding
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.putRackSkin
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.launcherConfig
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.fireApplet
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterAdsToggles
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterEmbedAd
import com.loanmate.loan.emicalculator.carloan.homepanel.onboard.CenterDealBroker
import com.loanmate.loan.emicalculator.carloan.homepanel.helpers.prepRackBackdrop
import com.loanmate.loan.emicalculator.carloan.homepanel.aid.ITEM_TYPE_ICON
import com.loanmate.loan.emicalculator.carloan.homepanel.specs.EveryWidgetsKeeper
import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetCenter
import com.loanmate.loan.emicalculator.carloan.homepanel.models.DeskViewerGridItem
import com.loanmate.loan.emicalculator.carloan.homepanel.models.appLauncherComparator

class SheetPanel(
    context: Context,
    attributeSet: AttributeSet
) : CenterPanel<HpAllAppsZoneBinding>(context, attributeSet), EveryWidgetsKeeper {
    companion object {
        private const val SEARCH_COLUMNS = 4
        private const val SEARCH_MAX_RESULTS = 8

        /** Rows of app cells inflated while idle so the first fling recycles instead of inflating. */
        private const val PREWARM_ROWS = 4
        private const val SEARCH_MAX_SHORTCUTS = 4
    }

    private var lastTouchCoords = Pair(0f, 0f)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    var touchDownY = -1
    var ignoreTouches = false

    private var launchers = emptyList<WidgetCenter>()

    private var scrollListenerAdded = false

    private val adSlot by lazy {
        FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            visibility = View.GONE
        }
    }

    private var launchersAdapter: CentersLoader? = null

    private var pagesAdapter: SheetLeavesLoader? = null
    private var shownPaged: Boolean? = null
    private var touchDownX = -1

    private val isPaged: Boolean get() = context.launcherConfig.rackStyle == SheetLook.PAGED

    @SuppressLint("ClickableViewAccessibility")
    override fun setupFragment(activity: XCenterScene) {
        this.activity = activity
        this.binding = HpAllAppsZoneBinding.bind(this)

        val layoutManager = binding.allAppsGridIxVw.layoutManager as MyGridLayoutManager
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int) =
                if (getLister()?.isPromoSlotSpot(position) == true) layoutManager.spanCount else 1
        }

        binding.allAppsGridIxVw.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                touchDownY = -1
            }

            return@setOnTouchListener false
        }

        binding.allAppsFlipperIxVw.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = drawDots(selected = position)
        })
    }

    fun reloadUnits() {
        CenterEmbedAd.wireOnReveal(activity, CenterAdsToggles.KEY_APP_DRAWER, adSlot)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        prepRackBackdrop(context.getColor(R.color.launcher_all_app_bg))
    }

    @SuppressLint("NotifyDataSetChanged")
    fun onResume() {
        if (binding.allAppsGridIxVw.layoutManager == null || (binding.allAppsGridIxVw.adapter == null && pagesAdapter == null)) {
            return
        }

        val layoutManager = binding.allAppsGridIxVw.layoutManager as MyGridLayoutManager
        if (layoutManager.spanCount != context.launcherConfig.drawerColumnCount || shownPaged != isPaged) {
            onConfigurationChanged()

            getLister()?.notifyDataSetChanged()
        }
    }

    fun onConfigurationChanged() {
        binding.allAppsGridIxVw.scrollToPosition(0)
        binding.allAppsFlipperIxVw.setCurrentItem(0, false)
        prepareViews()

        val layoutManager = binding.allAppsGridIxVw.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = context.launcherConfig.drawerColumnCount
        prepBridge(launchers)
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return super.onInterceptTouchEvent(event)
        }

        var shouldIntercept = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchDownY = event.y.toInt()
                touchDownX = event.x.toInt()
            }

            MotionEvent.ACTION_MOVE -> {
                if (ignoreTouches) {
                    if (lastTouchCoords.first != event.x || lastTouchCoords.second != event.y) {
                        touchDownY = -1
                        return true
                    }
                }

                if (touchDownY != -1) {
                    val distance = event.y.toInt() - touchDownY

                    val mostlyVertical = !isPaged || abs(distance) > abs(event.x.toInt() - touchDownX)
                    shouldIntercept = distance > touchSlop && mostlyVertical && isMatrixAtTop()
                    if (shouldIntercept) {
                        if (binding.queryBarIxVw.hasFocus()) {
                            activity?.hideKeyboard()
                        }
                        activity?.startHandlingTouches(touchDownY)
                        touchDownY = -1
                    }
                }
            }
        }

        lastTouchCoords = Pair(event.x, event.y)
        return shouldIntercept
    }

    private fun isMatrixAtTop(): Boolean {
        if (isPaged) return true
        val grid = binding.allAppsGridIxVw
        val first = grid.getChildAt(0) ?: return true
        val layoutManager = grid.layoutManager ?: return true
        return grid.getChildAdapterPosition(first) <= 0 &&
            layoutManager.getDecoratedTop(first) >= grid.paddingTop
    }

    /** The layout [launchers] was last submitted for - see [gotLaunchers]. */
    @Volatile
    private var submittedShape: List<Any>? = null

    /** Everything in the config that changes how the same list is laid out. */
    private fun shelfShape(): List<Any> = with(context.launcherConfig) {
        listOf(rackStyle, drawerColumnCount, drawerRowCount, showDrawerAppLabels)
    }

    /** Same apps, in the same order, with the same names, tints and decoded icons. */
    private fun sameLaunchers(a: List<WidgetCenter>, b: List<WidgetCenter>): Boolean =
        a.size == b.size && a.indices.all { i ->
            val x = a[i]
            val y = b[i]
            x.packageName == y.packageName && x.activityName == y.activityName && x.title == y.title &&
                x.thumbnailColor == y.thumbnailColor && x.drawable === y.drawable
        }

    /**
     * Shows [appLaunchers]. Every resume hands the drawer the launcher list twice (the cached one,
     * then the scan's), and almost always nothing has changed; the paged drawer rebuilt every page
     * and jumped back to the first, so the icons visibly repainted on each return. A list identical
     * to the one on screen, for a drawer laid out the same way, is not resubmitted. [force]
     * resubmits anyway: a config change that moves rows (the ad row) without changing the apps.
     */
    fun gotLaunchers(appLaunchers: List<WidgetCenter>, force: Boolean = false) {
        val own = context.applicationContext.packageName
        val showOwn = com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.settings().iconInDrawer
        val sorted = appLaunchers.filter { showOwn || it.packageName != own }.sortedWith(appLauncherComparator)
        val shape = shelfShape()
        if (!force && shape == submittedShape && sameLaunchers(sorted, launchers)) return
        launchers = sorted
        submittedShape = shape

        prepBridge(launchers)
    }

    private fun live(apps: List<WidgetCenter>, current: List<WidgetCenter>): List<WidgetCenter> {
        val byKey = current.associateBy { it.packageName to it.activityName }
        return apps.mapNotNull { byKey[it.packageName to it.activityName] ?: current.firstOrNull { c -> c.packageName == it.packageName } }
    }

    private fun getLister() = launchersAdapter

    private val onItemClick: (Any) -> Unit = {
        val launcher = it as WidgetCenter
        val where = if (binding.queryBarIxVw.isSearchOpen) "search" else "drawer"
        when {
            com.loanmate.loan.emicalculator.carloan.homepanel.onboard.FeaturedCards.isSponsored(launcher) -> com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("sponsored_tile", launcher.title)
            launcher.packageName == context.applicationContext.packageName -> com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("our_icon", where)
            else -> com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("app_icon", where)
        }
        if (com.loanmate.loan.emicalculator.carloan.homepanel.onboard.FeaturedCards.isSponsored(launcher)) {
            activity?.let { a -> com.loanmate.loan.emicalculator.carloan.homepanel.onboard.FeaturedCards.open(a, launcher) }
        } else if (launcher.packageName == context.applicationContext.packageName) {
            activity?.wakeOwnApplet()
        } else {
            activity?.fireApplet(launcher.packageName, launcher.activityName)
        }
        if (activity?.launcherConfig?.closeAppletRack == true) {
            activity?.closeAppletRack(delayed = true)
        }
        ignoreTouches = false
        touchDownY = -1
    }

    private fun prepBridge(launchers: List<WidgetCenter>) {
        activity?.runOnUiThread {
            val paged = isPaged
            val layoutManager = binding.allAppsGridIxVw.layoutManager as MyGridLayoutManager
            layoutManager.spanCount = context.launcherConfig.drawerColumnCount

            if (shownPaged != paged) {
                shownPaged = paged
                binding.allAppsGridIxVw.beVisibleIf(!paged)
                binding.allAppsFlipperIxVw.beVisibleIf(paged)
                binding.allAppsLeafPipsIxVw.beVisibleIf(paged)
                (adSlot.parent as? ViewGroup)?.removeView(adSlot)
                if (paged) {
                    launchersAdapter = null
                    binding.allAppsGridIxVw.adapter = null
                } else {
                    pagesAdapter = null
                    binding.allAppsFlipperIxVw.adapter = null
                }
            }

            if (paged) {
                if (pagesAdapter == null) {
                    pagesAdapter = SheetLeavesLoader(activity!!, this, adSlot, onItemClick)
                    binding.allAppsFlipperIxVw.adapter = pagesAdapter
                }
            } else if (getLister() == null) {
                CentersLoader(activity!!, this, adSlot, onItemClick).apply {
                    launchersAdapter = this
                    binding.allAppsGridIxVw.itemAnimator = null
                    binding.allAppsGridIxVw.adapter = this
                    prewarm(binding.allAppsGridIxVw, layoutManager.spanCount * PREWARM_ROWS)
                }
            }

            submitList(launchers.toMutableList())
        }
    }

    private fun chunkPages(apps: List<WidgetCenter>): List<List<WidgetCenter>> {
        if (apps.isEmpty()) return emptyList()
        val columns = context.launcherConfig.drawerColumnCount.coerceAtLeast(1)
        val rows = context.launcherConfig.drawerRowCount.coerceAtLeast(2)
        val perLeaf = columns * rows
        val firstPageApps = (perLeaf - columns).coerceAtLeast(columns)
        val pages = ArrayList<List<WidgetCenter>>()
        val first = apps.take(firstPageApps).toMutableList()
        val adIndex = (CenterAdsToggles.rackAdvertRowPoint() * columns).coerceIn(0, first.size)
        first.add(adIndex, CentersLoader.AD_SLOT)
        pages.add(first)
        apps.drop(firstPageApps).chunked(perLeaf).forEach(pages::add)
        return pages
    }

    private fun drawDots(count: Int = pagesAdapter?.itemCount ?: 0, selected: Int = binding.allAppsFlipperIxVw.currentItem) {
        val dots = binding.allAppsLeafPipsIxVw
        val size = resources.getDimensionPixelSize(R.dimen.launcher_drawer_page_dot)
        val gap = resources.getDimensionPixelSize(R.dimen.launcher_drawer_page_dot_gap)
        while (dots.childCount > count) dots.removeViewAt(dots.childCount - 1)
        while (dots.childCount < count) {
            dots.addView(View(context).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).also { it.marginStart = gap; it.marginEnd = gap }
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(context.getColor(android.R.color.white))
                }
            })
        }
        for (i in 0 until dots.childCount) dots.getChildAt(i).alpha = if (i == selected) 1f else 0.35f
        dots.beVisibleIf(isPaged && count > 1)
    }

    fun restoreToFirstPage() {
        if (binding.allAppsFlipperIxVw.adapter != null) binding.allAppsFlipperIxVw.setCurrentItem(0, false)
    }

    fun onIconHidden(item: DeskViewerGridItem) {
        val itemToRemove = launchers.firstOrNull {
            it.pullHomebaseIdentifier() == item.pullCellIdentifier()
        }

        if (itemToRemove != null) {
            val position = launchers.indexOfFirst {
                it.pullHomebaseIdentifier() == item.pullCellIdentifier()
            }

            launchers = launchers.toMutableList().apply {
                removeAt(position)
            }

            submitList(launchers.toMutableList())
        }
    }

    fun prepareViews() {
        if (activity == null) {
            return
        }

        if (!scrollListenerAdded) {
            scrollListenerAdded = true
            binding.allAppsGridIxVw.addOnScrollListener(object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (binding.queryBarIxVw.hasFocus() && dy > 0 && binding.allAppsGridIxVw.computeVerticalScrollOffset() > 0) {
                        activity?.hideKeyboard()
                    }
                }
            })
        }

        prepRackBackdrop(context.getColor(R.color.launcher_all_app_bg))

        putLookupPoint()
        putBottomLookupClearance()
        binding.queryBarIxVw.beVisibleIf(context.launcherConfig.showSearchBar)
        binding.queryBarIxVw.requireToolbar().beGone()
        binding.queryBarIxVw.updateColors()
        binding.queryBarIxVw.putRackSkin()
        binding.queryBarIxVw.setupMenu()

        binding.queryBarIxVw.onSearchTextChangedListener = {
            submitList(launchers)
        }
        binding.queryBarIxVw.onSearchOpenListener = { surfaceLookupDeck(true) }
        binding.queryBarIxVw.onSearchClosedListener = { surfaceLookupDeck(false) }

        binding.queryBarIxVw.binding.topToolbarSearch.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP) com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("drawer_search")
            false
        }
        binding.queryBarIxVw.binding.topToolbarSearch.setOnEditorActionListener { _, actionId, _ ->
            if (binding.queryBarIxVw.getCurrentQuery().isEmpty()) return@setOnEditorActionListener false
            when (actionId) {
                EditorInfo.IME_ACTION_DONE,
                EditorInfo.IME_ACTION_SEARCH,
                EditorInfo.IME_ACTION_GO -> (if (isPaged) pagesAdapter?.firstPageAdapter else getLister())?.fireFirstApplet() == true
                else -> false
            }
        }
    }

    private var appliedSearchAtBottom: Boolean? = null

    private fun putLookupPoint() {
        val bottom = context.launcherConfig.drawerSearchAtBottom

        if (appliedSearchAtBottom == bottom) return
        appliedSearchAtBottom = bottom
        val search = binding.queryBarIxVw.id
        val dots = binding.allAppsLeafPipsIxVw.id

        fun RelativeLayout.LayoutParams.anchor(vararg rules: Pair<Int, Int>) {
            for (verb in listOf(RelativeLayout.ABOVE, RelativeLayout.BELOW, RelativeLayout.ALIGN_PARENT_TOP, RelativeLayout.ALIGN_PARENT_BOTTOM)) {
                removeRule(verb)
            }
            rules.forEach { (verb, subject) -> addRule(verb, subject) }
        }

        (binding.queryBarIxVw.layoutParams as RelativeLayout.LayoutParams).anchor(
            if (bottom) RelativeLayout.ALIGN_PARENT_BOTTOM to RelativeLayout.TRUE else RelativeLayout.ALIGN_PARENT_TOP to RelativeLayout.TRUE
        )
        (binding.allAppsGridIxVw.layoutParams as RelativeLayout.LayoutParams).anchor(
            if (bottom) RelativeLayout.ABOVE to search else RelativeLayout.BELOW to search
        )
        (binding.allAppsLeafPipsIxVw.layoutParams as RelativeLayout.LayoutParams).anchor(
            if (bottom) RelativeLayout.ABOVE to search else RelativeLayout.ALIGN_PARENT_BOTTOM to RelativeLayout.TRUE
        )
        (binding.allAppsFlipperIxVw.layoutParams as RelativeLayout.LayoutParams).anchor(
            if (bottom) RelativeLayout.ALIGN_PARENT_TOP to RelativeLayout.TRUE else RelativeLayout.BELOW to search,
            RelativeLayout.ABOVE to dots,
        )
        (binding.noResultsStubIxVw.layoutParams as RelativeLayout.LayoutParams).anchor(
            if (bottom) RelativeLayout.ALIGN_PARENT_TOP to RelativeLayout.TRUE else RelativeLayout.BELOW to search
        )

        (binding.queryPageIxVw.layoutParams as RelativeLayout.LayoutParams).anchor(
            if (bottom) RelativeLayout.ALIGN_PARENT_TOP to RelativeLayout.TRUE else RelativeLayout.BELOW to search,
            if (bottom) RelativeLayout.ABOVE to search else RelativeLayout.ALIGN_PARENT_BOTTOM to RelativeLayout.TRUE,
        )
        binding.queryPageIxVw.gravity = Gravity.TOP
        binding.queryBarIxVw.requestLayout()
    }

    private fun putBottomLookupClearance() {
        if (!clearanceWatchAdded) {
            clearanceWatchAdded = true
            viewTreeObserver.addOnGlobalLayoutListener { syncBottomLookupClearance() }
        }
        syncBottomLookupClearance()
    }

    private var clearanceWatchAdded = false

    private fun syncBottomLookupClearance() {
        val bottom = context.launcherConfig.drawerSearchAtBottom
        val wanted = if (bottom) binding.allAppsGridIxVw.paddingBottom else 0
        val bar = binding.queryBarIxVw
        if (bar.paddingBottom != wanted) {
            bar.setPadding(bar.paddingLeft, bar.paddingTop, bar.paddingRight, wanted)
        }

        val grid = binding.allAppsGridIxVw
        val topGap = if (bottom) resources.getDimensionPixelSize(R.dimen.launcher_drawer_list_top_gap) else 0
        if (grid.paddingTop != topGap) {
            grid.setPadding(grid.paddingLeft, topGap, grid.paddingRight, grid.paddingBottom)
        }
        syncDotsToLookupCapsule(bottom)
    }

    private fun syncDotsToLookupCapsule(bottom: Boolean) {
        val dots = binding.allAppsLeafPipsIxVw
        val params = dots.layoutParams as? RelativeLayout.LayoutParams ?: return

        if (bottom) {
            val own = resources.getDimensionPixelSize(org.fossify.commons.R.dimen.medium_margin)
            if (dots.paddingBottom != own) dots.setPadding(dots.paddingLeft, dots.paddingTop, dots.paddingRight, own)
        }
        val margin = if (bottom) {
            val pill = binding.queryBarIxVw.binding.toolbarContainer
            var pillTop = 0
            var v: View? = pill
            while (v != null && v !== binding.queryBarIxVw) { pillTop += v.top; v = v.parent as? View }
            if (v == null || pill.height == 0) return
            val gap = resources.getDimensionPixelSize(R.dimen.launcher_page_dots_search_gap)
            -(pillTop - gap).coerceAtLeast(0)
        } else {
            0
        }
        if (params.bottomMargin != margin) {
            params.bottomMargin = margin
            dots.layoutParams = params
        }
    }

    private fun surfaceNoOutcomesPlaceholderIfNeeded() {
        val itemCount = if (isPaged) pagesAdapter?.itemCount else getLister()?.itemCount
        binding.noResultsStubIxVw.beVisibleIf(itemCount != null && itemCount == 0)
    }

    override fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: WidgetCenter) {
        val gridItem = DeskViewerGridItem(
            id = null,
            left = -1,
            top = -1,
            right = -1,
            bottom = -1,
            page = 0,
            packageName = appLauncher.packageName,
            activityName = appLauncher.activityName,
            title = appLauncher.title,
            type = ITEM_TYPE_ICON,
            className = "",
            widgetId = -1,
            shortcutId = "",
            icon = null,
            docked = false,
            parentId = null,
            drawable = appLauncher.drawable
        )

        activity?.surfaceAtriumGlyphTweaks(x, y, gridItem, true)
        ignoreTouches = true

        binding.queryBarIxVw.closeSearch()
    }

    fun restoreLookup() {
        if (binding.queryBarIxVw.isSearchOpen) {
            binding.queryBarIxVw.closeSearch()
        }
    }

    private fun submitList(items: List<WidgetCenter>) {
        val searchQuery = binding.queryBarIxVw.getCurrentQuery()
        if (binding.queryBarIxVw.isSearchOpen) {
            drawLookupDeck(items, searchQuery)
            return
        }
        val filtered = if (searchQuery.isNotEmpty()) {
            items.filter {
                it.title.normalizeString()
                    .contains(searchQuery.normalizeString(), ignoreCase = true)
            }
        } else {
            com.loanmate.loan.emicalculator.carloan.homepanel.onboard.FeaturedCards.interleave(items)
        }

        if (isPaged) {
            pagesAdapter?.submitPages(chunkPages(filtered), context.launcherConfig.drawerColumnCount, context.launcherConfig.drawerRowCount.coerceAtLeast(2))
            binding.allAppsFlipperIxVw.setCurrentItem(0, false)
            drawDots(selected = 0)
            surfaceNoOutcomesPlaceholderIfNeeded()
            return
        }
        getLister()?.submitList(withAdvertRow(filtered)) {
            surfaceNoOutcomesPlaceholderIfNeeded()
        }
    }

    private var searchAdapter: CentersLoader? = null
    private var suggestions: List<WidgetCenter>? = null
    private var suggestionsGeneration = 0

    private fun surfaceLookupDeck(open: Boolean) {
        binding.queryPageIxVw.beVisibleIf(open)
        if (open) {
            binding.allAppsGridIxVw.beGone()
            binding.allAppsFlipperIxVw.beGone()
            binding.allAppsLeafPipsIxVw.beGone()
            binding.noResultsStubIxVw.beGone()
            suggestions = null
            pullSelections()
            drawLookupDeck(launchers, binding.queryBarIxVw.getCurrentQuery())
        } else {
            val paged = isPaged
            binding.allAppsGridIxVw.beVisibleIf(!paged)
            binding.allAppsFlipperIxVw.beVisibleIf(paged)
            binding.allAppsLeafPipsIxVw.beVisibleIf(paged)
            searchAdapter?.highlight = ""
            submitList(launchers)
        }
    }

    private fun lookupGridBridge(): CentersLoader {
        searchAdapter?.let { return it }
        val grid = binding.queryAppsGridIxVw
        grid.layoutManager = object : GridLayoutManager(context, SEARCH_COLUMNS) {
            override fun canScrollVertically() = false
        }
        grid.itemAnimator = null
        grid.isNestedScrollingEnabled = false

        val rowGap = resources.getDimensionPixelSize(R.dimen.launcher_search_card_row_gap)
        grid.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val position = parent.getChildAdapterPosition(view)
                outRect.top = if (position >= SEARCH_COLUMNS) rowGap else 0
            }
        })
        return CentersLoader(activity!!, this, null, onItemClick).also {
            searchAdapter = it
            grid.adapter = it
        }
    }

    private fun pullSelections() {
        val generation = ++suggestionsGeneration
        val installed = launchers
        val pm = context.packageManager
        ensureBackgroundThread {
            val usage = runCatching { context.appUsageDB.getAll() }.getOrDefault(emptyList())
            val facts = HashMap<String, WidgetChoices.Facts?>()
            val ranked = WidgetChoices.rank(installed, usage, { pkg ->
                facts.getOrPut(pkg) {
                    runCatching {
                        val info = pm.getPackageInfo(pkg, 0)
                        WidgetChoices.Facts(info.firstInstallTime, info.applicationInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED)
                    }.getOrNull()
                }
            }, System.currentTimeMillis())
            activity?.runOnUiThread {
                if (generation != suggestionsGeneration) return@runOnUiThread
                suggestions = ranked
                if (binding.queryBarIxVw.isSearchOpen && binding.queryBarIxVw.getCurrentQuery().isEmpty()) {
                    drawLookupDeck(launchers, "")
                }
            }
        }
    }

    private fun drawLookupDeck(items: List<WidgetCenter>, query: String) {
        val adapter = lookupGridBridge()
        if (query.isEmpty()) {
            val apps = live(suggestions ?: emptyList(), items)
            binding.queryAppsTitleIxVw.setText(R.string.launcher_suggested_apps)
            adapter.highlight = ""
            adapter.submitList(apps.toMutableList())
            binding.queryAppsCellIxVw.beVisibleIf(apps.size >= WidgetChoices.MIN_TO_SHOW)
            binding.queryHotlinksCellIxVw.beGone()
            binding.noResultsStubIxVw.beGone()
            return
        }
        val needle = query.normalizeString()
        val matches = items.filter { it.title.normalizeString().contains(needle, ignoreCase = true) }
            .sortedWith(compareBy<WidgetCenter> { !it.title.normalizeString().startsWith(needle, ignoreCase = true) }.thenBy { it.title.lowercase() })
        binding.queryAppsTitleIxVw.setText(R.string.launcher_searched_apps)
        adapter.highlight = query
        adapter.submitList(matches.take(SEARCH_MAX_RESULTS).toMutableList())
        binding.queryAppsCellIxVw.beVisibleIf(matches.isNotEmpty())
        binding.noResultsStubIxVw.beVisibleIf(matches.isEmpty())
        drawShortcuts(matches.firstOrNull())
    }

    private fun drawShortcuts(app: WidgetCenter?) {
        val card = binding.queryHotlinksCellIxVw
        val list = binding.queryHotlinksRollIxVw
        list.removeAllViews()
        if (app == null) { card.beGone(); return }
        val launcherApps = context.applicationContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val shortcuts = runCatching {
            if (!launcherApps.hasShortcutHostPermission()) return@runCatching null
            val flags = LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
            launcherApps.getShortcuts(LauncherApps.ShortcutQuery().setQueryFlags(flags).setPackage(app.packageName), Process.myUserHandle())
        }.getOrNull()?.filter { it.isEnabled }?.take(SEARCH_MAX_SHORTCUTS).orEmpty()
        if (shortcuts.isEmpty()) { card.beGone(); return }
        binding.queryHotlinksTitleIxVw.text = app.title
        val inflater = LayoutInflater.from(context)
        val density = resources.displayMetrics.densityDpi
        shortcuts.forEach { shortcut ->
            val row = HpBinQueryShortcutBinding.inflate(inflater, list, false)
            row.queryHotlinkTextIxVw.text = shortcut.shortLabel ?: shortcut.longLabel ?: ""
            row.queryHotlinkIconIxVw.setImageDrawable(runCatching { launcherApps.getShortcutIconDrawable(shortcut, density) }.getOrNull())
            row.root.setOnClickListener {
                com.loanmate.loan.emicalculator.carloan.homepanel.Launcher.events.onClick("search_shortcut")
                runCatching {
                    launcherApps.startShortcut(shortcut.`package`, shortcut.id, null, null, Process.myUserHandle())
                }.onFailure { activity?.showErrorToast(it as? Exception ?: Exception(it)) }
                if (activity?.launcherConfig?.closeAppletRack == true) activity?.closeAppletRack(delayed = true)
            }
            list.addView(row.root)
        }
        card.beVisible()
    }

    private fun withAdvertRow(apps: List<WidgetCenter>): MutableList<WidgetCenter> {
        if (apps.isEmpty()) {
            return apps.toMutableList()
        }

        val spanCount = (binding.allAppsGridIxVw.layoutManager as MyGridLayoutManager).spanCount
        val row = CenterAdsToggles.rackAdvertRowPoint()
        val index = (row * spanCount).coerceIn(0, apps.size)
        return apps.toMutableList().apply { add(index, CentersLoader.AD_SLOT) }
    }
}
