package com.passion.knownow.core.parsing

import org.junit.Assert.assertEquals
import org.junit.Test

class TocParserTest {
    @Test
    fun parsesNestedEntries() {
        val md = """
            # Title
            - [A](https://example.com/a.md) -- desc A
              - [B](https://example.com/b.md) -- desc B
        """.trimIndent()
        val entries = TocParser.parse(md)
        assertEquals(2, entries.size)
        assertEquals(0, entries[0].depth)
        assertEquals("A", entries[0].title)
        assertEquals(1, entries[1].depth)
        assertEquals("B", entries[1].title)
    }
}