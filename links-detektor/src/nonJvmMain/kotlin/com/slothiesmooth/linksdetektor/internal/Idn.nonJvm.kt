package com.slothiesmooth.linksdetektor.internal

internal actual fun idnToAscii(host: String): String = PureIdn.toAscii(host)
