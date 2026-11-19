package com.slothiesmooth.linksdetektor

/**
 * Thrown when a string cannot be turned into exactly one URL.
 *
 * On JVM and Android this is `java.net.MalformedURLException`, so existing catch sites keep working.
 */
public expect open class MalformedUrlException(message: String) : Exception
