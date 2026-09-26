package com.kirtigames.abcd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class AlphabetTest {

    @Test
    fun englishHasEveryLetterInOrder() {
        assertEquals(('A'..'Z').map { it.toString() }, Letters.english.map { it.symbol })
    }

    @Test
    fun hindiHasAllVowelsAndConsonants() {
        assertEquals(13, Letters.swar.size)
        assertEquals(36, Letters.vyanjan.size)
        assertEquals("अ", Letters.swar.first().symbol)
        assertEquals("ज्ञ", Letters.vyanjan.last().symbol)
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
        for (track in Track.entries) {
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
        for (track in Track.entries) {
            for (letter in track.letters) {
                val word = letter.word ?: continue
                assertFalse("${letter.symbol} / $word", word.substring(0, letter.headLength).endsWith("्"))
            }
        }
    }

    @Test
    fun everyTrackHasEnoughPicturesToPlay() {
        for (track in Track.entries) {
            assertTrue(track.name, track.pictureLetters.size >= 3)
        }
    }

    @Test
    fun choicesContainTargetAndAreDistinct() {
        val random = Random(42)
        for (track in Track.entries) {
            for (target in track.letters) {
                val choices = Pick.choices(target, track.letters, 4, random)
                assertEquals(4, choices.size)
                assertEquals(4, choices.toSet().size)
                assertTrue(target in choices)
            }
        }
    }

    @Test
    fun anyExceptSkipsExcludedItem() {
        val random = Random(7)
        val excluded = Letters.swar[3]
        repeat(200) {
            assertNotEquals(excluded, Pick.anyExcept(Letters.swar, excluded, random))
        }
    }
}
