package com.slothiesmooth.linksdetektor.internal

/**
 * Formats a 16-byte IP address the way `InetAddress.getByAddress(bytes).hostAddress` does, so that results do
 * not depend on JVM-only APIs.
 *
 * An exact IPv4-mapped address (`::ffff:a.b.c.d`) is returned as a dotted quad. Everything else is returned as
 * eight lowercase hex groups wrapped in brackets, with no `::` compression and no leading zeros; IPv4-compatible
 * addresses are not unwrapped.
 *
 * @param bytes The 16 bytes of the address.
 * @return The dotted quad for a mapped IPv4 address, otherwise the bracketed IPv6 text.
 */
internal fun formatIp(bytes: ByteArray): String {
    val mapped = (0..9).all { bytes[it].toInt() == 0 } && bytes[10] == 0xff.toByte() && bytes[11] == 0xff.toByte()
    if (mapped) return (12..15).joinToString(".") { (bytes[it].toInt() and 0xff).toString() }
    return (0 until 8).joinToString(":", "[", "]") { group ->
        val high = bytes[group * 2].toInt() and 0xff
        val low = bytes[group * 2 + 1].toInt() and 0xff
        (high shl 8 or low).toString(16)
    }
}
