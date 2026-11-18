package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import androidx.compose.runtime.Immutable
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.DetectedLink
import dev.slothiesmooth.linksdetektor.shared.presentation.state.UiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList

/** The longest text that is checked. */
const val MAX_INPUT_CHARS = 20_000

/** How long typing must pause before the text is checked. */
const val INPUT_DEBOUNCE_MS = 250L

/** The text a detection ran on and the links found in it. */
@Immutable
data class DetectionUi(val analyzedText: String, val links: ImmutableList<DetectedLink>) {
    companion object {
        val Empty = DetectionUi("", persistentListOf())
    }
}

@Immutable
data class DetektorUiState(
    val input: String = "",
    val truncated: Boolean = false,
    val options: ImmutableSet<LinksDetektorOptions> = persistentSetOf(),
    val detecting: Boolean = false,
    val result: UiState<DetectionUi> = UiState.Ready(DetectionUi.Empty),
    val selectedId: Int? = null,
) {
    /** The ready link whose id is [selectedId], else null. */
    val selected: DetectedLink?
        get() = (result as? UiState.Ready)?.value?.links?.firstOrNull { it.id == selectedId }
}

/** Which body the screen shows. */
enum class DetektorScreenState { Empty, Loading, NoLinks, Error, Content }

fun screenStateOf(state: DetektorUiState): DetektorScreenState = when (val result = state.result) {
    is UiState.Failed -> DetektorScreenState.Error
    UiState.Loading -> DetektorScreenState.Loading
    is UiState.Ready -> when {
        result.value.analyzedText.isBlank() ->
            if (state.detecting && state.input.isNotBlank()) DetektorScreenState.Loading else DetektorScreenState.Empty

        result.value.links.isEmpty() -> DetektorScreenState.NoLinks
        else -> DetektorScreenState.Content
    }
}

/** The toggles the screen offers, in display order: every option except Default. */
val ToggleableOptions: ImmutableList<LinksDetektorOptions> =
    LinksDetektorOptions.entries.filter { it != LinksDetektorOptions.Default }.toImmutableList()
