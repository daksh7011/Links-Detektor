package `in`.technowolf.linksDetekt

import `in`.technowolf.linksDetekt.detector.CharExtensions.isHex
import `in`.technowolf.linksDetekt.detector.CharExtensions.splitByDot
import org.apache.commons.lang3.StringUtils
import java.net.IDN
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale

/**
 * Normalizes the host by converting hex characters to the actual textual representation, changes ip addresses
 * to a formal format. Then re-encodes the final host name.
 */
class HostNormalizer(private val _host: String?) {
    var bytes: ByteArray? = null
        private set
    var normalizedHost: String? = null
        private set

    init {
        normalizeHost()
    }

    private fun normalizeHost() {
        if (StringUtils.isEmpty(_host)) {
            return
        }
        var host: String
        host = try {
            // replace high unicode characters
            IDN.toASCII(_host)
        } catch (ex: IllegalArgumentException) {
            // occurs when the url is invalid. Just return
            return
        }
        host = host.lowercase(Locale.getDefault())
        host = UrlUtil.decode(host)
        bytes = tryDecodeHostToIp(host)
        if (bytes != null) {
            val address: InetAddress
            try {
                address = InetAddress.getByAddress(bytes)
                val ipAddress: String = address.hostAddress
                host = if (address is Inet6Address) {
                    "[$ipAddress]"
                } else {
                    ipAddress
                }
            } catch (e: UnknownHostException) {
                return
            }
        }
        if (StringUtils.isEmpty(host)) {
            return
        }
        host = UrlUtil.removeExtraDots(host)
        normalizedHost = UrlUtil.encode(host).replace("\\x", "%")
    }

    /**
     * Checks if the host is an ip address. Returns the byte representation of it
     */
    private fun tryDecodeHostToIp(host: String): ByteArray? {
        return if (host.startsWith("[") && host.endsWith("]")) {
            tryDecodeHostToIPv6(host)
        } else tryDecodeHostToIPv4(host)
    }

    /**
     * This covers cases like:
     * Hexadecimal: 0x1283983
     * Decimal: 12839273
     * Octal: 037362273110
     * Dotted Decimal: 192.168.1.1
     * Dotted Hexadecimal: 0xfe.0x83.0x18.0x1
     * Dotted Octal: 0301.00.046.00
     * Dotted Mixed: 0x38.168.077.1
     *
     * if ipv4 was found, _bytes is set to the byte representation of the ipv4 address
     */
    private fun tryDecodeHostToIPv4(host: String): ByteArray? {
        val parts: Array<String> = host.splitByDot()
        val numParts = parts.size
        if (numParts != 4 && numParts != 1) {
            return null
        }
        val bytes = ByteArray(16)

        // An ipv4 mapped ipv6 bytes will have the 11th and 12th byte as 0xff
        bytes[10] = 0xff.toByte()
        bytes[11] = 0xff.toByte()
        for (i in parts.indices) {
            var parsedNum: String
            var base: Int
            if (parts[i].startsWith("0x")) { // hex
                parsedNum = parts[i].substring(2)
                base = 16
            } else if (parts[i].startsWith("0")) { // octal
                parsedNum = parts[i].substring(1)
                base = 8
            } else { // decimal
                parsedNum = parts[i]
                base = 10
            }
            var section: Long
            section = try {
                if (parsedNum.isEmpty()) 0 else parsedNum.toLong(base)
            } catch (e: NumberFormatException) {
                return null
            }
            if (numParts == 4 && section > MAX_IPV4_PART || // This would look like 288.1.2.4
                numParts == 1 && section > MAX_NUMERIC_DOMAIN_VALUE || // This would look like 4294967299
                section < MIN_IP_PART
            ) {
                return null
            }
            // bytes 13->16 is where the ipv4 address of an ipv4-mapped-ipv6-address is stored.
            if (numParts == 4) {
                bytes[IPV4_MAPPED_IPV6_START_OFFSET + i] = section.toByte()
            } else { // numParts == 1
                var index = IPV4_MAPPED_IPV6_START_OFFSET
                bytes[index++] = (section shr 24 and 0xFFL).toByte()
                bytes[index++] = (section shr 16 and 0xFFL).toByte()
                bytes[index++] = (section shr 8 and 0xFFL).toByte()
                bytes[index] = (section and 0xFFL).toByte()
                return bytes
            }
        }
        return bytes
    }

