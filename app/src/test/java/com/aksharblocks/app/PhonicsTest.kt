package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.random.Random

class PhonicsTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    @Test
    fun everyEnglishLetterHasASound() {
        for (track in listOf(Track.ENGLISH, Track.LOWER)) {
            assertTrue(track.name, track.letters.all { !it.sound.isNullOrBlank() })
            assertTrue(GameMode.SOUNDS in track.modes)
        }
        // Capital and small letters make the same sound.
        assertEquals(Track.ENGLISH.letters.map { it.sound }, Track.LOWER.letters.map { it.sound })
        assertEquals("buh", Track.ENGLISH.letters[1].sound)
    }

    @Test
    fun learningALetterSaysItsSound() {
        val b = Track.ENGLISH.letters[1]
        assertEquals("B. B is for Ball. B says buh!", Voice.spokenText(English.learn(b)))
        assertEquals("b. b is for ball. b says buh!", Voice.spokenText(EnglishSmall.learn(Track.LOWER.letters[1])))
        // Numbers have no sound and say what they always said.
        assertEquals("one.", Voice.spokenText(EnglishNumbers.learn(Track.NUMBERS.letters[0])))
    }

    @Test
    fun theSoundsGameAsksBySound() {
        val b = Track.ENGLISH.letters[1]
        val c = Track.ENGLISH.letters[2]
        assertEquals("Which letter says buh?", Voice.spokenText(English.soundAsk(b)))
        assertTrue(Voice.spokenText(English.soundRight(b, Random(1))).endsWith("B says buh!"))
        assertEquals("That is C. Which letter says buh?", Voice.spokenText(English.soundWrong(c, b)))
        assertEquals("Letter sounds", English.soundsTitle)
        // Animals keep their own wording.
        assertEquals("Who says moo?", Voice.spokenText(AnimalWords.soundAsk(Track.ANIMALS.letters.first())))
    }

    @Test
    fun lettersThatSoundAlikeAreKnown() {
        // C and K both say "kuh": the game never offers both for one question (FindView).
        val sounds = Track.ENGLISH.letters.groupBy { it.sound }.filterValues { it.size > 1 }.mapValues { (_, v) -> v.map { it.symbol } }
        assertEquals(mapOf("kuh" to listOf("C", "K")), sounds)
    }
}
