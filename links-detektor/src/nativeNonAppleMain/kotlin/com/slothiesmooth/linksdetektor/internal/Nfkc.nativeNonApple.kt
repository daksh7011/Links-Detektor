package com.slothiesmooth.linksdetektor.internal

import com.slothiesmooth.linksdetektor.internal.nfkc.TableNfkc

internal actual fun nfkc(input: String): String = TableNfkc.normalize(input)
