package com.slothiesmooth.linksdetektor.internal

import java.net.IDN
import java.text.Normalizer

/**
 * Applies Unicode normalization form NFKC to [input].
 */
internal fun nfkc(input: String): String = Normalizer.normalize(input, Normalizer.Form.NFKC)

/**
 * Converts a host name to its ASCII (Punycode) form.
 *
 * @param host The host name, possibly with non-ASCII labels.
 * @return The ASCII host name.
 * @throws IllegalArgumentException If the host cannot be converted.
 */
internal fun idnToAscii(host: String): String = IDN.toASCII(host)
