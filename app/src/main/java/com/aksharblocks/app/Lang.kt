package com.aksharblocks.app

import java.util.Locale
import kotlin.random.Random

/**
 * Everything a track says out loud or shows as text, in its own language.
 *
 * Spoken sentences are split into reusable parts with `|` (see [Voice]): "Find the letter|B!"
 * lets one recording of "Find the letter" serve every letter. The phone voice reads the
 * parts joined with spaces; the `|` is never shown or spoken.
 */
interface Lang {
    val locale: Locale

    /** How the voice should say a letter's name. */
    fun name(letter: Letter): String

    /** Praise lines; one is picked at random. */
    val praises: List<String>
    fun praise(random: Random): String = praises.random(random)

    // Spoken
    fun learn(letter: Letter): String
    fun find(target: Letter): String
    fun found(target: Letter, random: Random): String = "${praise(random)}|${name(target)}!"
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
    fun countAsk(thing: CountThing): String
    fun countRight(number: Letter, thing: CountThing, random: Random): String
    fun countWrong(thing: CountThing): String
    val memoryAsk: String
    fun memoryDone(): String
    fun wordAsk(word: Word): String
    fun wordDone(word: Word, random: Random): String = "${praise(random)}|${word.text}!"

    // Animals' sounds game ("Who says moo?"); only picture tracks with sounds use these.
    fun soundAsk(target: Letter): String = find(target)
    fun soundRight(target: Letter, random: Random): String = found(target, random)
    fun soundWrong(tapped: Letter, target: Letter): String = notThis(tapped, target)

    // On screen
    fun learnTitle(number: Int, total: Int): String
    val findTitle: String
    fun popTitle(next: Letter): String
    val doneTitle: String
    val matchTitle: String
    fun traceTitle(number: Int, total: Int): String
    val countTitle: String
    val memoryTitle: String
    val wordsTitle: String
    fun modeLabel(mode: GameMode): String
}

open class EnglishLang : Lang {
    // Indian English: the built-in voice is Google's en-IN "enc" voice, and live speech should match it.
    override val locale: Locale = Locale.forLanguageTag("en-IN")

    // A lone "A" is often read as the word "a" ("uh"), so it is spelled the way it sounds.
    override fun name(letter: Letter) = if (letter.symbol.equals("A", ignoreCase = true)) "ay" else letter.symbol

    override val praises = listOf("Great job!", "Well done!", "Super!", "Awesome!", "You got it!", "Yay!")

    override fun learn(letter: Letter) = "${name(letter)}.|${name(letter)} is for ${letter.word}."
    override fun find(target: Letter) = "Find the letter|${name(target)}!"
    override fun notThis(tapped: Letter, target: Letter) = "That is|${name(tapped)}.|Find|${name(target)}!"
    override fun balloonsStart(first: Letter) = "Pop the balloons from ay to Z!|Find the letter|${name(first)}."
    override fun balloonsDone() = "Hooray! You popped all the letters from ay to Z!"
    override fun balloonsAgain(first: Letter) = "Let's go again!|Find the letter|${name(first)}."
    override fun matchAsk(letter: Letter) = "${letter.word}.|Which letter does ${letter.word} start with?"
    override fun matchRight(letter: Letter, random: Random) =
        "${praise(random)}|${letter.word} starts with ${name(letter)}!"
    override fun matchWrong(letter: Letter) = "Try again!|${letter.word}."
    override fun traceAsk(letter: Letter) = "Trace the letter|${name(letter)}!"
    override fun traceDone(letter: Letter, random: Random) = "${praise(random)}|You wrote|${name(letter)}!"
    override fun traceAgain() = "Try again. Draw over the letter."
    override fun countAsk(thing: CountThing) = "How many ${thing.many}?"
    override fun countRight(number: Letter, thing: CountThing, random: Random) =
        "${praise(random)}|${number.word}|${if (number.symbol == "1") thing.one else thing.many}!"
    override fun countWrong(thing: CountThing) = "Let's count again.|How many ${thing.many}?"
    override val memoryAsk = "Find the pairs!"
    override fun memoryDone() = "You found all the pairs!"
    override fun wordAsk(word: Word) = "Make the word|${word.text}!"

