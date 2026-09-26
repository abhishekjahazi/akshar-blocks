package com.kirtigames.abcd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentTest {

    @Test
    fun parsesAllColumns() {
        val letters = Content.parse("A\tApple\t🍎\n")
        assertEquals(listOf(Letter("A", "Apple", "🍎")), letters)
    }

    @Test
    fun wordAndPictureAreOptional() {
        val letters = Content.parse("अः\n" + "ष\tषट्कोण\t\n" + "ङ\t\t\n")
        assertEquals(3, letters.size)
        assertNull(letters[0].word)
        assertEquals("षट्कोण", letters[1].word)
        assertNull(letters[1].emoji)
        assertNull(letters[2].word)
    }

    @Test
    fun skipsCommentsBlankLinesAndWindowsLineEndings() {
        val letters = Content.parse("# heading\r\n\r\nB\tBall\t⚽\r\n   \n# end\n")
        assertEquals(listOf(Letter("B", "Ball", "⚽")), letters)
    }
}
