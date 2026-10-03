package com.aksharblocks.app

import android.content.res.AssetManager
import java.security.MessageDigest
import java.util.Locale

/**
 * Recorded voice clips (assets/voice/en, hi and mr). Every spoken sentence is made of parts
 * separated by `|`; a sentence is played from recordings only when every part has one,
 * otherwise the phone voice reads the whole sentence, so voices never mix mid-sentence.
 *
 * A part is in the sentence's language unless it starts with a language tag: in
 * "Cow!|@hi गाय!" the second part is Hindi. That lets picture tracks name things in both
 * English and Hindi.
 *
 * A part's file name comes from its text (see [clipId]), so recordings keep working when the
 * script is regenerated. store/voice/ has the recording scripts.
 */
object Voice {

    /** Languages with recordings; also the tags a part can start with (`@hi `). */
    val LANGUAGES = listOf("en", "hi", "mr")

    /** File extensions accepted for recordings, in order of preference. */
    val EXTENSIONS = listOf("m4a", "mp3", "ogg", "wav")

    /** One part of a sentence and the language it is spoken in. */
    data class Segment(val language: String, val text: String)

    /** clip id → asset path, for the recordings bundled with the app. */
    private var clips: Map<String, String> = emptyMap()

    fun load(assets: AssetManager) {
        val found = HashMap<String, String>()
        for (language in LANGUAGES) {
            for (file in assets.list("voice/$language").orEmpty()) {
                val id = file.substringBeforeLast('.')
                if (file.substringAfterLast('.').lowercase(Locale.ROOT) in EXTENSIONS) found[id] = "voice/$language/$file"
            }
        }
        clips = found
    }

    /** The sentence as one string, parts joined with spaces (language tags removed). */
    fun spokenText(text: String): String = parts(text).joinToString(" ")

    /** [text] with every part tagged as [language], so it keeps its voice inside another sentence. */
    fun tagged(text: String, language: String): String =
        segments(text, language).joinToString("|") { "@${it.language} ${it.text}" }

    /** The parts of [text] as written, language tags removed. */
    fun parts(text: String): List<String> = segments(text, "en").map { it.text }

    /** The parts of [text], each with its language; untagged parts are in [language]. */
    fun segments(text: String, language: String): List<Segment> =
        text.split('|').map { it.trim() }.filter { it.isNotEmpty() }.mapNotNull { part ->
            val tag = LANGUAGES.firstOrNull { part.startsWith("@$it ") }
            val body = if (tag == null) part else part.substring(tag.length + 2).trim()
            if (body.isEmpty()) null else Segment(tag ?: language, body)
        }

    /**
     * The sentence as the phone voice should read it: runs of parts in the same language,
     * joined with spaces, in order.
     */
    fun spokenRuns(text: String, locale: Locale): List<Pair<Locale, String>> {
        val runs = ArrayList<Pair<String, StringBuilder>>()
        for (segment in segments(text, languageOf(locale))) {
            val last = runs.lastOrNull()
            if (last != null && last.first == segment.language) last.second.append(' ').append(segment.text)
            else runs += segment.language to StringBuilder(segment.text)
        }
        return runs.map { (language, words) -> localeOf(language) to words.toString() }
    }

    /** Recordings for every part of [text], in order, or null if any part is missing. */
    fun clipsFor(text: String, locale: Locale): List<String>? {
        if (clips.isEmpty()) return null
        val segments = segments(text, languageOf(locale))
        if (segments.isEmpty()) return null
        return segments.map { clips[clipId(it.language, it.text)] ?: return null }
    }

    fun languageOf(locale: Locale) = when (locale.language) {
        "hi" -> "hi"
        "mr" -> "mr"
        else -> "en"
    }

    fun localeOf(language: String): Locale = when (language) {
        "hi" -> Hindi.locale
        "mr" -> Marathi.locale
        else -> English.locale
    }

    /** What identifies a part: its words, ignoring case, spacing and punctuation. */
    fun key(part: String): String =
        part.lowercase(Locale.ROOT)
            .replace(Regex("[.!?,।…:;\"“”]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    /** The file name (without extension) a recording of [part] must have, like en_3fa9c21b. */
    fun clipId(language: String, part: String): String {
        val digest = MessageDigest.getInstance("SHA-1").digest(key(part).toByteArray(Charsets.UTF_8))
        return language + "_" + digest.take(4).joinToString("") { "%02x".format(it) }
    }
}
