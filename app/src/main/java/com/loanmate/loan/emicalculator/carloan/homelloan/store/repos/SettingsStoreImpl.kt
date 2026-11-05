package com.loanmate.loan.emicalculator.carloan.homelloan.store.repos

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.loanmate.loan.emicalculator.carloan.homelloan.store.disk.PicksNotesSource
import com.loanmate.loan.emicalculator.carloan.homelloan.store.disk.SUPPORTED_LANGUAGES
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.Tongue
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.SettingsStore
import java.util.Locale

/** The onboarding flag lives in preferences; the language is AppCompat's per-app locale, which it persists itself. */
class SettingsStoreImpl(private val prefs: PicksNotesSource) : SettingsStore {
    override val languages: List<Tongue> = SUPPORTED_LANGUAGES

    override fun currentLanguageIndex(): Int {
        val tag = (AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()).language
        // Android reports Indonesian with the legacy code "in".
        val norm = if (tag == "in") "id" else tag
        return languages.indexOfFirst { it.tag == norm }.coerceAtLeast(0)
    }

    override fun setLanguage(index: Int) {
        if (index != currentLanguageIndex()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languages[index].tag))
        }
    }

    override fun isOnboarded(): Boolean = prefs.onboarded

    override fun answer(step: String): Set<Int>? = prefs.answer(step)

    override fun setAnswer(step: String, selection: Set<Int>) = prefs.keepAnswers(mapOf(step to selection))

    override fun takeFirstHome(): Boolean = prefs.firstHomePending.also { if (it) prefs.firstHomePending = false }

    override fun setOnboarded(answers: Map<String, Set<Int>>) {
        prefs.keepAnswers(answers)
        prefs.firstHomePending = true
        prefs.onboarded = true
    }
}
