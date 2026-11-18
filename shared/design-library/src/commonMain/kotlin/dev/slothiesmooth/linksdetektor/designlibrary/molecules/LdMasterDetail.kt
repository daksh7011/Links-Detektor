package dev.slothiesmooth.linksdetektor.designlibrary.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The content width from which a list and its detail sit side by side. */
val LdSidePaneFrom = 720.dp

/**
 * A list with a detail. From 720 dp the [master] sits beside the [detail] (or a placeholder while nothing is [chosen]);
 * below that the detail opens in a sheet while one is [chosen], and [onClose] dismisses it.
 */
@Composable
fun LdMasterDetail(
    chosen: Boolean,
    onClose: () -> Unit,
    master: @Composable (Modifier) -> Unit,
    detail: @Composable () -> Unit,
    placeholderTitle: String,
    placeholderMessage: String,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        if (maxWidth >= LdSidePaneFrom) {
            Row(Modifier.fillMaxSize()) {
                master(Modifier.weight(1f).fillMaxHeight())
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    if (chosen) detail() else LdEmptyState(placeholderTitle, placeholderMessage)
                }
            }
        } else {
            master(Modifier.fillMaxSize())
            if (chosen) LdSheet(onClose) { detail() }
        }
    }
}
