package com.aksharblocks.app

import kotlin.random.Random

/** One letter: its symbol, and (when it has one) a word that starts with it and a picture of that word. */
data class Letter(
    val symbol: String,
    val word: String? = null,
    val emoji: String? = null,
    /** Letters that belong together, like the syllables of one consonant in the बारहखड़ी. */
    val group: String? = null,
) {

    val hasPicture get() = word != null && emoji != null

    /**
     * How many characters at the start of [word] belong to the letter, including the vowel
     * signs and marks attached to it. "गाय" gives 2 ("गा"), so the letter is never cut away
     * from its vowel sign when the two parts are drawn in different colors.
     */
    val headLength: Int
        get() {
            val w = word ?: return 0
            var n = symbol.length
            while (n < w.length && isCombiningMark(w[n])) n++
            return n
        }
}

private fun isCombiningMark(c: Char): Boolean {
    val type = Character.getType(c)
    return type == Character.NON_SPACING_MARK.toInt() ||
        type == Character.COMBINING_SPACING_MARK.toInt() ||
        type == Character.ENCLOSING_MARK.toInt()
}

internal val ALPHABET_GAMES = listOf(GameMode.LEARN, GameMode.TRACE, GameMode.FIND, GameMode.BALLOONS, GameMode.MATCH)
internal val NUMBER_GAMES = listOf(GameMode.LEARN, GameMode.COUNT, GameMode.FIND, GameMode.TRACE)

/** A set of letters to learn, with its own voice and on-screen language. */
enum class Track(
    val label: String,
    val subtitle: String,
    val color: Int,
    val lang: Lang,
    /** The content file in assets/tracks/, or null when the letters are built from another track. */
    val file: String?,
    /** English shows "Aa" (capital and small); Hindi has no letter case. */
    val showsCase: Boolean,
    /** The games offered for this track, in menu order. */
    val modes: List<GameMode> = ALPHABET_GAMES,
) {
    ENGLISH("English", "A B C D", Palette.OCEAN, English, "english.tsv", showsCase = true),
    SWAR("हिंदी स्वर", "Hindi vowels", Palette.TOMATO, Hindi, "swar.tsv", showsCase = false),
    VYANJAN("हिंदी व्यंजन", "Hindi consonants", Palette.GRASS, Hindi, "vyanjan.tsv", showsCase = false),
    BARAKHADI(
        "बारहखड़ी", "Hindi syllables", Palette.GRAPE, Hindi, file = null, showsCase = false,
        modes = listOf(GameMode.LEARN, GameMode.BUILD, GameMode.FIND, GameMode.TRACE),
    ),
    NUMBERS("Numbers", "1 to 100", Palette.ORANGE, EnglishNumbers, "numbers.tsv", showsCase = false, modes = NUMBER_GAMES),
    GINTI("हिंदी गिनती", "Hindi numbers", Palette.PINK, HindiNumbers, "ginti.tsv", showsCase = false, modes = NUMBER_GAMES);

    val letters: List<Letter> get() = Content.letters(this)

    val isNumbers get() = this == NUMBERS || this == GINTI

    /**
     * Letters to offer as wrong answers next to [target]: the same group when it has one,
     * and nearby numbers for number tracks (27 with 24, 29, 31, not with 3 or 90).
     */
    fun choicePool(target: Letter): List<Letter> = when {
        target.group != null -> letters.filter { it.group == target.group }
        isNumbers -> letters.indexOf(target).let { i -> letters.subList(maxOf(0, i - 6), minOf(letters.size, i + 7)) }
        else -> letters
    }

    /** Letters that have a word and picture, for the picture game. */
    val pictureLetters: List<Letter> get() = letters.filter { it.hasPicture }

    fun display(letter: Letter): String =
        if (showsCase) letter.symbol + letter.symbol.lowercase() else letter.symbol

    fun colorFor(letter: Letter): Int = colorFor(letters.indexOf(letter))

    fun colorFor(index: Int): Int = LETTER_COLORS[index.coerceAtLeast(0) % LETTER_COLORS.size]

    private companion object {
        /** Bright colors that read well as big letters on white and hold white letters on top. */
        val LETTER_COLORS = intArrayOf(
            Palette.TOMATO, Palette.OCEAN, Palette.GRASS, Palette.GRAPE,
            Palette.ORANGE, Palette.PINK, Palette.TEAL,
        )
    }
}


/** Random choices for the games. */
object Pick {

    /** [target] plus `count - 1` other distinct items from [pool], in random order. */
    fun <T> choices(target: T, pool: List<T>, count: Int, random: Random): List<T> {
        val others = pool.filter { it != target }.shuffled(random).take(count - 1)
        return (others + target).shuffled(random)
    }

    fun <T> anyExcept(pool: List<T>, except: T?, random: Random): T =
        pool.filter { it != except }.random(random)
}
