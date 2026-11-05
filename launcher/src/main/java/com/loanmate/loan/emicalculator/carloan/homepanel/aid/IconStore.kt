package com.loanmate.loan.emicalculator.carloan.homepanel.aid

import com.loanmate.loan.emicalculator.carloan.homepanel.models.WidgetCenter

object IconStore {
    @Volatile
    private var cachedLaunchers = emptyList<WidgetCenter>()

    var launchers: List<WidgetCenter>
        get() = cachedLaunchers
        set(value) {
            synchronized(this) {
                cachedLaunchers = value
            }
        }

    @Volatile
    var installedSignature: String? = null

    @Volatile
    var sourceDirs: Map<String, String> = emptyMap()

    fun clear() {
        launchers = emptyList()
        installedSignature = null
        sourceDirs = emptyMap()
        // The on-demand cache holds the same icons; it must not hand an old one back.
        IconFetcher.clear()
    }
}