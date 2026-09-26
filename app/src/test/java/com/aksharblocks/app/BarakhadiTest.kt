package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class BarakhadiTest {

    companion object {
        @JvmStatic
        @BeforeClass
        fun loadContent() {
            Content.loadFrom(File("src/main/assets"))
        }
    }

    private val syllables get() = Track.BARAKHADI.letters

    @Test
    fun twelveMatras() {
        assertEquals(12, Barakhadi.matras.size)
        assertEquals("अ", Barakhadi.matras.first().vowel)
        assertEquals("", Barakhadi.matras.first().sign)
    }

    @Test
    fun rowForKa() {
        val ka = syllables.filter { it.group == "क" }.map { it.symbol }
        assertEquals(listOf("क", "का", "कि", "की", "कु", "कू", "के", "कै", "को", "कौ", "कं", "कः"), ka)
    }

    @Test
    fun everyConsonantExceptNgaAndNyaHasARow() {
        val groups = syllables.mapNotNull { it.group }.distinct()
        assertEquals(Track.VYANJAN.letters.size - 2, groups.size)
        assertFalse("ङ" in groups)
        assertFalse("ञ" in groups)
        assertEquals(groups.size * 12, syllables.size)
        assertEquals("ksha row", "क्षि", syllables.first { it.group == "क्ष" && Barakhadi.matraOf(it).vowel == "इ" }.symbol)
    }

    @Test
    fun matraOfFindsTheSignUsed() {
        for (syllable in syllables) {
            val matra = Barakhadi.matraOf(syllable)
            assertEquals(syllable.symbol, syllable.group + matra.sign)
        }
    }

    @Test
    fun wrongChoicesComeFromTheSameConsonant() {
        val target = syllables.first { it.symbol == "की" }
        val pool = Track.BARAKHADI.choicePool(target)
        assertEquals(12, pool.size)
        assertTrue(pool.all { it.group == "क" })
    }

    @Test
    fun lookAlikesArePairs() {
        val ii = Barakhadi.matras.first { it.vowel == "ई" }
        assertEquals("इ", Barakhadi.lookAlike(ii)?.vowel)
        assertEquals("ई", Barakhadi.lookAlike(Barakhadi.lookAlike(ii)!!)?.vowel)
    }

    @Test
    fun matraShownOnDottedCircle() {
        assertEquals("◌ी", Barakhadi.matras.first { it.vowel == "ई" }.shown)
        assertEquals("अ", Barakhadi.matras.first().shown)
    }
}
