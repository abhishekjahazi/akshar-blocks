package com.aksharblocks.app

import android.content.res.AssetManager
import java.io.File
import kotlin.random.Random

/** One side of a pair of opposites: a picture and its name in English and Hindi. */
data class Side(val emoji: String, val english: String, val hindi: String)

/** Two opposites, like big (🐘) and small (🐭). */
data class OppositePair(val a: Side, val b: Side)

/** One question: [shown] is on screen; the child picks [answer], its opposite, from [choices]. */
data class OppositeRound(val shown: Side, val answer: Side, val choices: List<Side>)

/** The opposites game's pairs (assets/opposites.tsv) and its questions. */
object Opposites {

    var pairs: List<OppositePair> = emptyList()
        private set

    fun load(assets: AssetManager) {
        pairs = assets.open(FILE).bufferedReader().use { parse(it.readText()) }
    }

    fun loadFrom(folder: File) {
        pairs = parse(File(folder, FILE).readText())
    }

    /** One pair per line: picture, English, Hindi, then the same for its opposite. */
    fun parse(text: String): List<OppositePair> =
        text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val c = line.split('\t').map { it.trim() }
                require(c.size >= 6) { "An opposites line needs 6 columns: $line" }
                OppositePair(Side(c[0], c[1], c[2]), Side(c[3], c[4], c[5]))
            }
            .toList()

    /**
     * A question from a pair other than [previous]'s, shown either way round (big → small, or
     * small → big). The wrong choices come from two other pairs, so only one choice is right.
     */
    fun round(random: Random, previous: OppositePair? = null): OppositeRound {
        val pair = pairs.filter { it != previous }.random(random)
        val (shown, answer) = if (random.nextBoolean()) pair.a to pair.b else pair.b to pair.a
        val others = pairs.filter { it != pair }.shuffled(random).take(CHOICES - 1)
            .map { if (random.nextBoolean()) it.a else it.b }
        return OppositeRound(shown, answer, (others + answer).shuffled(random))
    }

    fun pairOf(side: Side): OppositePair = pairs.first { it.a == side || it.b == side }

    const val CHOICES = 3
    private const val FILE = "opposites.tsv"
}

/** What the opposites game says: English, then Hindi. */
object OppositeWords {
    const val TITLE = "Opposites"
    const val HINDI_TITLE = "उल्टे शब्द"

    fun ask(shown: Side) = "${shown.english}!|@hi ${shown.hindi}!|What is the opposite?|@hi इसका उल्टा क्या है?"
    fun right(shown: Side, answer: Side) =
        "${shown.english} and ${answer.english.lowercase()}!|@hi ${shown.hindi} और ${answer.hindi}!"
    fun wrong(tapped: Side) = "That one is|${tapped.english.lowercase()}.|@hi ये ${tapped.hindi} है।|Try again!"

    /** Every line the game can say, for the voice recordings. */
    fun all(random: Random): List<String> = Opposites.pairs.flatMap { p ->
        listOf(p.a to p.b, p.b to p.a).flatMap { (shown, answer) ->
            listOf(ask(shown), "${English.praise(random)}|${right(shown, answer)}", wrong(shown))
        }
    }
}
