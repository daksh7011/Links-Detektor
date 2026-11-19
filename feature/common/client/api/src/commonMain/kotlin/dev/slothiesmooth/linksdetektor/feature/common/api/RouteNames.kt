package dev.slothiesmooth.linksdetektor.feature.common.api

import kotlinx.serialization.serializer

/** The serialized name of a `@Serializable` route; routes are matched by this string so it behaves the same on every target. */
inline fun <reified T : Any> routeNameOf(): String = serializer<T>().descriptor.serialName
