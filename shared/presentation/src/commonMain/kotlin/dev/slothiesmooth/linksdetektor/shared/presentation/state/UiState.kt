package dev.slothiesmooth.linksdetektor.shared.presentation.state

import androidx.compose.runtime.Immutable

/** What a screen shows for one piece of async content. */
@Immutable
sealed interface UiState<out T> {
    /** The content is being produced. */
    data object Loading : UiState<Nothing>

    /** The content is ready to show. */
    data class Ready<T>(val value: T) : UiState<T>

    /** The content could not be produced; [message] is ready to show to the user. */
    data class Failed(val message: String) : UiState<Nothing>
}
