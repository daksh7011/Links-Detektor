package com.slothiesmooth.linksdetektor

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins that, on the JVM, [MalformedUrlException] is `java.net.MalformedURLException`, so 2.x catch sites keep working.
 */
class MalformedUrlExceptionJvmTest {

    @Test
    fun `existing JVM catch sites still work`() {
        val thrown = assertFailsWith<java.net.MalformedURLException> { Url.create("hello") }
        assertEquals("We couldn't find any urls in string: hello", thrown.message)
    }

    @Test
    fun `MalformedUrlException is the JDK class on the JVM`() {
        assertEquals<KClass<*>>(java.net.MalformedURLException::class, MalformedUrlException::class)
    }
}
