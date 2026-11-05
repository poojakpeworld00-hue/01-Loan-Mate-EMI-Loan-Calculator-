package com.loanmate.loan.emicalculator.carloan.homelloan.launcher.support

import com.loanmate.loan.emicalculator.carloan.homelloan.launcher.entities.GadgetHub

object SymbolRepo {
    @Volatile
    private var cachedLaunchers = emptyList<GadgetHub>()

    var launchers: List<GadgetHub>
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
        SymbolLoader.clear()
    }
}