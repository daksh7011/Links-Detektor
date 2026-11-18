package com.slothiesmooth.linksdetektor.internal

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GapBufferTest {

    @Test
    fun readsBackItsText() {
        val buffer = GapBuffer("abc")
        assertEquals(3, buffer.length)
        assertEquals('a', buffer[0])
        assertEquals('c', buffer[2])
        assertEquals('b', buffer.getOrNull(1))
        assertNull(buffer.getOrNull(3))
        assertNull(buffer.getOrNull(-1))
        assertEquals("abc", buffer.toString())
    }

    @Test
    fun deletesRangesAnywhere() {
        val buffer = GapBuffer("0123456789")
        buffer.deleteRange(7, 9)
        buffer.deleteRange(0, 2)
        buffer.deleteAt(3)
        buffer.deleteRange(2, 2)
        assertEquals("23469", buffer.toString())
        assertEquals(5, buffer.length)
        assertEquals('9', buffer[4])
    }

    @Test
    fun insertsAfterDeletingAndWhenFull() {
        val buffer = GapBuffer("%41x")
        buffer.deleteRange(0, 3)
        buffer.insert(0, 'A')
        assertEquals("Ax", buffer.toString())
        buffer.append('!')
        repeat(40) { buffer.insert(1, '-') }
        assertEquals("A" + "-".repeat(40) + "x!", buffer.toString())
    }

    @Test
    fun growsFromEmpty() {
        val buffer = GapBuffer("")
        assertTrue(buffer.isEmpty())
        buffer.append('/')
        assertEquals("/", buffer.toString())
    }

    @Test
    fun rejectsOutOfRangeAccess() {
        val buffer = GapBuffer("ab")
        assertFailsWith<IndexOutOfBoundsException> { buffer[2] }
        assertFailsWith<IndexOutOfBoundsException> { buffer[-1] }
        assertFailsWith<IndexOutOfBoundsException> { buffer.deleteRange(1, 3) }
        assertFailsWith<IndexOutOfBoundsException> { buffer.deleteRange(2, 1) }
        assertFailsWith<IndexOutOfBoundsException> { buffer.insert(3, 'x') }
    }

    /** Every edit gives the same text as the same edit on a [StringBuilder]. */
    @Test
    fun matchesStringBuilderOnRandomEdits() {
        val random = Random(20261119)
        repeat(300) {
            val start = buildString { repeat(random.nextInt(0, 30)) { append('a' + random.nextInt(26)) } }
            val expected = StringBuilder(start)
            val buffer = GapBuffer(start)
            repeat(60) {
                when (random.nextInt(3)) {
                    0 -> {
                        val index = random.nextInt(0, expected.length + 1)
                        val char = 'A' + random.nextInt(26)
                        expected.insert(index, char)
                        buffer.insert(index, char)
                    }
                    1 -> {
                        val from = random.nextInt(0, expected.length + 1)
                        val to = random.nextInt(from, expected.length + 1)
                        expected.deleteRange(from, to)
                        buffer.deleteRange(from, to)
                    }
                    else -> if (expected.isNotEmpty()) {
                        val index = random.nextInt(expected.length)
                        assertEquals(expected[index], buffer[index])
                    }
                }
                assertEquals(expected.length, buffer.length)
            }
            assertEquals(expected.toString(), buffer.toString())
        }
    }
}
