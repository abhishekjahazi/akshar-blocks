package com.kirtigames.abcd

import android.content.Context

data class Profile(val id: Int, val name: String, val avatar: String)

/** The children who use the app, and which one is playing now. Kept on the device only. */
class ProfileStore(private val context: Context) {

    private val prefs = context.getSharedPreferences("profiles", Context.MODE_PRIVATE)

    init {
        if (all().isEmpty()) {
            save(listOf(Profile(1, "Child 1", AVATARS[0])))
            prefs.edit().putInt(KEY_CURRENT, 1).apply()
            migrateOldStars()
        }
    }

    fun all(): List<Profile> =
        prefs.getString(KEY_LIST, "").orEmpty().lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split('\t')
                val id = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                Profile(id, parts.getOrElse(2) { "" }, parts.getOrElse(1) { AVATARS[0] })
            }
            .toList()

    var currentId: Int
        get() = prefs.getInt(KEY_CURRENT, 1)
        set(value) = prefs.edit().putInt(KEY_CURRENT, value).apply()

    fun current(): Profile = all().let { list -> list.firstOrNull { it.id == currentId } ?: list.first() }

    val canAdd get() = all().size < MAX_PROFILES

    fun add(name: String, avatar: String): Profile? {
        val list = all()
        if (list.size >= MAX_PROFILES) return null
        val profile = Profile((list.maxOfOrNull { it.id } ?: 0) + 1, clean(name), avatar)
        save(list + profile)
        return profile
    }

    fun update(profile: Profile) {
        save(all().map { if (it.id == profile.id) profile.copy(name = clean(profile.name)) else it })
    }

    /** Removes a child and their progress. The last remaining child can't be removed. */
    fun remove(id: Int) {
        val remaining = all().filter { it.id != id }
        if (remaining.isEmpty()) return
        save(remaining)
        Player.erase(context, id)
        if (currentId == id) currentId = remaining.first().id
    }

    private fun save(list: List<Profile>) {
        prefs.edit().putString(KEY_LIST, list.joinToString("\n") { "${it.id}\t${it.avatar}\t${it.name}" }).apply()
    }

    /** Stars earned before profiles existed belong to the first child. */
    private fun migrateOldStars() {
        val old = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
        val stars = old.getInt("stars", 0)
        if (stars > 0) {
            Player(context, current()).setStars(stars)
            old.edit().clear().apply()
        }
    }

    private fun clean(name: String) =
        name.replace(Regex("[\\t\\n\\r]"), " ").trim().take(MAX_NAME_LENGTH).ifEmpty { "Child" }

    companion object {
        const val MAX_PROFILES = 4
        const val MAX_NAME_LENGTH = 16
        val AVATARS = listOf("🐯", "🐼", "🐵", "🐸", "🐰", "🐶", "🐱", "🦁")
        private const val KEY_LIST = "list"
        private const val KEY_CURRENT = "current"
    }
}

/** One child's stars and letter-by-letter results. */
class Player(context: Context, val profile: Profile) {

    private val prefs = context.getSharedPreferences(fileFor(profile.id), Context.MODE_PRIVATE)

    val stars: Int get() = prefs.getInt(KEY_STARS, 0)

    fun addStar() = setStars(stars + 1)

    fun setStars(count: Int) = prefs.edit().putInt(KEY_STARS, count).apply()

    /** The child picked [letter] when asked for it. */
    fun correct(track: Track, letter: Letter) = bump("$OK|${track.name}|${letter.symbol}")

    /** The child picked [tapped] when asked for [target]. */
    fun wrong(track: Track, target: Letter, tapped: Letter) {
        bump("$MISS|${track.name}|${target.symbol}")
        bump("$MIX|${track.name}|${target.symbol}|${tapped.symbol}")
    }

    fun report(track: Track): TrackReport {
        val ok = HashMap<String, Int>()
        val miss = HashMap<String, Int>()
        val mix = HashMap<Pair<String, String>, Int>()
        for ((key, value) in prefs.all) {
            val count = value as? Int ?: continue
            val parts = key.split('|')
            if (parts.getOrNull(1) != track.name) continue
            when (parts[0]) {
                OK -> ok[parts[2]] = count
                MISS -> miss[parts[2]] = count
                MIX -> if (parts.size == 4) mix[parts[2] to parts[3]] = count
            }
        }
        return Report.build(track.letters.map { it.symbol }, ok, miss, mix)
    }

    fun reset() = prefs.edit().clear().apply()

    private fun bump(key: String) = prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()

    companion object {
        private const val KEY_STARS = "stars"
        private const val OK = "ok"
        private const val MISS = "miss"
        private const val MIX = "mix"

        private fun fileFor(id: Int) = "progress_$id"

        fun erase(context: Context, id: Int) {
            context.deleteSharedPreferences(fileFor(id))
        }
    }
}

/** What a parent sees for one track. */
data class TrackReport(
    val known: Int,
    val total: Int,
    /** Letters the child most often gets wrong, worst first. */
    val practice: List<String>,
    /** Pairs of letters the child confuses, most often first. */
    val mixUps: List<Pair<String, String>>,
    val played: Boolean,
)

object Report {
    /** A letter counts as known after this many right answers. */
    const val KNOWN_AFTER = 3

    fun build(
        symbols: List<String>,
        ok: Map<String, Int>,
        miss: Map<String, Int>,
        mix: Map<Pair<String, String>, Int>,
    ): TrackReport {
        val known = symbols.count { (ok[it] ?: 0) >= KNOWN_AFTER }
        val practice = symbols
            .filter { (miss[it] ?: 0) >= 2 && (miss[it] ?: 0) > (ok[it] ?: 0) / 2 }
            .sortedByDescending { miss[it] ?: 0 }
            .take(5)

        // "b picked for d" and "d picked for b" are the same confusion.
        val pairs = HashMap<Pair<String, String>, Int>()
        for ((pair, count) in mix) {
            val key = if (pair.first <= pair.second) pair else pair.second to pair.first
            pairs[key] = (pairs[key] ?: 0) + count
        }
        val mixUps = pairs.filterValues { it >= 2 }.entries.sortedByDescending { it.value }.take(3).map { it.key }

        return TrackReport(known, symbols.size, practice, mixUps, played = ok.isNotEmpty() || miss.isNotEmpty())
    }
}
