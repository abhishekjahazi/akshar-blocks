package com.aksharblocks.app

import android.content.res.AssetManager
import java.io.File

/**
 * The letters, words and pictures for every track, read from the text files in
 * `assets/tracks/` so new content can be added without changing code.
 */
object Content {

    private val letters = HashMap<Track, List<Letter>>()
    private val strokes = HashMap<Track, Map<String, List<Stroke>>>()
    private val words = HashMap<Track, List<Word>>()

    fun letters(track: Track): List<Letter> =
        letters[track] ?: error("Content not loaded yet: ${track.name}")

    /** First words for the Words game (empty for tracks without it). */
    fun words(track: Track): List<Word> = words[track].orEmpty()

    /** How to write [letter] for tracing, or null when the track has no stroke data for it. */
    fun strokes(track: Track, letter: Letter): List<Stroke>? = strokes[track]?.get(letter.symbol)

    /** Loads every track from the app's assets. Called once when the app starts. */
    fun load(assets: AssetManager) {
        for (track in Track.entries) {
            track.wordsFile?.let { file ->
                words[track] = assets.open("words/$file").bufferedReader().use { Words.parse(it.readText()) }
            }
            if (track.file == null) continue
            letters[track] = assets.open("tracks/${track.file}").bufferedReader().use { parseFor(track, it.readText()) }
            // Stroke files are optional: letters without one are traced over their shape.
            strokes[track] = if (assets.list("tracing").orEmpty().contains(track.file)) {
                assets.open("tracing/${track.file}").bufferedReader().use { Tracing.parse(it.readText()) }
            } else {
                emptyMap()
            }
        }
        buildDerived()
        Rhymes.load(assets)
    }

    /** Loads every track from an assets folder on disk (used by tests). */
    fun loadFrom(folder: File) {
        for (track in Track.entries) {
            track.wordsFile?.let { file -> words[track] = Words.parse(File(folder, "words/$file").readText()) }
            if (track.file == null) continue
            letters[track] = parseFor(track, File(folder, "tracks/${track.file}").readText())
            val strokeFile = File(folder, "tracing/${track.file}")
            strokes[track] = if (strokeFile.exists()) Tracing.parse(strokeFile.readText()) else emptyMap()
        }
        buildDerived()
        Rhymes.loadFrom(folder)
    }

    /** Tracks made from other tracks: the बारहखड़ी comes from the consonants. */
    private fun buildDerived() {
        letters[Track.BARAKHADI] = Barakhadi.syllables(letters(Track.VYANJAN))
        strokes[Track.BARAKHADI] = emptyMap()
    }

    private fun parseFor(track: Track, text: String) = if (track.isPictures) parsePictures(text) else parse(text)

    /**
     * One letter per line: symbol, word and picture separated by tabs. Word and picture may be
     * empty. Blank lines and lines starting with `#` are ignored.
     */
    fun parse(text: String): List<Letter> =
        rows(text).map { columns ->
            Letter(
                symbol = columns[0],
                word = columns.getOrNull(1)?.ifEmpty { null },
                emoji = columns.getOrNull(2)?.ifEmpty { null },
            )
        }

    /**
     * Picture tracks, one per line: picture, English name, Hindi name and (animals) the sound it
     * makes. The picture is also the item's symbol.
     */
    fun parsePictures(text: String): List<Letter> =
        rows(text).map { columns ->
            Letter(
                symbol = columns[0],
                word = columns.getOrNull(1)?.ifEmpty { null },
                emoji = columns[0],
                hindi = columns.getOrNull(2)?.ifEmpty { null },
                sound = columns.getOrNull(3)?.ifEmpty { null },
            )
        }

    /** The tab-separated columns of each line, skipping blank lines and `#` comments. */
    private fun rows(text: String): List<List<String>> =
        text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line -> line.split('\t').map { it.trim() } }
            .toList()
}
