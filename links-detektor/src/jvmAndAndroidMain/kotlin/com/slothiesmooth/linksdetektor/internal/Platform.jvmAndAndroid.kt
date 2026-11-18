package com.slothiesmooth.linksdetektor.internal

import java.net.IDN

internal actual fun idnToAscii(host: String): String = IDN.toASCII(host)
