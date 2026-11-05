package com.fffskintools.skin.tools.emotes.presentation.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.fffskintools.skin.tools.emotes.data.remote.config.ToolProfile
import com.fffskintools.skin.tools.emotes.domain.repository.OptionsDepot
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow

data class SetupUiMode(val stepIndex: Int, val selections: List<Set<Int>>) {
    val selection: Set<Int> get() = selections[stepIndex]
}

/**
 * Phase position and every step's answers; kept in SavedStateHandle so a language change mid-onboarding keeps them.
 * The steps shown and their order come from Remote Config `onboarding.order`, frozen for the whole run.
 */
class SetupViewForm(
    private val settings: OptionsDepot,
    config: ToolProfile,
    isDefaultHome: Boolean,
    private val handle: SavedStateHandle,
) : ViewModel() {
    /** [resume]: the same steps and position as before Recents removed our task (SetupHeadway). */
    val resumed = handle.get<Boolean>(EXTRA_RESUME) == true && SetupHeadway.keys.isNotEmpty()

    val steps: List<Phase> = setupPhases(settings.languages).let { all ->
        val order = handle.get<Array<String>>(KEY_ORDER)?.toList()
            ?: SetupHeadway.keys.takeIf { resumed }
            ?: SetupGuards.views2(config, isDefaultHome).map { it.key }
        order.mapNotNull { key -> all.firstOrNull { it.key == key } }.distinct()
            .also { steps -> handle[KEY_ORDER] = steps.map { it.key }.toTypedArray() }
    }
    val skipButton = config.onboarding.skipButton
    val defaultHomeSkipButton = config.onboarding.defaultHomeSkipButton
    val favIndex = steps.indexOfFirst { it.key == FAV_KEY }

    private val _state = MutableStateFlow(
        SetupUiMode(
            stepIndex = (handle[KEY_STEP] ?: SetupHeadway.index.takeIf { resumed } ?: 0)
                .coerceIn(0, (steps.size - 1).coerceAtLeast(0)),
            selections = steps.mapIndexed { i, step ->
                handle.get<IntArray>("$KEY_SEL$i")?.toSet()
                    ?: if (step.key == LANG_KEY) setOf(settings.currentLanguageIndex()) else step.defaultSel
            },
        )
    )
    val state: StateFlow<SetupUiMode> = _state

    private val _finished = Channel<Unit>(Channel.CONFLATED)
    val finished = _finished.receiveAsFlow()

    /** Onboarding is over: a screen recreated after that (the language change) goes straight on to Home. */
    var done = false
        private set

    init {
        // Remote Config can turn every step off: onboarding is then done at once.
        if (steps.isEmpty()) skip()
    }

    fun select(position: Int) {
        val s = _state.value
        val current = s.selection
        val selection = when {
            !steps[s.stepIndex].multi -> setOf(position)
            position in current -> current - position
            else -> current + position
        }
        update(s.copy(selections = s.selections.replace(s.stepIndex, selection)))
    }

    fun switchAllStars() {
        if (favIndex < 0) return
        val all = steps[favIndex].opts.indices.toSet()
        update(_state.value.let { s -> s.copy(selections = s.selections.replace(favIndex, if (s.selections[favIndex] == all) emptySet() else all)) })
    }

    /** The language tag of the language step's option [index]. */
    fun languageTag(index: Int): String? = settings.languages.getOrNull(index)?.tag

    fun next() {
        val s = _state.value
        val language = s.selection.firstOrNull()?.takeIf { steps[s.stepIndex].key == LANG_KEY }
        // The language first, applied in place (the onboarding screen handles the locale change itself); on the last step
        // the screen waits for it to arrive before opening Home.
        language?.let(settings::setLanguage)
        if (s.stepIndex < steps.lastIndex) update(s.copy(stepIndex = s.stepIndex + 1)) else skip()
    }

    /** Ends onboarding and goes home, saving the answers given so far. */
    fun skip() {
        done = true
        val s = _state.value
        settings.setOnboarded(steps.indices.associate { steps[it].key to s.selections[it] })
        _finished.trySend(Unit)
    }

    /** Jumps to [index] (system key `back` / `reset`). */
    fun kitGoTo(index: Int) {
        val s = _state.value
        if (index in steps.indices) update(s.copy(stepIndex = index))
    }

    /** Goes back one step; false on the first step, where back should leave the screen. */
    fun back(): Boolean {
        val s = _state.value
        if (s.stepIndex == 0) return false
        update(s.copy(stepIndex = s.stepIndex - 1))
        return true
    }

    private fun update(s: SetupUiMode) {
        _state.value = s
        handle[KEY_STEP] = s.stepIndex
        SetupHeadway.keys = steps.map { it.key }
        SetupHeadway.index = s.stepIndex
        s.selections.forEachIndexed { i, sel -> handle["$KEY_SEL$i"] = sel.toIntArray() }
    }

    private fun List<Set<Int>>.replace(index: Int, value: Set<Int>) = toMutableList().also { it[index] = value }

    companion object {
        /** Intent extra: reopened after Recents removed our task, so go on where the user was. */
        const val EXTRA_RESUME = "resume"
        private const val KEY_STEP = "step"
        private const val KEY_SEL = "sel"
        private const val KEY_ORDER = "order"
    }
}
