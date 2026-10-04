package com.aksharblocks.app

/** Where a dress-up item goes on the child's animal. */
enum class Slot { HEAD, EYES, NECK, HAND }

/** Something to dress the child's animal in, bought with stars (never with money). */
data class Outfit(val id: String, val emoji: String, val name: String, val slot: Slot, val price: Int)

/** The reward shop: what can be bought, and the rules for buying and wearing. */
object Outfits {

    /** Cheapest first, so the first things come quickly. */
    val all = listOf(
        Outfit("bow", "🎀", "bow", Slot.HEAD, 5),
        Outfit("balloon", "🎈", "balloon", Slot.HAND, 5),
        Outfit("cap", "🧢", "cap", Slot.HEAD, 10),
        Outfit("glasses", "👓", "glasses", Slot.EYES, 10),
        Outfit("scarf", "🧣", "scarf", Slot.NECK, 15),
        Outfit("sunglasses", "🕶️", "sunglasses", Slot.EYES, 20),
        Outfit("hat", "🎩", "top hat", Slot.HEAD, 25),
        Outfit("crown", "👑", "crown", Slot.HEAD, 40),
    )

    fun byId(id: String): Outfit? = all.firstOrNull { it.id == id }

    /** Ids saved as one comma-separated string; unknown ids (from a newer version) are dropped. */
    fun decode(saved: String?): List<Outfit> = saved.orEmpty().split(',').mapNotNull(::byId)

    fun encode(outfits: List<Outfit>): String = outfits.joinToString(",") { it.id }

    /**
     * What is worn after tapping an owned [outfit]: off if it was on, otherwise on, replacing
     * whatever was in the same slot (one hat at a time).
     */
    fun toggle(wearing: List<Outfit>, outfit: Outfit): List<Outfit> =
        if (outfit in wearing) wearing - outfit else wearing.filter { it.slot != outfit.slot } + outfit

    const val ASK = "Dress up your animal!|Buy things with your stars."
    const val NEED_MORE = "Win more stars to get this!"
    fun bought(outfit: Outfit) = "Yay!|You got the ${outfit.name}!"
}
