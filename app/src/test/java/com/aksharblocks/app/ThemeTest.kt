package com.aksharblocks.app

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The light and dark looks. */
class ThemeTest {

    @After
    fun backToLight() = Theme.setDarkForTest(false)

    @Test
    fun textFollowsTheLook() {
        Theme.setDarkForTest(false)
        val lightText = Palette.INK
        Theme.setDarkForTest(true)
        assertNotEquals(lightText, Palette.INK)
        assertEquals(Theme.text, Palette.INK)
    }

    @Test
    fun textStaysReadableOnTheBackground() {
        for (dark in listOf(false, true)) {
            Theme.setDarkForTest(dark)
            assertTrue("dark=$dark", contrast(Theme.text, Theme.background) >= 7.0)
            assertTrue("dark=$dark", contrast(Theme.subtext, Theme.background) >= 4.5)
            // White letters on the dark "find this" block.
            assertTrue("dark=$dark", contrast(Palette.WHITE, Theme.prompt) >= 4.5)
        }
    }

    @Test
    fun printoutsAndSharedPicturesKeepDarkText() {
        Theme.setDarkForTest(true)
        assertEquals(0xFF1D2B53.toInt(), Palette.NAVY)
    }

    private fun luminance(color: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((color shr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
    }

    private fun contrast(a: Int, b: Int): Double {
        val (hi, lo) = listOf(luminance(a), luminance(b)).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }
}
