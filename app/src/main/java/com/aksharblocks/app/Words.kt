package com.aksharblocks.app

/** A first word to build: its letters (as the child sees them on blocks) and a picture. */
data class Word(val text: String, val emoji: String) {
    val letters: List<String> = Words.split(text)
}

object Words {

    /**
     * Splits a word into the blocks a child builds it from: one letter each, with any marks
     * that belong to it (ड + ़ = ड़) kept on the same block.
     */
    fun split(text: String): List<String> {
        val out = ArrayList<String>()
        for (c in text) {
            if (out.isNotEmpty() && isCombiningMark(c)) out[out.lastIndex] = out.last() + c else out += c.toString()
        }
        return out
    }

    /** One word per line: WORD <tab> PICTURE. Blank lines and lines starting with # are ignored. */
    fun parse(text: String): List<Word> =
        text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val columns = line.split('\t').map { it.trim() }
                if (columns.size < 2 || columns[0].isEmpty()) null else Word(columns[0], columns[1])
            }
            .toList()
}
