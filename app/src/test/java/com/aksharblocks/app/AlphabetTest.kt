package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.random.Random

class AlphabetTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            // Unit tests run from the app module folder, so this reads the real content files.
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun englishHasEveryLetterInOrder() {
        assertEquals(('A'..'Z').map { it.toString() }, Track.ENGLISH.letters.map { it.symbol })
    }

    @Test
    fun hindiHasAllVowelsAndConsonants() {
        assertEquals(13, Track.SWAR.letters.size)
        assertEquals(36, Track.VYANJAN.letters.size)
        assertEquals("अ", Track.SWAR.letters.first().symbol)
        assertEquals("ज्ञ", Track.VYANJAN.letters.last().symbol)
    }

    @Test
    fun symbolsAreUniqueWithinEachTrack() {
        for (track in Track.entries) {
            val symbols = track.letters.map { it.symbol }
            assertEquals(track.name, symbols.size, symbols.toSet().size)
        }
    }

    @Test
    fun everyWordStartsWithItsLetter() {
        for (track in Track.entries.filter { !it.isNumbers && !it.isPictures }) {
            for (letter in track.letters) {
                val word = letter.word ?: continue
                assertTrue("${letter.symbol} / $word", word.startsWith(letter.symbol, ignoreCase = true))
            }
        }
    }

    @Test
    fun headIncludesVowelSigns() {
        assertEquals(2, Letter("ग", "गाय").headLength)   // गा
        assertEquals(2, Letter("झ", "झंडा").headLength)  // झं
        assertEquals(1, Letter("क", "कबूतर").headLength)
        assertEquals(3, Letter("क्ष", "क्षत्रिय").headLength)
        assertEquals(1, Letter("A", "Apple").headLength)
    }

    @Test
    fun headNeverSplitsAConjunct() {
        // Ending on a virama (्) would draw half a conjunct in one color and half in another.
        for (track in Track.entries.filter { !it.isNumbers }) {
            for (letter in track.letters) {
                val word = letter.word ?: continue
                assertFalse("${letter.symbol} / $word", word.substring(0, letter.headLength).endsWith("्"))
            }
        }
    }

    @Test
    fun everyTrackHasEnoughPicturesToPlay() {
        for (track in Track.entries.filter { GameMode.MATCH in it.modes }) {
            assertTrue(track.name, track.pictureLetters.size >= 3)
        }
    }

    @Test
    fun choicesContainTargetAndAreDistinct() {
        val random = Random(42)
        for (track in Track.entries) {
            for (target in track.letters) {
                val choices = Pick.choices(target, track.choicePool(target), 4, random)
                assertEquals(4, choices.size)
                assertEquals(4, choices.toSet().size)
                assertTrue(target in choices)
            }
        }
    }

    @Test
    fun anyExceptSkipsExcludedItem() {
        val random = Random(7)
        val excluded = Track.SWAR.letters[3]
        repeat(200) {
            assertNotEquals(excluded, Pick.anyExcept(Track.SWAR.letters, excluded, random))
        }
    }
}
