package dev.slothiesmooth.linksdetektor.feature.detektor.basic.domain.usecase

import com.slothiesmooth.linksdetektor.LinksDetektor
import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import com.slothiesmooth.linksdetektor.Url
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet

/** Finds the links in a text; a seam so tests can replace the library. */
fun interface LinkEngine {
    fun detect(content: String, options: ImmutableSet<LinksDetektorOptions>): ImmutableList<Url>
}

/** The engine that calls the library. */
@Inject
@ContributesBinding(AppScope::class)
class LibraryLinkEngine : LinkEngine {
    override fun detect(content: String, options: ImmutableSet<LinksDetektorOptions>): ImmutableList<Url> =
        LinksDetektor(content, options).detect()
}
