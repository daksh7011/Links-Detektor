package com.slothiesmooth.linksdetektor.internal

/**
 * Applies Unicode normalization form NFKC to [input].
 */
internal expect fun nfkc(input: String): String

/**
 * Converts a host name to its ASCII (Punycode) form.
 *
 * @param host The host name, possibly with non-ASCII labels.
 * @return The ASCII host name.
 * @throws IllegalArgumentException If the host cannot be converted.
 */
internal expect fun idnToAscii(host: String): String
