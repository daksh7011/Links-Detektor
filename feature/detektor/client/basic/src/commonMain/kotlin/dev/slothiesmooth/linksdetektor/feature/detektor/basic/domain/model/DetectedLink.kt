package dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.model

import androidx.compose.runtime.Immutable
import com.slothiesmooth.linksdetektor.Url
import kotlinx.coroutines.CancellationException

/** The parts of one link, every one ready to show; a part the link does not have is empty. */
@Immutable
data class LinkParts(
    val full: String,
    val scheme: String,
    val username: String,
    val password: String,
    val host: String,
    val port: Int,
    val path: String,
    val query: String,
    val fragment: String,
)

/**
 * One link found in the analyzed text. [start] is inclusive and [endExclusive] exclusive, both indexes into that text.
 * [normalized] is null when normalizing threw.
 */
@Immutable
data class DetectedLink(
    val id: Int,
    val start: Int,
    val endExclusive: Int,
    val original: LinkParts,
    val normalized: LinkParts?,
)

/**
 * Maps a detected [Url] to a [DetectedLink]. The range comes from [Url.range], which covers the whole source span, including
 * characters the detector drops from `originalUrl`. [normalize] is a parameter so a failing normalizer can be tested.
 */
internal fun Url.toDetectedLink(id: Int, normalize: (Url) -> Url = Url::normalize): DetectedLink {
    val normalized = runCatching { normalize(this) }
        .onFailure { if (it is CancellationException) throw it }
        .getOrNull()
    return DetectedLink(
        id = id,
        start = range.first,
        endExclusive = range.last + 1,
        original = toParts(full = originalUrl.orEmpty()),
        normalized = normalized?.toParts(full = normalized.fullUrl),
    )
}

private fun Url.toParts(full: String) = LinkParts(
    full = full,
    scheme = scheme,
    username = username,
    password = password,
    host = host.orEmpty(),
    port = port,
    path = path.orEmpty(),
    query = query,
    fragment = fragment,
)
