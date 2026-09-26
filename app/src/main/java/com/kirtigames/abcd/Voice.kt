package com.kirtigames.abcd

import android.content.res.AssetManager
import java.io.File
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

    /**
     * clip id → where the recording is: an asset path (bundled with the app) or, in the
     * recording-studio build, an absolute file path of a take recorded on this phone.
     */
    private var clips: Map<String, String> = emptyMap()

    private lateinit var assets: AssetManager
    private var recordingsDir: File? = null

    /** [recordingsDir] holds takes from the recording studio; it wins over bundled clips. */
    fun load(assets: AssetManager, recordingsDir: File? = null) {
        this.assets = assets
        this.recordingsDir = recordingsDir
        reload()
    }

    /** Looks for recordings again (the studio calls this after each take). */
    fun reload() {
        val found = HashMap<String, String>()
        for (language in listOf("en", "hi")) {
            for (file in assets.list("voice/$language").orEmpty()) {
                if (isAudio(file)) found[file.substringBeforeLast('.')] = "voice/$language/$file"
            }
            File(recordingsDir ?: continue, language).listFiles().orEmpty().forEach { file ->
                if (isAudio(file.name)) found[file.nameWithoutExtension] = file.absolutePath
            }
        }
        clips = found
    }

    /** True for an absolute file path (a studio take) rather than an asset. */
    fun isFile(path: String) = path.startsWith("/")

    private fun isAudio(name: String) = name.substringAfterLast('.').lowercase(Locale.ROOT) in EXTENSIONS

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
