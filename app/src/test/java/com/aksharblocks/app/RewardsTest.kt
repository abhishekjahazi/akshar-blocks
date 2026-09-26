package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RewardsTest {

    @Test
    fun aStickerEveryFiveStars() {
        assertEquals(0, Stickers.unlocked(0))
        assertEquals(0, Stickers.unlocked(4))
        assertEquals(1, Stickers.unlocked(5))
        assertEquals(2, Stickers.unlocked(12))
        assertEquals(3, Stickers.starsToNext(12))
        assertEquals(5, Stickers.starsToNext(15))
    }

    @Test
    fun albumStopsWhenFull() {
        val full = Stickers.all.size * Stickers.STARS_EACH
        assertEquals(Stickers.all.size, Stickers.unlocked(full + 40))
        assertEquals(0, Stickers.starsToNext(full + 40))
    }

    @Test
    fun stickersAreAllDifferent() {
        assertEquals(Stickers.all.size, Stickers.all.map { it.emoji }.toSet().size)
    }

    @Test
    fun streakGrowsOnNextDayAndResetsAfterAGap() {
        assertEquals("first ever play", 1, Streak.next(lastDay = Long.MIN_VALUE / 2, streak = 0, today = 100))
        assertEquals("again the same day", 3, Streak.next(lastDay = 100, streak = 3, today = 100))
        assertEquals("the next day", 4, Streak.next(lastDay = 100, streak = 3, today = 101))
        assertEquals("missed a day", 1, Streak.next(lastDay = 100, streak = 3, today = 102))
    }

    @Test
    fun streakShownOnlyWhileAlive() {
        assertEquals(3, Streak.current(lastDay = 100, streak = 3, today = 100))
        assertEquals("yesterday still counts", 3, Streak.current(lastDay = 100, streak = 3, today = 101))
        assertEquals(0, Streak.current(lastDay = 100, streak = 3, today = 102))
    }

    private val letters = listOf("b", "d", "p", "q", "m").map { Letter(it) }

    @Test
    fun weakLettersWeighMore() {
        val stats = LetterStats(
            ok = mapOf("b" to 8, "d" to 1),
            miss = mapOf("d" to 4),
            mix = emptyMap(),
        )
        assertTrue(Coach.weight("d", stats) > Coach.weight("p", stats)) // missed often vs never seen
        assertTrue(Coach.weight("p", stats) > Coach.weight("b", stats)) // new vs well known
        assertTrue("known letters still come up", Coach.weight("b", stats) > 0f)
    }

    @Test
    fun weakLetterIsPickedMostOften() {
        val stats = LetterStats(
            ok = mapOf("b" to 9, "p" to 9, "q" to 9, "m" to 9),
            miss = mapOf("d" to 5),
            mix = emptyMap(),
        )
        val random = Random(3)
        val picks = List(1000) { Coach.pickTarget(letters, stats, random).symbol }
        val d = picks.count { it == "d" }
        assertTrue("d picked $d times", d > 600)
        assertTrue("others still appear", picks.toSet().size == letters.size)
    }

    @Test
    fun mixUpsBecomeTheWrongChoices() {
        val stats = LetterStats(
            ok = emptyMap(),
            miss = mapOf("b" to 3),
            mix = mapOf(("b" to "d") to 3, ("p" to "b") to 1),
        )
        val b = letters[0]
        assertEquals(listOf("d", "p"), Coach.mixPartners(b, letters, stats).map { it.symbol })
        val choices = Coach.choices(b, letters, stats, 4, Random(5))
        assertEquals(4, choices.size)
        assertEquals(4, choices.toSet().size)
        assertTrue(choices.map { it.symbol }.containsAll(listOf("b", "d", "p")))
    }

    @Test
    fun noHistoryMeansPlainRandomChoices() {
        val choices = Coach.choices(letters[2], letters, LetterStats.EMPTY, 4, Random(9))
        assertEquals(4, choices.toSet().size)
        assertTrue(letters[2] in choices)
    }
}
