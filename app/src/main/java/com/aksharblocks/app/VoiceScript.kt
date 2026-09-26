package com.aksharblocks.app

import kotlin.random.Random

/**
 * Builds the recording scripts: every part the app can say, once, with the file name its
 * recording needs. Generated from the same code that speaks, so it can't fall out of date.
 * Parts used for many different letters ("Find the letter", "शाबाश!") come first, as they
 * pay off most.
 */
object VoiceScript {

    data class Line(val file: String, val text: String, val section: String)

    const val EVERYDAY = "Everyday phrases"

    /** Lines per language ("en", "hi"), in recording order. */
    fun lines(): Map<String, List<Line>> {
        val collector = Collector()
        collector.collect()
        return collector.result()
    }

    private class Collector {
        private class Entry(val text: String, val section: String, val subjects: MutableSet<String> = HashSet())

        private val entries = mapOf("en" to LinkedHashMap<String, Entry>(), "hi" to LinkedHashMap())
        private val random = Random(0)

        fun add(language: String, section: String, subject: String, text: String) {
            for (part in Voice.parts(text)) {
                val id = Voice.clipId(language, part)
                entries.getValue(language).getOrPut(id) { Entry(part, section) }.subjects += subject
            }
        }

        fun add(lang: Lang, section: String, subject: String, vararg texts: String) {
            for (text in texts) add(Voice.languageOf(lang.locale), section, subject, text)
        }

        fun collect() {
            // Things said everywhere.
            for (lang in listOf(English, Hindi)) {
                lang.praises.forEach { add(lang, EVERYDAY, "*", it) }
                GameMode.entries.forEach { add(lang, EVERYDAY, "*", lang.modeLabel(it)) }
            }
            with(CommonWords) {
                add(English, EVERYDAY, "*", LETS_PLAY, YOUR_STICKERS, WIN_MORE_STARS, REST)
            }

            for (track in Track.entries) {
                val lang = track.lang
                val section = track.label
                val letters = track.letters
                if (track == Track.BARAKHADI) {
                    collectBarakhadi(track)
                    continue
                }
                letters.forEachIndexed { i, letter ->
                    val other = letters[(i + 1) % letters.size]
                    val s = letter.symbol
                    add(
                        lang, section, s,
                        lang.name(letter), lang.learn(letter), lang.find(letter), lang.found(letter, random),
                        lang.notThis(letter, other), lang.traceAsk(letter), lang.traceDone(letter, random),
                    )
                    if (GameMode.BALLOONS in track.modes) add(lang, section, s, lang.balloonsStart(letter), lang.balloonsAgain(letter))
                    if (GameMode.MATCH in track.modes && letter.hasPicture) {
                        add(lang, section, s, lang.matchAsk(letter), lang.matchRight(letter, random), lang.matchWrong(letter))
                    }
                }
                add(lang, section, "*", lang.traceAgain())
                if (GameMode.BALLOONS in track.modes) add(lang, section, "*", lang.balloonsDone())
                if (GameMode.WORDS in track.modes) {
                    for (word in track.words) {
                        add(lang, section, word.text, lang.wordAsk(word), lang.wordDone(word, random), "${word.text}!")
                        word.letters.forEach { add(lang, section, word.text, "${lang.name(Letter(it))}!") }
                    }
                }
                if (GameMode.MEMORY in track.modes) {
                    add(lang, section, "*", lang.memoryAsk, lang.memoryDone())
                    letters.filter { it.hasPicture }.forEach { add(lang, section, it.symbol, "${it.word}!") }
                }
                if (GameMode.COUNT in track.modes) {
                    for (thing in Counting.things) {
                        add(lang, section, thing.emoji, lang.countAsk(thing), lang.countWrong(thing))
                        letters.take(Counting.MAX).forEach { add(lang, section, it.symbol, lang.countRight(it, thing, random)) }
                    }
                }
            }

            for (sticker in Stickers.all) {
                add(English, "Stickers", sticker.emoji, sticker.name, CommonWords.newSticker(sticker))
            }
        }

        private fun collectBarakhadi(track: Track) {
            val section = track.label
            val rows = track.letters.groupBy { it.group!! }
            for ((consonant, row) in rows) {
                add(Hindi, section, consonant, BarakhadiWords.intro(consonant))
                row.forEach { syllable ->
                    val matra = Barakhadi.matraOf(syllable)
                    add(
                        Hindi, section, syllable.symbol,
                        BarakhadiWords.sound(consonant, matra, syllable.symbol),
                        BarakhadiWords.buildAsk(syllable.symbol),
                        BarakhadiWords.buildRight(consonant, matra, syllable.symbol, random),
                        BarakhadiWords.buildWrong(row.first().symbol, syllable.symbol),
                        Hindi.find(syllable), Hindi.traceAsk(syllable),
                    )
                }
            }
        }

        fun result(): Map<String, List<Line>> = entries.mapValues { (language, byId) ->
            // A part used for three or more different letters is an everyday phrase.
            val lines = byId.map { (id, entry) ->
                val everyday = entry.subjects.size >= 3 || "*" in entry.subjects
                Line(id, entry.text, if (everyday) EVERYDAY else entry.section)
            }
            lines.filter { it.section == EVERYDAY } + lines.filter { it.section != EVERYDAY }
        }
    }
}
