package com.kirtigames.abcd

import java.util.Locale
import kotlin.random.Random

/** Everything a track says out loud or shows as text, in its own language. */
interface Lang {
    val locale: Locale

    /** How the voice should say a letter's name. */
    fun name(letter: Letter): String
    fun praise(random: Random): String

    // Spoken
    fun learn(letter: Letter): String
    fun find(target: Letter): String
    fun found(target: Letter, random: Random): String = "${praise(random)} ${name(target)}!"
    fun notThis(tapped: Letter, target: Letter): String
    fun balloonsStart(first: Letter): String
    fun balloonsDone(): String
    fun balloonsAgain(first: Letter): String
    fun matchAsk(letter: Letter): String
    fun matchRight(letter: Letter, random: Random): String
    fun matchWrong(letter: Letter): String
    fun traceAsk(letter: Letter): String
    fun traceDone(letter: Letter, random: Random): String
    fun traceAgain(): String

    // On screen
    fun learnTitle(number: Int, total: Int): String
    val findTitle: String
    fun popTitle(next: Letter): String
    val doneTitle: String
    val matchTitle: String
    fun traceTitle(number: Int, total: Int): String
    fun modeLabel(mode: GameMode): String
}

object English : Lang {
    override val locale: Locale = Locale.US

    // A lone "A" is often read as the word "a" ("uh"), so it is spelled the way it sounds.
    override fun name(letter: Letter) = if (letter.symbol == "A") "ay" else letter.symbol

    override fun praise(random: Random) =
        listOf("Great job!", "Well done!", "Super!", "Awesome!", "You got it!", "Yay!").random(random)

    override fun learn(letter: Letter) = "${name(letter)}. ${name(letter)} is for ${letter.word}."
    override fun find(target: Letter) = "Find the letter ${name(target)}!"
    override fun notThis(tapped: Letter, target: Letter) = "That is ${name(tapped)}. Find ${name(target)}!"
    override fun balloonsStart(first: Letter) = "Pop the balloons from ay to Z! Find the letter ${name(first)}."
    override fun balloonsDone() = "Hooray! You popped all the letters from ay to Z!"
    override fun balloonsAgain(first: Letter) = "Let's go again! Find the letter ${name(first)}."
    override fun matchAsk(letter: Letter) = "${letter.word}. Which letter does ${letter.word} start with?"
    override fun matchRight(letter: Letter, random: Random) =
        "${praise(random)} ${letter.word} starts with ${name(letter)}!"
    override fun matchWrong(letter: Letter) = "Try again! ${letter.word}."
    override fun traceAsk(letter: Letter) = "Trace the letter ${name(letter)}!"
    override fun traceDone(letter: Letter, random: Random) = "${praise(random)} You wrote ${name(letter)}!"
    override fun traceAgain() = "Try again. Draw over the letter."

    override fun learnTitle(number: Int, total: Int) = "Letter $number of $total"
    override val findTitle = "Find the letter"
    override fun popTitle(next: Letter) = "Pop the letter ${next.symbol}"
    override val doneTitle = "Hooray!"
    override val matchTitle = "Which letter?"
    override fun traceTitle(number: Int, total: Int) = "Trace $number of $total"
    override fun modeLabel(mode: GameMode) = when (mode) {
        GameMode.LEARN -> "Learn"
        GameMode.TRACE -> "Trace"
        GameMode.FIND -> "Find it"
        GameMode.BALLOONS -> "Balloons"
        GameMode.MATCH -> "Pictures"
    }
}

object Hindi : Lang {
    override val locale: Locale = Locale.forLanguageTag("hi-IN")

    override fun name(letter: Letter) = letter.symbol

    override fun praise(random: Random) =
        listOf("शाबाश!", "बहुत बढ़िया!", "वाह!", "बहुत अच्छे!").random(random)

    override fun learn(letter: Letter) =
        if (letter.word != null) "${letter.symbol}. ${letter.symbol} से ${letter.word}." else "${letter.symbol}."
    override fun find(target: Letter) = "${target.symbol} ढूंढो!"
    override fun notThis(tapped: Letter, target: Letter) = "यह ${tapped.symbol} है। ${target.symbol} ढूंढो!"
    override fun balloonsStart(first: Letter) = "गुब्बारे फोड़ो! ${first.symbol} से शुरू करो।"
    override fun balloonsDone() = "शाबाश! तुमने सारे अक्षर फोड़ दिए!"
    override fun balloonsAgain(first: Letter) = "फिर से खेलो! ${first.symbol} ढूंढो।"
    override fun matchAsk(letter: Letter) = "${letter.word}. ${letter.word} किस अक्षर से शुरू होता है?"
    override fun matchRight(letter: Letter, random: Random) =
        "${praise(random)} ${letter.word}, ${letter.symbol} से शुरू होता है!"
    override fun matchWrong(letter: Letter) = "फिर से कोशिश करो! ${letter.word}."
    override fun traceAsk(letter: Letter) = "${letter.symbol} बनाओ!"
    override fun traceDone(letter: Letter, random: Random) = "${praise(random)} तुमने ${letter.symbol} लिखा!"
    override fun traceAgain() = "फिर से कोशिश करो। अक्षर के ऊपर बनाओ।"

    override fun learnTitle(number: Int, total: Int) = "अक्षर $number / $total"
    override val findTitle = "अक्षर ढूंढो"
    override fun popTitle(next: Letter) = "${next.symbol} फोड़ो"
    override val doneTitle = "शाबाश!"
    override val matchTitle = "कौन सा अक्षर?"
    override fun traceTitle(number: Int, total: Int) = "लिखो $number / $total"
    override fun modeLabel(mode: GameMode) = when (mode) {
        GameMode.LEARN -> "सीखो"
        GameMode.TRACE -> "लिखो"
        GameMode.FIND -> "ढूंढो"
        GameMode.BALLOONS -> "गुब्बारे"
        GameMode.MATCH -> "चित्र"
    }
}
