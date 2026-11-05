package com.loanmate.loan.emicalculator.carloan.homelloan.loan.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.HistoryEntry
import com.loanmate.loan.emicalculator.carloan.homelloan.loan.data.LoanPrefs

/**
 * Persists the most recent calculations as a JSON blob inside [LoanPrefs].
 * Newest entries are kept at index 0; the list is capped at [MAX_ITEMS] so it
 * doesn't grow unbounded.
 */
class HistoryRepo(private val pref: LoanPrefs) {

    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<HistoryEntry>>() {}.type

    fun getAll(): List<HistoryEntry> {
        val raw = pref.getString(KEY, "")
        if (raw.isEmpty()) return emptyList()
        return runCatching {
            gson.fromJson<MutableList<HistoryEntry>>(raw, listType) ?: mutableListOf()
        }.getOrDefault(mutableListOf())
    }

    fun add(item: HistoryEntry) {
        val current = getAll().toMutableList()
        current.add(0, item)
        while (current.size > MAX_ITEMS) current.removeAt(current.size - 1)
        save(current)
    }

    fun delete(id: String) {
        val current = getAll().toMutableList()
        if (current.removeAll { it.id == id }) save(current)
    }

    fun clear() {
        pref.putString(KEY, "")
    }

    private fun save(items: List<HistoryEntry>) {
        pref.putString(KEY, gson.toJson(items))
    }

    companion object {
        private const val KEY = "history_items"
        const val MAX_ITEMS = 50
    }
}
