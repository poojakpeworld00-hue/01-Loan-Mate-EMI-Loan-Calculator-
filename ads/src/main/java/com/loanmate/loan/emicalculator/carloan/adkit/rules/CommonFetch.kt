package com.loanmate.loan.emicalculator.carloan.adkit.rules

/**
 * One load at a time, shared by every requester (preload + screens): [join] returns true only for the caller
 * that must start the load; everyone joined before [finish] is told the result once. Main-thread use.
 */
class CommonFetch<T> {
    private val waiters = mutableListOf<(T) -> Unit>()

    var inFlight: Boolean = false
        private set

    fun join(onDone: (T) -> Unit): Boolean {
        waiters += onDone
        if (inFlight) return false
        inFlight = true
        return true
    }

    fun finish(result: T) {
        inFlight = false
        val done = waiters.toList()
        waiters.clear()
        done.forEach { it(result) }
    }
}