    override fun learnTitle(number: Int, total: Int) = "Letter $number of $total"
    override val findTitle = "Find the letter"
    override fun popTitle(next: Letter) = "Pop the letter ${next.symbol}"
    override val doneTitle = "Hooray!"
    override val matchTitle = "Which letter?"
    override fun traceTitle(number: Int, total: Int) = "Trace $number of $total"
    override val countTitle = "How many?"
    override val memoryTitle = "Find the pairs"
    override val wordsTitle = "Make the word"
    override fun modeLabel(mode: GameMode) = when (mode) {
        GameMode.LEARN -> "Learn"
        GameMode.TRACE -> "Trace"
        GameMode.BUILD -> "Build"
        GameMode.COUNT -> "Count"
        GameMode.MEMORY -> "Memory"
        GameMode.WORDS -> "Words"
        GameMode.FIND -> "Find it"
        GameMode.BALLOONS -> "Balloons"
        GameMode.MATCH -> "Pictures"
        GameMode.SOUNDS -> "Sounds"
    }
}

object English : EnglishLang()

/**
 * English wording for small letters. Most lines match the capital-letter ones word for word
 * ("b is for ball"), so they share voice clips; finding asks for the "small letter".
 */
object EnglishSmall : EnglishLang() {
    override fun name(letter: Letter) = if (letter.symbol.equals("a", ignoreCase = true)) "ay" else letter.symbol
    override fun find(target: Letter) = "Find the small letter|${name(target)}!"
    override fun traceAsk(letter: Letter) = "Trace the small letter|${name(letter)}!"
    override fun learnTitle(number: Int, total: Int) = "Small letter $number of $total"
    override val findTitle = "Find the small letter"
}

/** English wording for the number track: "Find the number seven", not "the letter". */
object EnglishNumbers : EnglishLang() {
    // Spoken as words: a recording of "seven" is clearer than asking what "7" should sound like.
    override fun name(letter: Letter) = letter.word ?: letter.symbol
    override fun learn(letter: Letter) = "${name(letter)}."
    override fun find(target: Letter) = "Find the number|${name(target)}!"
    override fun traceAsk(letter: Letter) = "Trace the number|${name(letter)}!"
    override fun traceAgain() = "Try again. Draw over the number."
    override fun learnTitle(number: Int, total: Int) = "Number $number of $total"
    override val findTitle = "Find the number"
}

open class HindiLang : Lang {
    override val locale: Locale = Locale.forLanguageTag("hi-IN")

    override fun name(letter: Letter) = letter.symbol

    override val praises = listOf("शाबाश!", "बहुत बढ़िया!", "वाह!", "बहुत अच्छे!")

    override fun learn(letter: Letter) =
        if (letter.word != null) "${letter.symbol}.|${letter.symbol} से ${letter.word}." else "${letter.symbol}."
    override fun find(target: Letter) = "${name(target)}|ढूंढो!"
    override fun notThis(tapped: Letter, target: Letter) = "यह|${name(tapped)}|है।|${name(target)}|ढूंढो!"
    override fun balloonsStart(first: Letter) = "गुब्बारे फोड़ो!|${first.symbol}|से शुरू करो।"
    override fun balloonsDone() = "शाबाश! तुमने सारे अक्षर फोड़ दिए!"
    override fun balloonsAgain(first: Letter) = "फिर से खेलो!|${first.symbol}|ढूंढो।"
    override fun matchAsk(letter: Letter) = "${letter.word}.|${letter.word} किस अक्षर से शुरू होता है?"
    override fun matchRight(letter: Letter, random: Random) =
        "${praise(random)}|${letter.word}, ${letter.symbol} से शुरू होता है!"
    override fun matchWrong(letter: Letter) = "फिर से कोशिश करो!|${letter.word}."
    override fun traceAsk(letter: Letter) = "${name(letter)}|बनाओ!"
    override fun traceDone(letter: Letter, random: Random) = "${praise(random)}|तुमने|${name(letter)}|लिखा!"
    override fun traceAgain() = "फिर से कोशिश करो। अक्षर के ऊपर बनाओ।"
    override fun countAsk(thing: CountThing) = "${thing.hindi} गिनो! ${thing.hindiHowMany} हैं?"
    override fun countRight(number: Letter, thing: CountThing, random: Random) =
        "${praise(random)}|${number.word}|${thing.hindi}!"
    override fun countWrong(thing: CountThing) = "फिर से गिनो!|${thing.hindiHowMany} ${thing.hindi} हैं?"
    override val memoryAsk = "जोड़ी मिलाओ!"
    override fun memoryDone() = "शाबाश! सारी जोड़ियाँ मिल गईं!"
    override fun wordAsk(word: Word) = "${word.text}|बनाओ!"

