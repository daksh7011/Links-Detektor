package com.slothiesmooth.linksdetektor.internal

import java.net.IDN
import java.text.Normalizer

internal actual fun nfkc(input: String): String = Normalizer.normalize(input, Normalizer.Form.NFKC)

internal actual fun idnToAscii(host: String): String = IDN.toASCII(host)
