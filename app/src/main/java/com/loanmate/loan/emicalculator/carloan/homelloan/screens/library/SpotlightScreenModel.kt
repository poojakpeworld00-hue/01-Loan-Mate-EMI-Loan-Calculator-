package com.loanmate.loan.emicalculator.carloan.homelloan.screens.library

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.ShelfOrder
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.entity.PieceTwo
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.ShelfStore
import com.loanmate.loan.emicalculator.carloan.homelloan.logic.repos.DocsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpotlightScreenModel(
    repository: ShelfStore,
    private val media: DocsStore,
    args: SavedStateHandle,
) : ViewModel() {
    val type: ShelfOrder = ShelfOrder.entries[args[SpotlightActivity.EXTRA_CATALOG] ?: 0]
    val item: PieceTwo = repository.section(type)
        .groups[args[SpotlightActivity.EXTRA_GROUP] ?: 0]
        .items[args[SpotlightActivity.EXTRA_ITEM] ?: 0]

    private val _messages = Channel<Int>(Channel.BUFFERED)

    /** One-off messages (string resources) to show as a toast. */
    val messages = _messages.receiveAsFlow()

    /** Saves the item image to the gallery; the caller must hold the storage permission below Android 10. */
    fun download() {
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { media.saveToGallery(item.image, item.name) }
            _messages.send(message(saved))
        }
    }

    @StringRes
    private fun message(saved: Boolean) = if (saved) R.string.download_done else R.string.download_failed
}