    override fun learnTitle(number: Int, total: Int) = "अक्षर $number / $total"
    override val findTitle = "अक्षर ढूंढो"
    override fun popTitle(next: Letter) = "${next.symbol} फोड़ो"
    override val doneTitle = "शाबाश!"
    override val matchTitle = "कौन सा अक्षर?"
    override fun traceTitle(number: Int, total: Int) = "लिखो $number / $total"
    override val countTitle = "कितने हैं?"
    override val memoryTitle = "जोड़ी मिलाओ"
    override val wordsTitle = "शब्द बनाओ"
    override fun modeLabel(mode: GameMode) = when (mode) {
        GameMode.LEARN -> "सीखो"
        GameMode.TRACE -> "लिखो"
        GameMode.BUILD -> "जोड़ो"
        GameMode.COUNT -> "गिनो"
        GameMode.MEMORY -> "जोड़ी"
        GameMode.WORDS -> "शब्द"
        GameMode.FIND -> "ढूंढो"
        GameMode.BALLOONS -> "गुब्बारे"
        GameMode.MATCH -> "चित्र"
        GameMode.SOUNDS -> "आवाज़"
    }
}

object Hindi : HindiLang()

/** Hindi wording for गिनती. Numbers are spoken as words (सात), which the voice reads reliably. */
object HindiNumbers : HindiLang() {
    override fun name(letter: Letter) = letter.word ?: letter.symbol
    override fun learn(letter: Letter) = "${name(letter)}."
    override fun traceAgain() = "फिर से कोशिश करो। संख्या के ऊपर बनाओ।"
    override fun learnTitle(number: Int, total: Int) = "संख्या $number / $total"
    override val findTitle = "संख्या ढूंढो"
}

/**
 * Picture tracks (colors, shapes, animals) are spoken in English with the Hindi name after it:
 * "Red!|@hi लाल!". Animals also say what sound they make.
 */
open class PictureLang(
    /** Says what to look for: "Find the color" (red), "Find the" (cow). */
    private val findPrefix: String,
    /** One item, for screen titles: "Color 3 of 9". */
    private val item: String,
) : EnglishLang() {
    override fun name(letter: Letter) = letter.word ?: letter.symbol

    private fun hindi(letter: Letter) = letter.hindi?.let { "|@hi $it!" }.orEmpty()

    private fun says(letter: Letter) = letter.sound?.let { "The ${name(letter).lowercase()} says $it!" }

    override fun learn(letter: Letter) = "${name(letter)}!${hindi(letter)}" + says(letter)?.let { "|$it" }.orEmpty()
    override fun find(target: Letter) = "$findPrefix|${name(target)}!${hindi(target)}"
    override fun found(target: Letter, random: Random) = "${praise(random)}|${name(target)}!${hindi(target)}"
    override fun notThis(tapped: Letter, target: Letter) = "That is|${name(tapped)}!|Find|${name(target)}!"

    override fun soundAsk(target: Letter) = "Who says|${target.sound}?"
    override fun soundRight(target: Letter, random: Random) = "${praise(random)}|${says(target) ?: "${name(target)}!"}"
    override fun soundWrong(tapped: Letter, target: Letter) = "That is|${name(tapped)}!|Who says|${target.sound}?"

    override fun learnTitle(number: Int, total: Int) = "$item $number of $total"
    override val findTitle = "Find it"
    val soundsTitle = "Who says it?"
}

object ColorWords : PictureLang("Find the color", "Color")
object ShapeWords : PictureLang("Find the", "Shape")
object AnimalWords : PictureLang("Find the", "Animal")

/** मराठी: the same games as Hindi, in Marathi words, with its own voice. */
object Marathi : HindiLang() {
    override val locale: Locale = Locale.forLanguageTag("mr-IN")

    override val praises = listOf("शाबास!", "खूप छान!", "व्वा!", "छान!")

