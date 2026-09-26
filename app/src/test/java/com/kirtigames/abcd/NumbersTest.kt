package com.kirtigames.abcd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.random.Random

class NumbersTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }

        private const val DEVANAGARI_DIGITS = "०१२३४५६७८९"
    }

    @Test
    fun englishNumbersAreOneToHundredWithWords() {
        val numbers = Track.NUMBERS.letters
        assertEquals((1..100).map { it.toString() }, numbers.map { it.symbol })
        assertEquals("seven", numbers[6].word)
        assertEquals("twenty-one", numbers[20].word)
        assertEquals("one hundred", numbers[99].word)
    }

    @Test
    fun hindiNumbersUseDevanagariDigits() {
        val ginti = Track.GINTI.letters
        assertEquals(100, ginti.size)
        ginti.forEachIndexed { i, letter ->
            val expected = (i + 1).toString().map { DEVANAGARI_DIGITS[it - '0'] }.joinToString("")
            assertEquals(expected, letter.symbol)
        }
    }

    @Test
    fun hindiNumberWords() {
        val words = Track.GINTI.letters.map { it.word }
        assertEquals("एक", words[0])
        assertEquals("दस", words[9])
        assertEquals("उनचास", words[48])
        assertEquals("निन्यानवे", words[98])
        assertEquals("सौ", words[99])
        assertEquals("every word is different", 100, words.toSet().size)
    }

    @Test
    fun wrongChoicesAreNearbyNumbers() {
        val numbers = Track.NUMBERS.letters
        val target = numbers[26] // 27
        val pool = Track.NUMBERS.choicePool(target)
        assertTrue(target in pool)
        assertTrue(pool.all { kotlin.math.abs(it.symbol.toInt() - 27) <= 6 })
        // Still enough to fill four choices at the very start of the list.
        assertTrue(Track.NUMBERS.choicePool(numbers[0]).size >= 4)
    }

    @Test
    fun numbersAreSpokenAsNumbersNotLetters() {
        val seven = Track.GINTI.letters[6]
        assertEquals("सात ढूंढो!", Voice.spokenText(HindiNumbers.find(seven)))
        assertEquals("Find the number seven!", Voice.spokenText(EnglishNumbers.find(Track.NUMBERS.letters[6])))
    }

    @Test
    fun countingPhrases() {
        val apples = Counting.things.first()
        val random = Random(1)
        assertEquals("How many apples?", English.countAsk(apples))
        assertTrue(Voice.spokenText(EnglishNumbers.countRight(Track.NUMBERS.letters[0], apples, random)).endsWith("one apple!"))
        assertTrue(Voice.spokenText(EnglishNumbers.countRight(Track.NUMBERS.letters[6], apples, random)).endsWith("seven apples!"))
        assertTrue(Voice.spokenText(HindiNumbers.countRight(Track.GINTI.letters[6], apples, random)).endsWith("सात सेब!"))
    }
}
