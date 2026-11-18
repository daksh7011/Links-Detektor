package dev.slothiesmooth.linksdetektor.feature.detektor.basic.presentation

import androidx.compose.runtime.Immutable
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.DetectedLink
import dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model.LinkParts
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/** One line of the detail table: a part of the link as typed and, when it could be tidied, as tidied. */
@Immutable
data class PartRow(val label: String, val original: String, val normalized: String?, val changed: Boolean)

internal data class RowSpec(val label: String, val alwaysShown: Boolean, val value: (LinkParts) -> String)

internal val RowSpecs: ImmutableList<RowSpec> = persistentListOf(
    RowSpec("Full URL", alwaysShown = true) { it.full },
    RowSpec("Scheme", alwaysShown = false) { it.scheme },
    RowSpec("Username", alwaysShown = false) { it.username },
    RowSpec("Password", alwaysShown = false) { it.password },
    RowSpec("Host", alwaysShown = true) { it.host },
    RowSpec("Port", alwaysShown = true) { it.port.toString() },
    RowSpec("Path", alwaysShown = false) { it.path },
    RowSpec("Query", alwaysShown = false) { it.query },
    RowSpec("Fragment", alwaysShown = false) { it.fragment },
)

/** The parts of [link], original beside tidied; a part both versions lack is left out, except the full URL, host and port. */
fun detailRows(link: DetectedLink): ImmutableList<PartRow> = RowSpecs
    .mapNotNull { spec ->
        val original = spec.value(link.original)
        val normalized = link.normalized?.let(spec.value)
        if (spec.alwaysShown || original.isNotEmpty() || !normalized.isNullOrEmpty()) {
            PartRow(spec.label, original, normalized, changed = normalized != null && normalized != original)
        } else {
            null
        }
    }
    .toImmutableList()
