package com.slothiesmooth.linksdetektor.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class InputTextReaderTest {

    @Test
    fun peekCharBeyondEndThrows() {
        val thrown = assertFailsWith<IndexOutOfBoundsException> { InputTextReader("ab").peekChar(3) }
        assertEquals(IndexOutOfBoundsException::class, thrown::class)
    }

    @Test
    fun excessiveBacktrackThrowsIllegalState() {
        val reader = InputTextReader("ab")
        reader.read()
        val thrown = assertFailsWith<IllegalStateException> {
            // The limit is content length (2) * 10 = 20 backtracked characters; the 21st goBack throws.
            repeat(21) {
                reader.goBack()
                reader.read()
            }
        }
        assertTrue(
            thrown.message.orEmpty().startsWith(
                "Backtracked max amount of characters. Endless loop detected. Bad Text: '"
            ),
            "message: ${thrown.message}"
        )
    }
}
