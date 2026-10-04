package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Fruits, body and family pictures, maths, the reward shop and worksheets (version 1.5). */
class NewThingsTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun newPictureTracksLoadWithHindiNames() {
        assertEquals(18, Track.FRUITS.letters.size)
        assertEquals(12, Track.BODY.letters.size)
        assertEquals(8, Track.FAMILY.letters.size)
        assertEquals(Letter("🥭", "Mango", "🥭", hindi = "आम"), Track.FRUITS.letters[2])
        assertEquals("Show me the|Nose!|@hi नाक!", BodyWords.find(Track.BODY.letters.first { it.word == "Nose" }))
    }

    @Test
    fun theWorldHoldsEveryPictureTrack() {
        assertEquals(
            listOf(Track.COLORS, Track.SHAPES, Track.ANIMALS, Track.FRUITS, Track.BODY, Track.FAMILY),
            Track.entries.filter { it.isPictures },
        )
        // Home: the other tracks, World, Maths, Rhymes and My name fill four rows of three.
        assertEquals(12, Track.entries.count { !it.isPictures } + 4)
    }

    @Test
    fun sumsStayWithinTen() {
        val random = Random(7)
        repeat(500) {
            val add = Maths.add(random)
            assertTrue(add.a in 1..5 && add.b in 1..5 && add.total <= Maths.MAX)
            val take = Maths.takeAway(random)
            assertTrue("$take", take.b >= 1 && take.left >= 1 && take.a <= Maths.MAX)
            val compare = Maths.compare(random)
            assertTrue("$compare", compare.a != compare.b && compare.a in 1..9 && compare.b in 1..9)
        }
    }

    @Test
    fun choicesHoldTheAnswerAndTwoOthers() {
        val random = Random(3)
        for (answer in 0..Maths.MAX) {
            val choices = Maths.choices(answer, random)
            assertEquals(3, choices.toSet().size)
            assertTrue(answer in choices)
            assertTrue(choices.all { it in 0..Maths.MAX })
            assertEquals(choices.sorted(), choices)
        }
    }

    @Test
    fun patternsRepeatAndTheAnswerContinuesThem() {
        val random = Random(11)
        repeat(300) {
            val p = Maths.pattern(random)
            val full = p.shown + p.answer
            // Some unit length of 2 or 3 repeats through the whole row.
            val unit = (2..3).firstOrNull { n -> full.indices.all { full[it] == full[it % n] } }
            assertTrue("$full", unit != null)
            assertTrue(p.shown.size >= unit!! * 2)
            assertEquals(3, p.choices.toSet().size)
            assertTrue(p.answer in p.choices)
        }
    }

    @Test
    fun mathsSpeaksEnglishThenHindi() {
        assertEquals("3 and 2 make 5!|@hi तीन और दो, पाँच!", MathWords.addRight(Sum(3, 2)))
        assertEquals("7 is more than 4!|@hi सात, चार से ज़्यादा!", MathWords.compareRight(Sum(4, 7), more = true))
        assertEquals("4 is less than 7!|@hi चार, सात से कम!", MathWords.compareRight(Sum(4, 7), more = false))
        val fish = Counting.things.first { it.emoji == "🐟" }
        assertTrue(MathWords.takeAwayAsk(fish).endsWith("कितनी बचीं?"))
        val hindi = VoiceScript.lines().getValue("hi").map { it.text }
        assertTrue("पाँच में से दो गए, तीन बचे!" in hindi)
    }

    @Test
    fun wearingOneThingPerSlot() {
        val (bow, cap, glasses) = listOf("bow", "cap", "glasses").map { Outfits.byId(it)!! }
        var wearing = Outfits.toggle(emptyList(), bow)
        wearing = Outfits.toggle(wearing, glasses)
        wearing = Outfits.toggle(wearing, cap)
        assertEquals(setOf(cap, glasses), wearing.toSet()) // the cap replaced the bow
        assertEquals(listOf(glasses), Outfits.toggle(wearing, cap)) // tapping again takes it off
        assertEquals(listOf(bow, glasses), Outfits.decode(Outfits.encode(listOf(bow, glasses)) + ",unknown"))
        assertEquals(Outfits.all.size, Outfits.all.map { it.id }.toSet().size)
        assertFalse(Outfits.all.any { ',' in it.id })
    }

    @Test
    fun worksheetsHaveEveryLetter() {
        assertEquals(('A'..'Z').map { "$it" }, Worksheet.CAPITALS.symbols)
        assertEquals((1..20).map { "$it" }, Worksheet.NUMBERS.symbols)
        assertEquals("ज्ञ", Worksheet.VYANJAN.symbols.last())
        for (sheet in Worksheet.entries) {
            assertEquals(sheet.name, sheet.count, sheet.symbols.size)
            assertEquals(sheet.symbols, sheet.pages.flatten())
            assertTrue(sheet.pages.all { it.size <= Worksheet.ROWS_PER_PAGE })
        }
    }
}
