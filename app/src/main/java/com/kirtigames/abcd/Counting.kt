package com.kirtigames.abcd

/** Something to count, with its names. Hindi asks कितने or कितनी to match the word's gender. */
data class CountThing(
    val emoji: String,
    val one: String,
    val many: String,
    val hindi: String,
    val hindiHowMany: String,
)

object Counting {

    /** Numbers in the counting game go up to this (one "ten frame"). */
    const val MAX = 10

    /** All are old emoji, so they show on Android 7 too. */
    val things = listOf(
        CountThing("🍎", "apple", "apples", "सेब", "कितने"),
        CountThing("⚽", "ball", "balls", "गेंदें", "कितनी"),
        CountThing("🐟", "fish", "fish", "मछलियाँ", "कितनी"),
        CountThing("⭐", "star", "stars", "तारे", "कितने"),
        CountThing("🎈", "balloon", "balloons", "गुब्बारे", "कितने"),
        CountThing("🌸", "flower", "flowers", "फूल", "कितने"),
        CountThing("🚗", "car", "cars", "गाड़ियाँ", "कितनी"),
        CountThing("🐤", "chick", "chicks", "चूज़े", "कितने"),
    )
}
