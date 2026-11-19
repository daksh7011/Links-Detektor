package dev.slothiesmooth.linksdetektor.desktop

import dev.slothiesmooth.linksdetektor.client.BaseAppGraph
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph

@DependencyGraph(AppScope::class)
interface DesktopAppGraph : BaseAppGraph
