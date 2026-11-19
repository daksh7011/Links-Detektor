@file:JvmName("MalformedUrlExceptionJvm")

package com.slothiesmooth.linksdetektor

/** `java.net.MalformedURLException`, so JVM and Android catch sites written against 2.x keep working. */
public actual typealias MalformedUrlException = java.net.MalformedURLException
