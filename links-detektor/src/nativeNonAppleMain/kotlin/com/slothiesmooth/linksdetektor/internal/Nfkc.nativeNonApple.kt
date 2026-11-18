package com.slothiesmooth.linksdetektor.internal

import doist.x.normalize.Form
import doist.x.normalize.normalize

internal actual fun nfkc(input: String): String = input.normalize(Form.NFKC)
