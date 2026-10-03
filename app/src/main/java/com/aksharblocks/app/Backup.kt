package com.aksharblocks.app

import android.content.Context

/** A backup's saved values, by preferences file and then by key. */
typealias BackupData = Map<String, Map<String, Any>>

/**
 * A backup of the children and their progress, as a plain text file a parent saves wherever
 * they like (the phone, Google Drive, a computer). Android backups are off on purpose, so this
 * is how progress survives a new phone or a reinstall.
 *
 * The file is one line per saved value: `file <tab> key <tab> type <tab> value`, under a header
 * line. Tabs, line breaks and backslashes inside keys and values are escaped.
 */
object Backup {

    const val HEADER = "Akshar Blocks backup 1"

    fun encode(data: BackupData): String = buildString {
        append(HEADER).append('\n')
        for ((file, values) in data.toSortedMap()) {
            for ((key, value) in values.toSortedMap()) {
                val type = when (value) {
                    is Int -> "i"
                    is Long -> "l"
                    is Float -> "f"
                    is Boolean -> "b"
                    is String -> "s"
                    else -> continue // nothing the app saves; skipped rather than guessed at
                }
                append(escape(file)).append('\t').append(escape(key)).append('\t')
                    .append(type).append('\t').append(escape(value.toString())).append('\n')
            }
        }
    }

    /** Reads a backup; throws [IllegalArgumentException] for anything that isn't one. */
    fun decode(text: String): BackupData {
        val lines = text.lineSequence().map { it.trimEnd('\r') }.filter { it.isNotEmpty() }.toList()
        require(lines.firstOrNull() == HEADER) { "Not an Akshar Blocks backup" }
        val data = LinkedHashMap<String, MutableMap<String, Any>>()
        for (line in lines.drop(1)) {
            val parts = line.split('\t')
            require(parts.size == 4) { "Damaged line in backup" }
            val (file, key, type, raw) = parts.map(::unescape)
            require(file == PROFILES || file == SETTINGS || file.matches(PROGRESS)) { "Unknown part of backup: $file" }
            val value: Any = when (type) {
                "i" -> raw.toInt()
                "l" -> raw.toLong()
                "f" -> raw.toFloat()
                "b" -> raw.toBooleanStrict()
                "s" -> raw
                else -> throw IllegalArgumentException("Unknown value type in backup")
            }
            data.getOrPut(file) { LinkedHashMap() }[key] = value
        }
        require(data[PROFILES].orEmpty().isNotEmpty()) { "The backup has no children in it" }
        return data
    }

    /** Everything worth keeping: the children, their progress and the parent's settings. */
    fun export(context: Context): String {
        val files = listOf(PROFILES, SETTINGS) + ProfileStore(context).all().map { "progress_${it.id}" }
        return encode(files.associateWith { name ->
            @Suppress("UNCHECKED_CAST")
            (context.getSharedPreferences(name, Context.MODE_PRIVATE).all.filterValues { it != null } as Map<String, Any>)
        })
    }

    /**
     * Replaces the children, progress and settings on this phone with [text]'s. Checked first,
     * so a broken file changes nothing. Returns how many children were restored.
     */
    fun restore(context: Context, text: String): Int {
        val data = decode(text)
        // Clear what's there now (cleared, not deleted, so open copies of the settings see it).
        val old = ProfileStore(context).all().map { "progress_${it.id}" }
        for (name in (old + data.keys + listOf(PROFILES, SETTINGS)).toSet()) {
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
        for ((name, values) in data) {
            val editor = context.getSharedPreferences(name, Context.MODE_PRIVATE).edit()
            for ((key, value) in values) {
                when (value) {
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is String -> editor.putString(key, value)
                }
            }
            editor.commit()
        }
        return ProfileStore(context).all().size
    }

    private fun escape(text: String) =
        text.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r")

    private fun unescape(text: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\\' && i + 1 < text.length) {
                out.append(
                    when (text[i + 1]) {
                        't' -> '\t'
                        'n' -> '\n'
                        'r' -> '\r'
                        else -> text[i + 1]
                    },
                )
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }

    private const val PROFILES = "profiles"
    private const val SETTINGS = "settings"
    private val PROGRESS = Regex("progress_[0-9]+")
}