    /**
     * Recommendation for IPv6 Address Text Representation
     * http://tools.ietf.org/html/rfc5952
     *
     * if ipv6 was found, _bytes is set to the byte representation of the ipv6 address
     */
    private fun tryDecodeHostToIPv6(host: String): ByteArray? {
        val ip = host.substring(1, host.length - 1)
        val parts: List<String> = ArrayList(listOf(*ip.split(":".toRegex()).toTypedArray()))
        if (parts.size < 3) {
            return null
        }

        // Check for embedded ipv4 address
        val lastPart = parts[parts.size - 1]
        val zoneIndexStart = lastPart.lastIndexOf("%")
        val lastPartWithoutZoneIndex = if (zoneIndexStart == -1) lastPart else lastPart.substring(0, zoneIndexStart)
        var ipv4Address: ByteArray? = null
        if (!isHexSection(lastPartWithoutZoneIndex)) {
            ipv4Address = tryDecodeHostToIPv4(lastPartWithoutZoneIndex)
        }
        val bytes = ByteArray(16)
        // How many parts do we need to fill by the end of this for loop?
        val totalSize = if (ipv4Address == null) 8 else 6
        // How many zeroes did we fill in the case of double colons? Ex: [::1] will have numberOfFilledZeroes = 7
        var numberOfFilledZeroes = 0
        // How many sections do we have to parse through? Ex: [fe80:ff::192.168.1.1] size = 3, another ex: [a:a::] size = 4
        val size = if (ipv4Address == null) parts.size else parts.size - 1
        for (i in 0 until size) {
            val lenPart = parts[i].length
            if (lenPart == 0 && i != 0 && i != parts.size - 1) {
                numberOfFilledZeroes = totalSize - size
                for (k in i until numberOfFilledZeroes + i) {
                    System.arraycopy(sectionToTwoBytes(0), 0, bytes, k * 2, 2)
                }
            }
            var section: Int
            section = try {
                if (lenPart == 0) 0 else parts[i].toInt(16)
            } catch (e: NumberFormatException) {
                return null
            }
            if (section > MAX_IPV6_PART || section < MIN_IP_PART) {
                return null
            }
            System.arraycopy(sectionToTwoBytes(section), 0, bytes, (numberOfFilledZeroes + i) * 2, 2)
        }
        if (ipv4Address != null) {
            System.arraycopy(
                ipv4Address, IPV4_MAPPED_IPV6_START_OFFSET, bytes, IPV4_MAPPED_IPV6_START_OFFSET,
                NUMBER_BYTES_IN_IPV4
            )
        }
        return bytes
    }

    companion object {
        private const val MAX_NUMERIC_DOMAIN_VALUE = 4294967295L
        private const val MAX_IPV4_PART = 255
        private const val MIN_IP_PART = 0
        private const val MAX_IPV6_PART = 0xFFFF
        private const val IPV4_MAPPED_IPV6_START_OFFSET = 12
        private const val NUMBER_BYTES_IN_IPV4 = 4
        private fun isHexSection(section: String): Boolean {
            for (element in section) {
                if (element.isHex().not()) {
                    return false
                }
            }
            return true
        }

        private fun sectionToTwoBytes(section: Int): ByteArray {
            val bytes = ByteArray(2)
            bytes[0] = (section shr 8 and 0xff).toByte()
            bytes[1] = (section and 0xff).toByte()
            return bytes
        }
    }
}
