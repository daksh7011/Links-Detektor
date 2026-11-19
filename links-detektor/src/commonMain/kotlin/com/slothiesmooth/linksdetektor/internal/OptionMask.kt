package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.LinksDetektorOptions
import kotlinx.collections.immutable.ImmutableSet
import kotlin.jvm.JvmInline

/**
 * The bitwise union of a set of [LinksDetektorOptions], with a single place that implements the bit test.
 *
 * @property value The combined bit value of all options in the mask.
 */
@JvmInline
internal value class OptionMask(val value: Int) {

    /**
     * Whether every bit of [flag] is set in this mask.
     */
    fun has(flag: LinksDetektorOptions): Boolean = value and flag.value == flag.value

    companion object {
        /**
         * Combines [options] with a bitwise OR. An empty set yields the [LinksDetektorOptions.Default] mask.
         */
        fun of(options: ImmutableSet<LinksDetektorOptions>): OptionMask =
            OptionMask(options.fold(LinksDetektorOptions.Default.value) { acc, option -> acc or option.value })
    }
}
