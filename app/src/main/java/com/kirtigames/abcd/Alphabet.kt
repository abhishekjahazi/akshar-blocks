package com.kirtigames.abcd

import kotlin.random.Random

/** One letter: its symbol, and (when it has one) a word that starts with it and a picture of that word. */
data class Letter(val symbol: String, val word: String? = null, val emoji: String? = null) {

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

/** A set of letters to learn, with its own voice and on-screen language. */
enum class Track(
    val label: String,
    val subtitle: String,
    val color: Int,
    val lang: Lang,
    val letters: List<Letter>,
    /** English shows "Aa" (capital and small); Hindi has no letter case. */
    val showsCase: Boolean,
) {
    ENGLISH("English", "A B C D", Palette.OCEAN, English, Letters.english, showsCase = true),
    SWAR("हिंदी स्वर", "Hindi vowels", Palette.TOMATO, Hindi, Letters.swar, showsCase = false),
    VYANJAN("हिंदी व्यंजन", "Hindi consonants", Palette.GRASS, Hindi, Letters.vyanjan, showsCase = false);

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

object Letters {

    val english = listOf(
        Letter("A", "Apple", "🍎"),
        Letter("B", "Ball", "⚽"),
        Letter("C", "Cat", "🐱"),
        Letter("D", "Dog", "🐶"),
        Letter("E", "Elephant", "🐘"),
        Letter("F", "Fish", "🐟"),
        Letter("G", "Grapes", "🍇"),
        Letter("H", "House", "🏠"),
        Letter("I", "Ice cream", "🍦"),
        Letter("J", "Juice", "🧃"),
        Letter("K", "Kite", "🪁"),
        Letter("L", "Lion", "🦁"),
        Letter("M", "Monkey", "🐒"),
        Letter("N", "Nose", "👃"),
        Letter("O", "Orange", "🍊"),
        Letter("P", "Penguin", "🐧"),
        Letter("Q", "Queen", "👸"),
        Letter("R", "Rabbit", "🐰"),
        Letter("S", "Sun", "☀️"),
        Letter("T", "Tree", "🌳"),
        Letter("U", "Umbrella", "☂️"),
        Letter("V", "Violin", "🎻"),
        Letter("W", "Watch", "⌚"),
        Letter("X", "Xmas tree", "🎄"),
        Letter("Y", "Yo-yo", "🪀"),
        Letter("Z", "Zebra", "🦓"),
    )

    /** स्वर: the 13 Hindi vowels. अः has no common word, so it is shown on its own. */
    val swar = listOf(
        Letter("अ", "अनानास", "🍍"),
        Letter("आ", "आम", "🥭"),
        Letter("इ", "इमारत", "🏢"),
        Letter("ई", "ईद", "🌙"),
        Letter("उ", "उल्लू", "🦉"),
        Letter("ऊ", "ऊन", "🧶"),
        Letter("ऋ", "ऋषि", "🧘"),
        Letter("ए", "एक", "1️⃣"),
        Letter("ऐ", "ऐनक", "👓"),
        Letter("ओ", "ओस", "💧"),
        Letter("औ", "औरत", "👩"),
        Letter("अं", "अंगूर", "🍇"),
        Letter("अः"),
    )

    /** व्यंजन: the 36 Hindi consonants. ङ, ञ and ण begin no common words, so they have none. */
    val vyanjan = listOf(
        Letter("क", "कबूतर", "🕊️"),
        Letter("ख", "खरगोश", "🐰"),
        Letter("ग", "गाय", "🐄"),
        Letter("घ", "घड़ी", "⌚"),
        Letter("ङ"),
        Letter("च", "चम्मच", "🥄"),
        Letter("छ", "छतरी", "☂️"),
        Letter("ज", "जहाज", "🚢"),
        Letter("झ", "झंडा", "🚩"),
        Letter("ञ"),
        Letter("ट", "टमाटर", "🍅"),
        Letter("ठ", "ठेला", "🛒"),
        Letter("ड", "डमरू", "🥁"),
        Letter("ढ", "ढोल", "🪘"),
        Letter("ण"),
        Letter("त", "तरबूज", "🍉"),
        Letter("थ", "थाली", "🍽️"),
        Letter("द", "दरवाज़ा", "🚪"),
        Letter("ध", "धनुष", "🏹"),
        Letter("न", "नल", "🚰"),
        Letter("प", "पतंग", "🪁"),
        Letter("फ", "फूल", "🌸"),
        Letter("ब", "बतख", "🦆"),
        Letter("भ", "भालू", "🐻"),
        Letter("म", "मछली", "🐟"),
        Letter("य", "यज्ञ", "🔥"),
        Letter("र", "रेलगाड़ी", "🚂"),
        Letter("ल", "लहसुन", "🧄"),
        Letter("व", "वकील", "👨‍⚖️"),
        Letter("श", "शेर", "🦁"),
        Letter("ष", "षट्कोण"),
        Letter("स", "सेब", "🍎"),
        Letter("ह", "हाथी", "🐘"),
        Letter("क्ष", "क्षत्रिय"),
        Letter("त्र", "त्रिशूल", "🔱"),
        Letter("ज्ञ", "ज्ञानी"),
    )
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
