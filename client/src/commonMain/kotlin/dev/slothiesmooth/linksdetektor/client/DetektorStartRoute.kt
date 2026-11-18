package dev.slothiesmooth.linksdetektor.client

import dev.slothiesmooth.linksdetektor.feature.common.api.AppStartRoute
import dev.slothiesmooth.linksdetektor.feature.detektor.api.DetektorRoute
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/** The app opens on the link detector. */
@Inject
@ContributesBinding(AppScope::class)
class DetektorStartRoute : AppStartRoute {
    override val route: Any = DetektorRoute
}
