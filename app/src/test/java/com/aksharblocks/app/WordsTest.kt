package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class WordsTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun splitsIntoLetterBlocks() {
        assertEquals(listOf("c", "a", "t"), Words.split("cat"))
        assertEquals(listOf("ज", "ल"), Words.split("जल"))
        // ड़ is ड plus a dot mark: one block, not two.
        assertEquals(listOf("प", "ड़"), Words.split("पड़"))
    }

    @Test
    fun wordListsLoad() {
        assertEquals(20, Track.ENGLISH.words.size)
        assertEquals(12, Track.VYANJAN.words.size)
        assertEquals(22, Track.BARAKHADI.words.size)
        assertTrue(Track.SWAR.words.isEmpty())
    }

    @Test
    fun everyWordIsBuiltFromLettersOfItsTrack() {
        val english = Track.ENGLISH.letters.map { it.symbol.lowercase() }.toSet()
        for (word in Track.ENGLISH.words) {
            assertTrue(word.text, word.letters.all { it in english })
            assertTrue("${word.text} is short enough", word.letters.size in 2..6)
        }
        val hindi = Track.VYANJAN.letters.map { it.symbol }.toSet()
        for (word in Track.VYANJAN.words) {
            assertTrue(word.text, word.letters.all { it in hindi })
            assertTrue("${word.text} is short enough", word.letters.size in 2..6)
        }
        // मात्रा words are built from बारहखड़ी syllables, and each one really uses a matra.
        val syllables = Track.BARAKHADI.letters.map { it.symbol }.toSet()
        for (word in Track.BARAKHADI.words) {
            assertTrue(word.text, word.letters.all { it in syllables })
            assertTrue("${word.text} has a matra", word.letters.any { it !in hindi })
        }
    }

    @Test
    fun wordsAreUniqueAndHavePictures() {
        for (track in listOf(Track.ENGLISH, Track.VYANJAN, Track.BARAKHADI)) {
            val texts = track.words.map { it.text }
            assertEquals(texts.size, texts.toSet().size)
            assertTrue(track.words.all { it.emoji.isNotBlank() })
        }
    }
}
