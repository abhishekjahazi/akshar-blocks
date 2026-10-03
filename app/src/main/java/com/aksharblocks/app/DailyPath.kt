package com.aksharblocks.app

/** One game in today's path, finished after [goal] stars (or, in Learn, new letters seen). */
data class PathStep(val track: Track, val mode: GameMode) {
    val goal: Int
        get() = when (mode) {
            GameMode.LEARN -> 5
            GameMode.MEMORY -> 1
            GameMode.WORDS -> 2
            else -> 3
        }

    fun encode() = "${track.name}:${mode.name}"

    companion object {
        fun decode(text: String): PathStep? {
            val (track, mode) = text.split(':').takeIf { it.size == 2 } ?: return null
            return PathStep(
                Track.entries.firstOrNull { it.name == track } ?: return null,
                GameMode.entries.firstOrNull { it.name == mode } ?: return null,
            )
        }
    }
}

/**
 * Today's games: a short guided session picked from the child's progress, so a child who
 * just taps "play" still moves forward. Two sections a day, each with a learning game
 * (Learn while the letters are new, then Trace) and a practice game that changes daily.
 */
object DailyPath {

    const val STEPS = 4

    /** A track counts as done for the path once this share of its letters is known. */
    const val MASTERED = 0.8f

    /** A following track opens once this share of the one before it is known. */
    const val OPENS_NEXT = 0.5f

    /** Learn is used until this share is known; after that, Trace. */
    const val LEARN_UNTIL = 0.3f

    /** Letters, Hindi and numbers; each group's tracks in teaching order. */
    private val GROUPS = listOf(
        listOf(Track.ENGLISH, Track.LOWER),
        listOf(Track.SWAR, Track.VYANJAN, Track.BARAKHADI),
        listOf(Track.NUMBERS, Track.GINTI),
    )

    /** Which two groups play on a day, cycling over three days. */
    private val DAY_GROUPS = listOf(0 to 1, 1 to 2, 0 to 2)

    /** Practice games to rotate through, where the track has them. */
    private val PRACTICE = listOf(
        GameMode.FIND, GameMode.SOUNDS, GameMode.BALLOONS, GameMode.MATCH, GameMode.MEMORY,
        GameMode.COUNT, GameMode.BUILD, GameMode.WORDS,
    )

    /** Today's steps. [share] is the share of a track's letters the child knows (0 to 1). */
    fun plan(day: Long, share: (Track) -> Float): List<PathStep> {
        val (first, second) = DAY_GROUPS[Math.floorMod(day, DAY_GROUPS.size.toLong()).toInt()]
        return listOf(first, second).flatMap { group ->
            val track = pick(GROUPS[group], share)
            listOf(PathStep(track, learnMode(track, share(track))), PathStep(track, practiceMode(track, share(track), day)))
        }
    }

    /** The first open track in [group] that isn't mastered yet; when all are, the weakest. */
    fun pick(group: List<Track>, share: (Track) -> Float): Track {
        val open = group.filterIndexed { i, _ -> i == 0 || share(group[i - 1]) >= OPENS_NEXT }
        return open.firstOrNull { share(it) < MASTERED } ?: open.minBy(share)
    }

    private fun learnMode(track: Track, share: Float): GameMode = when {
        // The बारहखड़ी chart has no stars to count; building syllables teaches the same thing.
        track == Track.BARAKHADI -> GameMode.BUILD
        share < LEARN_UNTIL -> GameMode.LEARN
        else -> GameMode.TRACE
    }

    private fun practiceMode(track: Track, share: Float, day: Long): GameMode {
        val choices = PRACTICE.filter { mode ->
            mode in track.modes && mode != learnMode(track, share) &&
                // Words need letters the child already knows.
                (mode != GameMode.WORDS || share >= OPENS_NEXT)
        }
        return choices[Math.floorMod(day, choices.size.toLong()).toInt()]
    }
}
