package com.slothiesmooth.linksdetektor

/** Thrown when a string cannot be turned into exactly one URL. */
public actual open class MalformedUrlException actual constructor(message: String) : Exception(message)
