package com.aksharblocks.app

/** One letter of a child's name, with the track that knows how to write and say it. */
data class NameLetter(val letter: Letter, val track: Track)

/**
 * Splits a child's name into letters to trace. English names start with a capital and go on
 * in small letters (A a r a v), which both have stroke order; Hindi and Marathi names are split
 * into the blocks a child writes (रि + या), each traced over its shape.
 */
object NameLetters {

    /** Longer names are cut here; tracing twelve letters is plenty for a small child. */
    const val MAX = 12

    fun of(name: String): List<NameLetter> {
        val devanagari = name.any { it in 'ऀ'..'ॿ' }
        return if (devanagari) hindi(name) else english(name)
    }

    /** The first [count] letters, for Home's "My name" card (a pencil when there are none). */
    fun sample(name: String, count: Int): List<String> =
        of(name).take(count).map { it.letter.symbol }.ifEmpty { listOf("✍️") }

    private fun english(name: String): List<NameLetter> =
        name.filter { it in 'A'..'Z' || it in 'a'..'z' }.take(MAX).mapIndexed { i, c ->
            if (i == 0) NameLetter(Letter(c.uppercase()), Track.ENGLISH) else NameLetter(Letter(c.lowercase()), Track.LOWER)
        }

    private fun hindi(name: String): List<NameLetter> {
        val known = listOf(Track.SWAR, Track.VYANJAN, Track.BARAKHADI)
        return Words.split(name.filter { it in 'ऀ'..'ॿ' && it != '।' }).take(MAX).map { block ->
            val track = known.firstOrNull { track -> track.letters.any { it.symbol == block } } ?: Track.VYANJAN
            NameLetter(Letter(block), track)
        }
    }
}
