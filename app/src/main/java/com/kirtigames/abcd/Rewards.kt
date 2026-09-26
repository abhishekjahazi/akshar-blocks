package com.kirtigames.abcd

import kotlin.random.Random

data class Sticker(val emoji: String, val name: String)

/** The sticker album: a new sticker for every few stars. Unlocks follow from the star count alone. */
object Stickers {

    const val STARS_EACH = 5

    /** In unlock order. All are old emoji, so they show on Android 7 too. */
    val all = listOf(
        Sticker("🐶", "puppy"), Sticker("🐱", "kitten"), Sticker("🐰", "bunny"), Sticker("🐼", "panda"),
        Sticker("🐸", "frog"), Sticker("🐵", "monkey"), Sticker("🐤", "chick"), Sticker("🐢", "turtle"),
        Sticker("🐝", "bee"), Sticker("🐞", "ladybug"), Sticker("🐧", "penguin"), Sticker("🐯", "tiger"),
        Sticker("🐻", "bear"), Sticker("🐨", "koala"), Sticker("🐮", "cow"), Sticker("🐷", "piggy"),
        Sticker("🐔", "hen"), Sticker("🐭", "mouse"), Sticker("🐙", "octopus"), Sticker("🦀", "crab"),
        Sticker("🐬", "dolphin"), Sticker("🐳", "whale"), Sticker("🐘", "elephant"), Sticker("🐪", "camel"),
        Sticker("🐴", "horse"), Sticker("🦁", "lion"), Sticker("🦄", "unicorn"), Sticker("🌈", "rainbow"),
        Sticker("🚀", "rocket"), Sticker("🎂", "cake"),
    )

    fun unlocked(stars: Int): Int = (stars / STARS_EACH).coerceAtMost(all.size)

    /** Stars still needed for the next sticker, or 0 when the album is full. */
    fun starsToNext(stars: Int): Int =
        if (unlocked(stars) >= all.size) 0 else STARS_EACH - stars % STARS_EACH
}

/** Days played in a row. Days are counted in the phone's own time zone. */
object Streak {

    /** The streak after playing on [today], given the last day played and the streak then. */
    fun next(lastDay: Long, streak: Int, today: Long): Int = when (today) {
        lastDay -> streak.coerceAtLeast(1)
        lastDay + 1 -> streak + 1
        else -> 1
    }

    /** Still alive today: played today or yesterday. */
    fun current(lastDay: Long, streak: Int, today: Long): Int =
        if (today - lastDay <= 1) streak else 0
}

/** One child's results in one track, by letter symbol. */
class LetterStats(
    val ok: Map<String, Int>,
    val miss: Map<String, Int>,
    /** (asked for, picked instead) → times. */
    val mix: Map<Pair<String, String>, Int>,
) {
    companion object {
        val EMPTY = LetterStats(emptyMap(), emptyMap(), emptyMap())
    }
}

/**
 * Smart practice: letters the child gets wrong come up more often, new letters get a
 * gentle push, and well-known letters still appear now and then.
 */
object Coach {

    fun weight(symbol: String, stats: LetterStats): Float {
        val ok = stats.ok[symbol] ?: 0
        val miss = stats.miss[symbol] ?: 0
        if (ok + miss == 0) return NEW_LETTER
        return (1f + 2f * miss - 0.4f * ok).coerceIn(MIN_WEIGHT, MAX_WEIGHT)
    }

    /** A random letter from [candidates], weighted toward the ones that need practice. */
    fun pickTarget(candidates: List<Letter>, stats: LetterStats, random: Random, except: Letter? = null): Letter {
        val pool = candidates.filter { it != except }.ifEmpty { candidates }
        val weights = pool.map { weight(it.symbol, stats) }
        var roll = random.nextFloat() * weights.sum()
        for (i in pool.indices) {
            roll -= weights[i]
            if (roll <= 0f) return pool[i]
        }
        return pool.last()
    }

    /** Letters the child has confused with [target] (either way round), most often first. */
    fun mixPartners(target: Letter, candidates: List<Letter>, stats: LetterStats): List<Letter> {
        val counts = HashMap<String, Int>()
        for ((pair, count) in stats.mix) {
            val other = when (target.symbol) {
                pair.first -> pair.second
                pair.second -> pair.first
                else -> continue
            }
            counts[other] = (counts[other] ?: 0) + count
        }
        return candidates.filter { it != target && (counts[it.symbol] ?: 0) > 0 }
            .sortedByDescending { counts[it.symbol] }
    }

    /** [count] choices including [target], with up to two of its usual mix-ups among the wrong ones. */
    fun choices(target: Letter, pool: List<Letter>, stats: LetterStats, count: Int, random: Random): List<Letter> {
        val tricky = mixPartners(target, pool, stats).take(minOf(2, count - 1))
        val rest = pool.filter { it != target && it !in tricky }.shuffled(random).take(count - 1 - tricky.size)
        return (listOf(target) + tricky + rest).shuffled(random)
    }

    private const val NEW_LETTER = 1.5f
    private const val MIN_WEIGHT = 0.3f
    private const val MAX_WEIGHT = 6f
}
