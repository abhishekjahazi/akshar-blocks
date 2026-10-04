package com.aksharblocks.app

import kotlin.random.Random

/** The maths games, with their picture and names (English, Hindi). */
enum class MathGame(val emoji: String, val label: String, val hindi: String, val color: Int) {
    ADD("➕", "Add", "जोड़ो", Palette.OCEAN),
    TAKE_AWAY("➖", "Take away", "घटाओ", Palette.TOMATO),
    MORE("⚖️", "More or less", "ज़्यादा या कम", Palette.GRASS),
    PATTERN("🔴", "Patterns", "आगे क्या?", Palette.GRAPE),
}

/** One question: "3 + 2" (add), "5 − 2" (take away) or "4 or 7, which is more?" (compare). */
data class Sum(val a: Int, val b: Int) {
    val total get() = a + b
    val left get() = a - b
}

/** A repeating row of pictures to continue: [shown] then "?", whose answer is [answer]. */
data class Pattern(val shown: List<String>, val answer: String, val choices: List<String>)

/** Makes the questions. Numbers stay within 10, the size of one ten frame. */
object Maths {
    const val MAX = 10

    /** Both parts 1–5, so each group fits in one row of five. */
    fun add(random: Random): Sum = Sum(random.nextInt(1, 6), random.nextInt(1, 6))

    /** 2–9 things, 1 or more of them go away, at least one is left. */
    fun takeAway(random: Random): Sum {
        val a = random.nextInt(2, MAX)
        return Sum(a, random.nextInt(1, a))
    }

    /** Two different amounts, 1–9 each. */
    fun compare(random: Random): Sum {
        val a = random.nextInt(1, MAX)
        var b = random.nextInt(1, MAX - 1)
        if (b >= a) b++
        return Sum(a, b)
    }

    /** [answer] with two close numbers, all different and within 0–10, smallest first. */
    fun choices(answer: Int, random: Random): List<Int> {
        val near = (maxOf(0, answer - 2)..minOf(MAX, answer + 2)).filter { it != answer }.shuffled(random).take(2)
        return (near + answer).sorted()
    }

    /** Repeating units: AB, AAB, ABB, ABC. */
    private val UNITS = listOf(listOf(0, 1), listOf(0, 0, 1), listOf(0, 1, 1), listOf(0, 1, 2))

    /** Pictures for patterns: plain, bright and easy to tell apart. */
    val PATTERN_PICTURES = listOf("🔴", "🔵", "🟡", "🟢", "⭐", "🍎", "🐟", "🌸")

    fun pattern(random: Random): Pattern {
        val unit = UNITS.random(random)
        val pictures = PATTERN_PICTURES.shuffled(random).take(4)
        // The unit shows twice, sometimes with the start of a third; for AB a little more.
        val length = unit.size * 2 + random.nextInt(0, if (unit.size == 2) 2 else 1) + if (unit.size == 2) 1 else 0
        val sequence = List(length + 1) { pictures[unit[it % unit.size]] }
        val answer = sequence.last()
        val others = pictures.filter { it != answer }.shuffled(random).take(2)
        return Pattern(sequence.dropLast(1), answer, (others + answer).shuffled(random))
    }
}

/** What the maths games say: English, then the same in Hindi. */
object MathWords {
    private val HINDI_NUMBERS = listOf("शून्य", "एक", "दो", "तीन", "चार", "पाँच", "छह", "सात", "आठ", "नौ", "दस")

    fun hindi(n: Int) = HINDI_NUMBERS[n]

    const val MENU_ASK = "Let's play with numbers!|@hi चलो, गिनती से खेलें!"

    fun addAsk(thing: CountThing) = "How many ${thing.many} in all?|@hi कुल ${thing.hindiHowMany} ${thing.hindi}?"
    fun addRight(sum: Sum) = "${sum.a} and ${sum.b} make ${sum.total}!|@hi ${hindi(sum.a)} और ${hindi(sum.b)}, ${hindi(sum.total)}!"
    const val ADD_WRONG = "Count them all!|@hi सबको गिनो!"

    fun takeAwayAsk(thing: CountThing) =
        "How many ${thing.many} are left?|@hi " + if (thing.hindiHowMany == "कितनी") "कितनी बचीं?" else "कितने बचे?"
    fun takeAwayRight(sum: Sum) = "${sum.a} take away ${sum.b} is ${sum.left}!|@hi ${hindi(sum.a)} में से ${hindi(sum.b)} गए, ${hindi(sum.left)} बचे!"
    const val TAKE_AWAY_WRONG = "Count the ones that are left!|@hi जो बचे हैं, उन्हें गिनो!"

    const val MORE_ASK = "Which side has more?|@hi किधर ज़्यादा हैं?"
    const val FEWER_ASK = "Which side has fewer?|@hi किधर कम हैं?"
    fun compareRight(sum: Sum, more: Boolean): String {
        val (big, small) = if (sum.a > sum.b) sum.a to sum.b else sum.b to sum.a
        return if (more) "$big is more than $small!|@hi ${hindi(big)}, ${hindi(small)} से ज़्यादा!"
        else "$small is less than $big!|@hi ${hindi(small)}, ${hindi(big)} से कम!"
    }
    const val LOOK_AGAIN = "Look again!|@hi फिर से देखो!"

    const val PATTERN_ASK = "What comes next?|@hi आगे क्या आएगा?"
    const val PATTERN_RIGHT = "You found the pattern!|@hi सही पहचाना!"

    /** Every line the maths games can say, for the voice recordings. */
    fun all(): List<String> {
        val lines = mutableListOf(MENU_ASK, ADD_WRONG, TAKE_AWAY_WRONG, MORE_ASK, FEWER_ASK, LOOK_AGAIN, PATTERN_ASK, PATTERN_RIGHT)
        Counting.things.forEach { lines += addAsk(it); lines += takeAwayAsk(it) }
        for (a in 1..5) for (b in 1..5) lines += addRight(Sum(a, b))
        for (a in 2 until Maths.MAX) for (b in 1 until a) lines += takeAwayRight(Sum(a, b))
        for (a in 1 until Maths.MAX) for (b in 1 until Maths.MAX) if (a != b) {
            lines += compareRight(Sum(a, b), more = true)
            lines += compareRight(Sum(a, b), more = false)
        }
        return lines
    }
}
