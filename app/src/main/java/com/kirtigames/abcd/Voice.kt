package com.kirtigames.abcd

import android.content.res.AssetManager
import java.security.MessageDigest
import java.util.Locale

/**
 * Recorded voice clips (assets/voice/en and assets/voice/hi). Every spoken sentence is made of
 * parts separated by `|`; a sentence is played from recordings only when every part has one,
 * otherwise the phone voice reads the whole sentence, so voices never mix mid-sentence.
 *
 * A part's file name comes from its text (see [clipId]), so recordings keep working when the
 * script is regenerated. store/voice/ has the recording scripts.
 */
object Voice {

    /** File extensions accepted for recordings, in order of preference. */
    val EXTENSIONS = listOf("m4a", "mp3", "ogg", "wav")

    /** clip id → asset path, for the recordings bundled with the app. */
    private var clips: Map<String, String> = emptyMap()

    fun load(assets: AssetManager) {
        val found = HashMap<String, String>()
        for (language in listOf("en", "hi")) {
            for (file in assets.list("voice/$language").orEmpty()) {
                val id = file.substringBeforeLast('.')
                if (file.substringAfterLast('.').lowercase(Locale.ROOT) in EXTENSIONS) found[id] = "voice/$language/$file"
            }
        }
        clips = found
    }

    /** The sentence as the phone voice should read it. */
    fun spokenText(text: String): String = parts(text).joinToString(" ")

    fun parts(text: String): List<String> = text.split('|').map { it.trim() }.filter { it.isNotEmpty() }

    /** Recordings for every part of [text], in order, or null if any part is missing. */
    fun clipsFor(text: String, locale: Locale): List<String>? {
        if (clips.isEmpty()) return null
        val language = languageOf(locale)
        val parts = parts(text)
        if (parts.isEmpty()) return null
        return parts.map { clips[clipId(language, it)] ?: return null }
    }

    fun languageOf(locale: Locale) = if (locale.language == "hi") "hi" else "en"

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
