package com.aksharblocks.app

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

/**
 * Bundled picture artwork (Google's Noto Emoji, Apache 2.0, in assets/art/), so every phone
 * shows the same pictures instead of its own emoji font. Emoji without artwork fall back
 * to the font.
 */
object Art {

    private lateinit var assets: AssetManager
    private var available: Set<String> = emptySet()

    /** Decoded pictures, keyed by file and scale. Bounded so a page of stickers can't use too much memory. */
    private val cache = object : LruCache<String, Bitmap>(CACHE_KB) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun load(assets: AssetManager) {
        this.assets = assets
        available = assets.list("art").orEmpty().toSet()
    }

    /** Noto's file name for an emoji: its code points in hex, without the FE0F variation selector. */
    fun fileName(emoji: String): String {
        val parts = ArrayList<String>()
        var i = 0
        while (i < emoji.length) {
            val cp = Character.codePointAt(emoji, i)
            if (cp != 0xFE0F) parts += String.format(java.util.Locale.ROOT, "%04x", cp)
            i += Character.charCount(cp)
        }
        return "emoji_u" + parts.joinToString("_") + ".png"
    }

    fun has(emoji: String): Boolean = fileName(emoji) in available

    /** The picture for [emoji], decoded no bigger than needed for [sizePx], or null if there's none. */
    fun bitmap(emoji: String, sizePx: Float): Bitmap? {
        val file = fileName(emoji)
        if (file !in available) return null
        // Pictures are 512 px; halve while still at least as big as they'll be drawn.
        var sample = 1
        while (SOURCE_SIZE / (sample * 2) >= sizePx && sample < 8) sample *= 2
        val key = "$file@$sample"
        cache.get(key)?.let { return it }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = assets.open("art/$file").use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        cache.put(key, bitmap)
        return bitmap
    }

    private const val SOURCE_SIZE = 512
    private const val CACHE_KB = 24 * 1024
}
