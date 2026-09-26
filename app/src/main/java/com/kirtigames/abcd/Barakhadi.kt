package com.kirtigames.abcd

/** A vowel sign (मात्रा). [sign] is empty for अ, which adds no mark. */
data class Matra(val vowel: String, val sign: String) {

    /** The sign on a dotted circle (◌ि), the usual way to show a matra by itself. */
    val shown: String get() = if (sign.isEmpty()) vowel else "◌$sign"
}

/** बारहखड़ी: every consonant written with each of the twelve matras (क का कि की …). */
object Barakhadi {

    val matras = listOf(
        Matra("अ", ""),
        Matra("आ", "ा"),
        Matra("इ", "ि"),
        Matra("ई", "ी"),
        Matra("उ", "ु"),
        Matra("ऊ", "ू"),
        Matra("ए", "े"),
        Matra("ऐ", "ै"),
        Matra("ओ", "ो"),
        Matra("औ", "ौ"),
        Matra("अं", "ं"),
        Matra("अः", "ः"),
    )

    /** ङ and ञ are almost never written with matras, so they are left out. */
    private val skipped = setOf("ङ", "ञ")

    /** Pairs children most often confuse, used as tricky wrong answers. */
    private val lookAlikes = mapOf(
        "ि" to "ी", "ी" to "ि",
        "ु" to "ू", "ू" to "ु",
        "े" to "ै", "ै" to "े",
        "ो" to "ौ", "ौ" to "ो",
        "ा" to "ो", "ं" to "ः", "ः" to "ं",
    )

    /** All syllables, consonant by consonant, each tagged with its consonant as its group. */
    fun syllables(consonants: List<Letter>): List<Letter> =
        consonants.filter { it.symbol !in skipped }.flatMap { consonant ->
            matras.map { Letter(consonant.symbol + it.sign, group = consonant.symbol) }
        }

    /** Which matra a syllable was built with. */
    fun matraOf(syllable: Letter): Matra {
        val consonant = syllable.group ?: return matras[0]
        return matras.firstOrNull { consonant + it.sign == syllable.symbol } ?: matras[0]
    }

    /** The look-alike of [matra] (ि for ी), or null when it has none. */
    fun lookAlike(matra: Matra): Matra? = lookAlikes[matra.sign]?.let { sign -> matras.first { it.sign == sign } }
}
