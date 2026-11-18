package dev.slothiesmooth.linksdetektor.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdButton
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdText
import dev.slothiesmooth.linksdetektor.designlibrary.atoms.LdTextStyle
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldDimens

private val SpinnerSize = 32.dp
private val SpinnerStroke = 3.dp

@Composable
private fun CenteredColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(MaterialTheme.ldDimens.space6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.ldDimens.space3, Alignment.CenterVertically),
    ) { content() }
}

/** Nothing to show yet: a title and a one-line hint. */
@Composable
fun LdEmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    CenteredColumn(modifier) {
        LdText(title, style = LdTextStyle.Label)
        LdText(message, style = LdTextStyle.BodySm, color = MaterialTheme.ldColors.dim)
    }
}

/** Work in progress: an amber spinner and what is being done. */
@Composable
fun LdLoadingState(label: String, modifier: Modifier = Modifier) {
    CenteredColumn(modifier) {
        CircularProgressIndicator(
            modifier = Modifier.size(SpinnerSize),
            color = MaterialTheme.ldColors.amber,
            strokeWidth = SpinnerStroke,
        )
        LdText(label, style = LdTextStyle.BodySm, color = MaterialTheme.ldColors.dim)
    }
}

/** Something failed: what, why, and one way forward. There is no red in this system. */
@Composable
fun LdErrorState(title: String, message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    CenteredColumn(modifier) {
        LdText(title, style = LdTextStyle.Label)
        LdText(message, style = LdTextStyle.BodySm, color = MaterialTheme.ldColors.dim)
        LdButton("Try again", onClick = onRetry)
    }
}
