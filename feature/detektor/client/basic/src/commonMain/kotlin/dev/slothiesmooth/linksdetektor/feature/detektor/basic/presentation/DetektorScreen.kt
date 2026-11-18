package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdText
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdTextStyle
import dev.slothiesmooth.linksdetektor.designlibrary.molecules.LdEmptyState
import dev.slothiesmooth.linksdetektor.designlibrary.molecules.LdErrorState
import dev.slothiesmooth.linksdetektor.designlibrary.molecules.LdLoadingState
import dev.slothiesmooth.linksdetektor.designlibrary.molecules.LdMasterDetail
import dev.slothiesmooth.linksdetektor.designlibrary.molecules.LdSidePaneFrom
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldDimens
import dev.slothiesmooth.linksdetektor.shared.presentation.state.UiState

private val ContentMaxWidth = 1360.dp
private val WideGutter = 24.dp

/** The detector screen: collects the view model's state and delegates to [DetektorContent]. */
@Composable
fun DetektorScreen(viewModel: DetektorViewModel) {
    val state by viewModel.state.collectAsState()
    DetektorContent(
        state = state,
        onInputChange = viewModel::onInputChange,
        onToggleOption = viewModel::onToggleOption,
        onSelectLink = viewModel::onSelectLink,
        onRetry = viewModel::onRetry,
    )
}

/** The stateless screen: everything it shows comes from [state]. */
@Composable
fun DetektorContent(
    state: DetektorUiState,
    onInputChange: (String) -> Unit,
    onToggleOption: (LinksDetektorOptions) -> Unit,
    onSelectLink: (Int?) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = MaterialTheme.ldDimens
    BoxWithConstraints(modifier.fillMaxSize()) {
        val gutter = if (maxWidth >= LdSidePaneFrom) WideGutter else dimens.space4
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = ContentMaxWidth)
                .fillMaxSize()
                .padding(horizontal = gutter, vertical = dimens.space4),
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
        ) {
            LdText("Links Detektor", style = LdTextStyle.Title)
            LdText(
                "Paste any text. Every link in it is found and taken apart.",
                style = LdTextStyle.BodySm,
                color = MaterialTheme.ldColors.dim,
            )
            InputPane(state.input, state.truncated, onInputChange, Modifier.fillMaxWidth())
            OptionChips(state.options, onToggleOption, Modifier.fillMaxWidth())
            Body(state, onSelectLink, onRetry, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun Body(state: DetektorUiState, onSelectLink: (Int?) -> Unit, onRetry: () -> Unit, modifier: Modifier) {
    val ready = (state.result as? UiState.Ready)?.value
    when (screenStateOf(state)) {
        DetektorScreenState.Empty ->
            LdEmptyState("Nothing to check yet", "Paste some text above and the links in it show up here.", modifier)

        DetektorScreenState.Loading -> LdLoadingState("Checking…", modifier)
        DetektorScreenState.NoLinks ->
            LdEmptyState("No links found", "Try another option, or text with a web address in it.", modifier)

        DetektorScreenState.Error ->
            LdErrorState("Could not check that text", (state.result as? UiState.Failed)?.message.orEmpty(), onRetry, modifier)

        DetektorScreenState.Content -> if (ready != null) {
            BoxWithConstraints(modifier) {
                val sideBySide = maxWidth >= LdSidePaneFrom
                val selected = state.selected
                val closeDetail = { onSelectLink(null) }
                LdMasterDetail(
                    chosen = selected != null,
                    onClose = closeDetail,
                    master = { masterModifier ->
                        Column(masterModifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.ldDimens.space3)) {
                            HighlightPane(ready.analyzedText, ready.links, state.selectedId, onSelectLink, Modifier.fillMaxWidth())
                            ResultList(ready.links, state.selectedId, onSelectLink, Modifier.weight(1f).fillMaxWidth())
                        }
                    },
                    detail = {
                        if (selected != null) {
                            DetailPane(selected, onClose = if (sideBySide) null else closeDetail)
                        }
                    },
                    placeholderTitle = "Pick a link",
                    placeholderMessage = "Its parts show here, before and after tidying.",
                )
            }
        }
    }
}
