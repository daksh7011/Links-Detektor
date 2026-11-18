package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdButton
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdButtonStyle
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdChip
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdText
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdTextField
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdTextStyle
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.ldFocusRing
import dev.slothiesmooth.linksdetektor.designlibrary.molecules.LdPanel
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldDimens
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldType
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.DetectedLink
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

private val HighlightMaxHeight = 200.dp
private val RuleWidth = 1.dp

// Wide enough for the longest label ("Username", "Password", "Fragment") in the label style, so none breaks mid-word.
private val LabelWidth = 88.dp

@Composable
internal fun InputPane(
    input: String,
    truncated: Boolean,
    onInputChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(MaterialTheme.ldDimens.space2)) {
        LdTextField(
            value = input,
            onValueChange = onInputChange,
            label = "Your text",
            placeholder = "Paste or type text with links in it",
            minLines = 4,
            maxLines = 10,
            modifier = Modifier.fillMaxWidth(),
            fieldModifier = Modifier.testTag("detektor-input"),
        )
        if (truncated) {
            LdText(
                "Only the first 20,000 characters are checked.",
                style = LdTextStyle.Caption,
                color = MaterialTheme.ldColors.amber,
            )
        }
    }
}

private fun optionLabel(option: LinksDetektorOptions): String = when (option) {
    LinksDetektorOptions.Default -> "Default"
    LinksDetektorOptions.QUOTE_MATCH -> "Double quotes"
    LinksDetektorOptions.SINGLE_QUOTE_MATCH -> "Single quotes"
    LinksDetektorOptions.BRACKET_MATCH -> "Brackets"
    LinksDetektorOptions.JSON -> "JSON"
    LinksDetektorOptions.JAVASCRIPT -> "JavaScript"
    LinksDetektorOptions.XML -> "XML"
    LinksDetektorOptions.HTML -> "HTML"
    LinksDetektorOptions.ALLOW_SINGLE_LEVEL_DOMAIN -> "Single-word hosts"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun OptionChips(
    options: ImmutableSet<LinksDetektorOptions>,
    onToggleOption: (LinksDetektorOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(MaterialTheme.ldDimens.space2)) {
        ToggleableOptions.forEach { option ->
            LdChip(
                text = optionLabel(option),
                selected = option in options,
                onToggle = { onToggleOption(option) },
                modifier = Modifier.testTag("option-${option.name}"),
            )
        }
    }
}

/** The analyzed text with every link marked; a tap on a link selects it. */
@Composable
internal fun HighlightPane(
    analyzedText: String,
    links: ImmutableList<DetectedLink>,
    selectedId: Int?,
    onSelectLink: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.ldColors
    val type = MaterialTheme.ldType
    val style = remember(colors, type) {
        HighlightStyle(
            link = SpanStyle(color = colors.amber, fontFamily = type.code.fontFamily, textDecoration = TextDecoration.Underline),
            selected = SpanStyle(color = colors.amberInk, background = colors.amber, fontFamily = type.code.fontFamily),
        )
    }
    val highlighted = remember(analyzedText, links, selectedId, style) {
        buildHighlight(analyzedText, links, selectedId, style)
    }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    LdPanel(modifier) {
        Text(
            text = highlighted,
            style = type.body.copy(color = colors.fg),
            onTextLayout = { layout = it },
            modifier = Modifier
                .testTag("detektor-highlight")
                .heightIn(max = HighlightMaxHeight)
                .verticalScroll(rememberScrollState())
                .pointerInput(highlighted) {
                    detectTapGestures { position ->
                        val offset = layout?.getOffsetForPosition(position) ?: return@detectTapGestures
                        highlighted.getStringAnnotations(LINK_TAG, offset, offset).firstOrNull()
                            ?.item?.toIntOrNull()
                            ?.let(onSelectLink)
                    }
                },
        )
    }
}

@Composable
internal fun ResultList(
    links: ImmutableList<DetectedLink>,
    selectedId: Int?,
    onSelectLink: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimens = MaterialTheme.ldDimens
    Column(modifier.testTag("detektor-results"), verticalArrangement = Arrangement.spacedBy(dimens.space2)) {
        LdText(if (links.size == 1) "1 link found" else "${links.size} links found", style = LdTextStyle.Label)
        LazyColumn(Modifier.fillMaxWidth()) {
            items(links, key = { it.id }) { link ->
                ResultRow(link, selected = link.id == selectedId, onClick = { onSelectLink(link.id) })
            }
        }
    }
}

@Composable
private fun ResultRow(link: DetectedLink, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.ldColors
    val dimens = MaterialTheme.ldDimens
    val interactions = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .testTag("result-${link.id}")
            .ldFocusRing(interactions, dimens.radiusField)
            .background(if (selected) colors.surface2 else Color.Transparent)
            .clickable(interactionSource = interactions, indication = null, role = Role.Button, onClick = onClick)
            .padding(vertical = dimens.space3, horizontal = dimens.space2),
    ) {
        LdText(link.original.full, style = LdTextStyle.Code)
        LdText(link.original.host, style = LdTextStyle.Caption, color = colors.dim)
        Box(Modifier.fillMaxWidth().padding(top = dimens.space3).height(RuleWidth).background(colors.line))
    }
}

@Composable
internal fun DetailPane(link: DetectedLink, onClose: (() -> Unit)?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.ldColors
    val dimens = MaterialTheme.ldDimens
    val rows = remember(link) { detailRows(link) }
    LdPanel(modifier.testTag("detektor-detail")) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.space3)) {
            TableRow {
                Spacer(Modifier.width(LabelWidth))
                Cell("Original", Modifier.weight(1f), LdTextStyle.Caption, colors.dim)
                Cell("Tidied", Modifier.weight(1f), LdTextStyle.Caption, colors.dim)
            }
            rows.forEachIndexed { index, row ->
                TableRow {
                    Cell(row.label, Modifier.width(LabelWidth), LdTextStyle.Label, colors.dim)
                    Cell(row.original, Modifier.weight(1f), LdTextStyle.Code)
                    when {
                        row.normalized != null ->
                            Cell(row.normalized, Modifier.weight(1f), LdTextStyle.Code, if (row.changed) colors.amber else colors.fg)

                        index == 0 -> Cell("This link could not be tidied.", Modifier.weight(1f), LdTextStyle.BodySm, colors.dim)
                        else -> Spacer(Modifier.weight(1f))
                    }
                }
            }
            if (onClose != null) LdButton("Close", onClick = onClose, style = LdButtonStyle.Ghost)
        }
    }
}

@Composable
private fun TableRow(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MaterialTheme.ldDimens.space3), content = content)
}

@Composable
private fun RowScope.Cell(
    text: String,
    modifier: Modifier,
    style: LdTextStyle = LdTextStyle.Body,
    color: Color = MaterialTheme.ldColors.fg,
) {
    LdText(text, modifier = modifier, style = style, color = color)
}
