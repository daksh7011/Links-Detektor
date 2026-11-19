package dev.slothiesmooth.linksdetektor.feature.detektor.api

import dev.slothiesmooth.linksdetektor.feature.common.api.Feature
import kotlinx.serialization.Serializable

/** The cross-feature interface of the link detector. */
interface DetektorFeature : Feature

/** The route of the detector screen, the app's start destination. */
@Serializable
data object DetektorRoute
