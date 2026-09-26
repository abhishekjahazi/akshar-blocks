package com.kirtigames.abcd

import android.content.res.AssetManager
import java.io.File

/**
 * The letters, words and pictures for every track, read from the text files in
 * `assets/tracks/` so new content can be added without changing code.
 */
object Content {

    private val letters = HashMap<Track, List<Letter>>()

    fun letters(track: Track): List<Letter> =
        letters[track] ?: error("Content not loaded yet: ${track.name}")

    /** Loads every track from the app's assets. Called once when the app starts. */
    fun load(assets: AssetManager) {
        for (track in Track.entries) {
            letters[track] = assets.open("tracks/${track.file}").bufferedReader().use { parse(it.readText()) }
        }
    }

    /** Loads every track from a folder on disk (used by tests). */
    fun loadFrom(folder: File) {
        for (track in Track.entries) {
            letters[track] = parse(File(folder, track.file).readText())
        }
    }

    /**
     * One letter per line: symbol, word and picture separated by tabs. Word and picture may be
     * empty. Blank lines and lines starting with `#` are ignored.
     */
    fun parse(text: String): List<Letter> =
        text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val columns = line.split('\t').map { it.trim() }
                Letter(
                    symbol = columns[0],
                    word = columns.getOrNull(1)?.ifEmpty { null },
                    emoji = columns.getOrNull(2)?.ifEmpty { null },
                )
            }
            .toList()
}