    override fun learn(letter: Letter) =
        if (letter.word != null) "${letter.symbol}.|${letter.symbol},|${letter.word}." else "${letter.symbol}."
    override fun find(target: Letter) = "${name(target)}|शोधा!"
    override fun notThis(tapped: Letter, target: Letter) = "हे|${name(tapped)}|आहे.|${name(target)}|शोधा!"
    override fun balloonsStart(first: Letter) = "फुगे फोडा!|${first.symbol}|पासून सुरू करा."
    override fun balloonsDone() = "शाबास! तू सगळी अक्षरे फोडलीस!"
    override fun balloonsAgain(first: Letter) = "पुन्हा खेळूया!|${first.symbol}|शोधा."
    override fun matchAsk(letter: Letter) = "${letter.word}.|${letter.word} कोणत्या अक्षराने सुरू होते?"
    override fun matchRight(letter: Letter, random: Random) =
        "${praise(random)}|${letter.word},|${letter.symbol}|ने सुरू होते!"
    override fun matchWrong(letter: Letter) = "पुन्हा प्रयत्न कर!|${letter.word}."
    override fun traceAsk(letter: Letter) = "${name(letter)}|लिहा!"
    override fun traceDone(letter: Letter, random: Random) = "${praise(random)}|तू|${name(letter)}|लिहिलेस!"
    override fun traceAgain() = "पुन्हा प्रयत्न कर. अक्षरावर गिरव."
    override val memoryAsk = "जोड्या जुळवा!"
    override fun memoryDone() = "शाबास! सगळ्या जोड्या जुळल्या!"
    override fun wordAsk(word: Word) = "${word.text}|बनवा!"

    override fun learnTitle(number: Int, total: Int) = "अक्षर $number / $total"
    override val findTitle = "अक्षर शोधा"
    override fun popTitle(next: Letter) = "${next.symbol} फोडा"
    override val doneTitle = "शाबास!"
    override val matchTitle = "कोणते अक्षर?"
    override fun traceTitle(number: Int, total: Int) = "लिहा $number / $total"
    override val memoryTitle = "जोड्या जुळवा"
    override val wordsTitle = "शब्द बनवा"
    override fun modeLabel(mode: GameMode) = when (mode) {
        GameMode.LEARN -> "शिका"
        GameMode.TRACE -> "लिहा"
        GameMode.BUILD -> "जोडा"
        GameMode.COUNT -> "मोजा"
        GameMode.MEMORY -> "जोड्या"
        GameMode.WORDS -> "शब्द"
        GameMode.FIND -> "शोधा"
        GameMode.BALLOONS -> "फुगे"
        GameMode.MATCH -> "चित्रे"
        GameMode.SOUNDS -> "आवाज"
    }
}

/** Words for the बारहखड़ी games, which exist only in Hindi. */
object BarakhadiWords {
    fun title(consonant: String) = "$consonant की बारहखड़ी"
    const val BUILD_TITLE = "मात्रा जोड़ो"

    /** "क, आ, का": the consonant, the vowel, then the syllable they make. */
    fun sound(consonant: String, matra: Matra, syllable: String) = "$consonant,|${matra.vowel},|$syllable"
    fun intro(consonant: String) = "$consonant|की बारहखड़ी। छूकर सुनो!"
    fun buildAsk(syllable: String) = "$syllable|बनाओ! कौन सी मात्रा लगेगी?"
    fun buildRight(consonant: String, matra: Matra, syllable: String, random: Random) =
        "${Hindi.praise(random)}|$consonant|में|${matra.vowel} की मात्रा,|$syllable!"
    fun buildWrong(made: String, target: String) = "यह|$made|है।|$target|बनाओ!"
}

/** Things the app says that don't belong to one track. */
object CommonWords {
    const val LETS_PLAY = "Let's play!"
    fun hello(name: String) = "Hi $name!|$LETS_PLAY"
    fun helloHindi(name: String) = "नमस्ते $name!"
    const val YOUR_STICKERS = "Your stickers!"
    const val WIN_MORE_STARS = "Win more stars to get this sticker!"
    fun streak(days: Int) = "$days days in a row! Wow!"
    fun newSticker(sticker: Sticker) = "New sticker!|A ${sticker.name}!"
    const val REST = "Great playing today!|Time to rest now. See you tomorrow!"
    const val TODAYS_GAMES = "Today's games!"
    const val PATH_START = "Today's games!|Tap the big block to play."
    const val PATH_NEXT = "Well done!|Next game!"
    const val PATH_DONE = "You finished today's games!|Come back tomorrow for more."
    const val MY_NAME = "My name"
    const val RHYMES = "Rhymes"
    const val RHYMES_ASK = "Pick a rhyme!"
    const val NAME_START = "Let's write your name!"
    const val NAME_DONE = "Wonderful!|You wrote your name!"
}
