package com.aksharblocks.app

import android.content.res.AssetManager
import java.io.File

/** A rhyme: its title, the language it is sung in ("en", "hi", "mr"), a picture and its lines. */
data class Rhyme(val title: String, val language: String, val emoji: String, val lines: List<String>) {
    val locale get() = Voice.localeOf(language)
}

/** The rhymes in assets/rhymes/rhymes.txt. */
object Rhymes {

    var all: List<Rhyme> = emptyList()
        private set

    fun load(assets: AssetManager) {
        all = assets.open(FILE).bufferedReader().use { parse(it.readText()) }
    }

    /** Loads from an assets folder on disk (used by tests). */
    fun loadFrom(folder: File) {
        all = parse(File(folder, FILE).readText())
    }

    /**
     * A rhyme starts with `= TITLE <tab> LANGUAGE <tab> PICTURE`; the lines after it are the
     * rhyme. Blank lines and lines starting with `#` are ignored.
     */
    fun parse(text: String): List<Rhyme> {
        val rhymes = ArrayList<Rhyme>()
        var header: List<String>? = null
        val lines = ArrayList<String>()
        fun close() {
            val h = header ?: return
            if (lines.isNotEmpty()) rhymes += Rhyme(h[0], h.getOrElse(1) { "en" }, h.getOrElse(2) { "🎵" }, lines.toList())
            lines.clear()
        }
        for (raw in text.lineSequence()) {
            val line = raw.trimEnd('\r').trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("=")) {
                close()
                header = line.removePrefix("=").split('\t').map { it.trim() }
            } else if (header != null) {
                lines += line
            }
        }
        close()
        return rhymes
    }

    private const val FILE = "rhymes/rhymes.txt"
}
