package com.slothiesmooth.linksdetektor.internal

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.precomposedStringWithCanonicalMapping
import platform.Foundation.precomposedStringWithCompatibilityMapping

// Foundation leaves the voiced mark of half-width katakana (U+FF76 U+FF9E) decomposed after the compatibility
// mapping, so a canonical composition pass follows to get true NFKC.
@OptIn(BetaInteropApi::class)
internal actual fun nfkc(input: String): String {
    val compatible = NSString.create(string = input).precomposedStringWithCompatibilityMapping
    return NSString.create(string = compatible).precomposedStringWithCanonicalMapping
}
