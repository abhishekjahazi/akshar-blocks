package com.aksharblocks.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyPathTest {

    private fun shares(vararg known: Pair<Track, Float>): (Track) -> Float {
        val map = known.toMap()
        return { map[it] ?: 0f }
    }

    @Test
    fun aNewChildStartsWithLearningEnglishAndHindiVowels() {
        val plan = DailyPath.plan(day = 0, share = shares())
        assertEquals(
            listOf(
                PathStep(Track.ENGLISH, GameMode.LEARN), plan[1],
                PathStep(Track.SWAR, GameMode.LEARN), plan[3],
            ),
            plan,
        )
    }

    @Test
    fun everyDayHasFourStepsTheTracksReallyOffer() {
        for (day in 0L until 60L) {
            for (known in listOf(0f, 0.4f, 0.6f, 0.9f, 1f)) {
                val plan = DailyPath.plan(day) { known }
                assertEquals(DailyPath.STEPS, plan.size)
                plan.forEach { assertTrue("$day: $it", it.mode in it.track.modes) }
                // Each section gets one learning game and a different practice game.
                assertNotEquals(plan[0].mode, plan[1].mode)
                assertNotEquals(plan[2].mode, plan[3].mode)
                assertEquals(plan[0].track, plan[1].track)
                assertEquals(plan[2].track, plan[3].track)
            }
        }
    }

    @Test
    fun sectionsTakeTurnsOverThreeDays() {
        val groups = (0L until 3L).map { day -> DailyPath.plan(day, shares()).map { it.track }.distinct() }
        assertEquals(listOf(Track.ENGLISH, Track.SWAR), groups[0])
        assertEquals(listOf(Track.SWAR, Track.NUMBERS), groups[1])
        assertEquals(listOf(Track.ENGLISH, Track.NUMBERS), groups[2])
    }

    @Test
    fun theNextTrackWaitsUntilTheFirstIsMastered() {
        val english = listOf(Track.ENGLISH, Track.LOWER)
        assertEquals(Track.ENGLISH, DailyPath.pick(english, shares(Track.ENGLISH to 0.6f)))
        assertEquals(Track.LOWER, DailyPath.pick(english, shares(Track.ENGLISH to 0.85f)))
        // Small letters can't come before half the capitals are known, even when everything else is done.
        assertEquals(Track.ENGLISH, DailyPath.pick(english, shares(Track.ENGLISH to 0.4f, Track.LOWER to 1f)))
    }

    @Test
    fun whenEverythingIsMasteredTheWeakestComesBack() {
        val hindi = listOf(Track.SWAR, Track.VYANJAN, Track.BARAKHADI)
        assertEquals(Track.VYANJAN, DailyPath.pick(hindi, shares(Track.SWAR to 1f, Track.VYANJAN to 0.85f, Track.BARAKHADI to 0.9f)))
    }

    @Test
    fun learnGivesWayToTraceOnceLettersAreKnown() {
        assertEquals(GameMode.LEARN, DailyPath.plan(0, shares(Track.ENGLISH to 0.2f))[0].mode)
        assertEquals(GameMode.TRACE, DailyPath.plan(0, shares(Track.ENGLISH to 0.5f))[0].mode)
    }

    @Test
    fun barakhadiIsLearnedByBuildingSyllables() {
        val plan = DailyPath.plan(0, shares(Track.SWAR to 1f, Track.VYANJAN to 0.9f))
        assertEquals(PathStep(Track.BARAKHADI, GameMode.BUILD), plan[2])
        assertEquals(PathStep(Track.BARAKHADI, GameMode.FIND), plan[3])
    }

    @Test
    fun wordsWaitUntilHalfTheLettersAreKnown() {
        val early = (0L until 30L).flatMap { DailyPath.plan(it, shares(Track.ENGLISH to 0.3f, Track.SWAR to 0.3f)) }
        assertTrue(early.none { it.mode == GameMode.WORDS })
        val later = (0L until 30L).flatMap { DailyPath.plan(it, shares(Track.ENGLISH to 0.6f, Track.SWAR to 0.6f)) }
        assertTrue(later.any { it.mode == GameMode.WORDS && it.track == Track.ENGLISH })
    }

    @Test
    fun stepsSurviveSaving() {
        val step = PathStep(Track.GINTI, GameMode.COUNT)
        assertEquals(step, PathStep.decode(step.encode()))
        assertNull(PathStep.decode("NOPE:LEARN"))
        assertNull(PathStep.decode("garbage"))
    }
}
