package com.loanmate.loan.emicalculator.carloan.homelloan.presentation.catalog

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loanmate.loan.emicalculator.carloan.homelloan.R
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.LibrarySort
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.model.Unit2
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository.LibraryDepot
import com.loanmate.loan.emicalculator.carloan.homelloan.domain.repository.FilesDepot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FocusViewForm(
    repository: LibraryDepot,
    private val media: FilesDepot,
    args: SavedStateHandle,
) : ViewModel() {
    val type: LibrarySort = LibrarySort.entries[args[FocusActivity.EXTRA_CATALOG] ?: 0]
    val item: Unit2 = repository.section(type)
        .groups[args[FocusActivity.EXTRA_GROUP] ?: 0]
        .items[args[FocusActivity.EXTRA_ITEM] ?: 0]

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
