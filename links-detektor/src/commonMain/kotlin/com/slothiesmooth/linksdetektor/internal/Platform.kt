package com.slothiesmooth.linksdetektor.internal

/**
 * Converts a host name to its ASCII (Punycode) form.
 *
 * @param host The host name, possibly with non-ASCII labels.
 * @return The ASCII host name.
 * @throws IllegalArgumentException If the host cannot be converted.
 */
internal expect fun idnToAscii(host: String): String
