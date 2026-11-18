package dev.slothiesmooth.linksdetektor.designlibrary.molecules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldColors
import dev.slothiesmooth.linksdetektor.designlibrary.tokens.ldDimens

/** A bottom sheet in the surface colour with rounded top corners and the standard gutters. */
@Composable
fun LdSheet(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val colors = MaterialTheme.ldColors
    val dimens = MaterialTheme.ldDimens
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = dimens.radiusSheet, topEnd = dimens.radiusSheet),
        containerColor = colors.surface,
        contentColor = colors.fg,
        scrimColor = colors.scrim,
        tonalElevation = 0.dp,
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = dimens.space4).padding(bottom = dimens.space6),
            verticalArrangement = Arrangement.spacedBy(dimens.space3),
            content = content,
        )
    }
}
