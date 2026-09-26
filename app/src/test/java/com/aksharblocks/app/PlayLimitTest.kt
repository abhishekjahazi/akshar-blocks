package com.aksharblocks.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayLimitTest {

    @Test
    fun noLimitNeverEnds() {
        assertFalse(PlayLimit.isOver(limitMinutes = 0, secondsToday = 10 * 3600, extraMinutes = 0))
    }

    @Test
    fun endsWhenTheLimitIsReached() {
        assertFalse(PlayLimit.isOver(limitMinutes = 15, secondsToday = 15 * 60 - 1, extraMinutes = 0))
        assertTrue(PlayLimit.isOver(limitMinutes = 15, secondsToday = 15 * 60, extraMinutes = 0))
    }

    @Test
    fun extraMinutesFromAParentExtendIt() {
        assertFalse(PlayLimit.isOver(limitMinutes = 15, secondsToday = 20 * 60, extraMinutes = 10))
        assertTrue(PlayLimit.isOver(limitMinutes = 15, secondsToday = 25 * 60, extraMinutes = 10))
    }

    @Test
    fun choicesStartWithOff() {
        assertTrue(PlayLimit.CHOICES.first() == 0)
        assertTrue(PlayLimit.CHOICES.drop(1).all { it > 0 })
    }
}
