package de.rodeprojects.liquidratios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rodeprojects.liquidratios.data.MixStore
import de.rodeprojects.liquidratios.domain.EditorAction
import de.rodeprojects.liquidratios.domain.EditorState
import de.rodeprojects.liquidratios.domain.Mix
import de.rodeprojects.liquidratios.domain.MixEditor
import java.io.IOException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal enum class StorageError { LOAD, SAVE }

internal data class AppState(
    val editor: EditorState? = null,
    val storageError: StorageError? = null,
)

internal class MixViewModel(private val store: MixStore) : ViewModel() {
    private val updates = Channel<Mix>(Channel.CONFLATED)
    private val current = MutableStateFlow(AppState())
    val state = current.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                current.value = AppState(EditorState(store.load()))
            } catch (_: IOException) {
                current.value = AppState(EditorState(), StorageError.LOAD)
            }

            // One writer preserves order even during rapid slider and component changes.
            for (mix in updates) {
                try {
                    store.save(mix)
                    current.value = current.value.copy(storageError = null)
                } catch (_: IOException) {
                    current.value = current.value.copy(storageError = StorageError.SAVE)
                }
            }
        }
    }

    fun dispatch(action: EditorAction) {
        val before = current.value.editor ?: return
        val after = MixEditor.reduce(before, action)
        current.value = current.value.copy(editor = after)
        if (after.mix != before.mix) updates.trySend(after.mix)
    }
}
