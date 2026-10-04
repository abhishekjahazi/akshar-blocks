package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** The opposites game (बड़ा – छोटा). */
class OppositesTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun pairsLoadWithBothNames() {
        assertEquals(10, Opposites.pairs.size)
        assertEquals(OppositePair(Side("🐘", "Big", "बड़ा"), Side("🐭", "Small", "छोटा")), Opposites.pairs.first())
        val sides = Opposites.pairs.flatMap { listOf(it.a, it.b) }
        // Every picture and every word is used once, so a picture can't be the answer to two questions.
        assertEquals(sides.size, sides.map { it.emoji }.toSet().size)
        assertEquals(sides.size, sides.map { it.english }.toSet().size)
        assertEquals(sides.size, sides.map { it.hindi }.toSet().size)
    }

    @Test
    fun everyRoundHasExactlyOneRightChoice() {
        val random = Random(5)
        var previous: OppositePair? = null
        repeat(400) {
            val round = Opposites.round(random, previous)
            val pair = Opposites.pairOf(round.shown)
            assertNotEquals(previous, pair)
            assertTrue(round.answer == pair.a || round.answer == pair.b)
            assertNotEquals(round.shown, round.answer)
            assertEquals(Opposites.CHOICES, round.choices.toSet().size)
            assertEquals(listOf(round.answer), round.choices.filter { Opposites.pairOf(it) == pair })
            assertTrue(round.shown !in round.choices)
            previous = pair
        }
    }

    @Test
    fun saysThePairInEnglishThenHindi() {
        val (big, small) = Opposites.pairs.first().let { it.a to it.b }
        assertEquals("Big and small!|@hi बड़ा और छोटा!", OppositeWords.right(big, small))
        assertEquals(
            listOf(Voice.Segment("en", "Big!"), Voice.Segment("hi", "बड़ा!"), Voice.Segment("en", "What is the opposite?"), Voice.Segment("hi", "इसका उल्टा क्या है?")),
            Voice.segments(OppositeWords.ask(big), "en"),
        )
        val hindi = VoiceScript.lines().getValue("hi").map { it.text }
        assertTrue("छोटा और बड़ा!" in hindi)
    }

    @Test
    fun everyPictureHasArtwork() {
        val art = File("src/main/assets/art").list().orEmpty().toSet()
        val missing = Opposites.pairs.flatMap { listOf(it.a.emoji, it.b.emoji) }.filter { Art.fileName(it) !in art }
        assertTrue("no artwork for $missing", missing.isEmpty())
    }
}
