package com.aksharblocks.app

import android.content.Context
import android.content.res.AssetManager
import android.content.res.Configuration
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate

/** Light, dark, or whatever the phone is set to. */
enum class ThemeMode(val label: String) {
    SYSTEM("Same as the phone"),
    LIGHT("Light"),
    DARK("Dark");

    /** The matching AppCompat night mode, so the parent area's dialogs and buttons follow too. */
    val nightMode: Int
        get() = when (this) {
            SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
}

/**
 * The colors of the "colourful glass" look, for light and dark. Screens read these as they
 * draw, so switching [dark] changes every screen at once, with no restart.
 *
 * Glass is see-through white over soft colour blobs: whiter in light mode, a faint sheen in
 * dark mode. Colored blocks (letters, answers, menu cards) stay the same in both.
 */
object Theme {

    /** True while the dark look is on. Set by [apply]. */
    var dark = false
        private set

    /** Works out light or dark from the parent's choice and the phone's setting. */
    fun apply(context: Context, mode: ThemeMode = Settings(context).themeMode) {
        AppCompatDelegate.setDefaultNightMode(mode.nightMode)
        dark = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM ->
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    /** For tests: set the look directly. */
    internal fun setDarkForTest(value: Boolean) {
        dark = value
    }

    val background get() = if (dark) 0xFF0D1230.toInt() else 0xFFEEF3FF.toInt()
    val text get() = if (dark) 0xFFF4F6FF.toInt() else 0xFF1B2240.toInt()
    val subtext get() = if (dark) 0xFFAEB7DD.toInt() else 0xFF4E5880.toInt()

    /** A glass card's fill, its strong version (buttons on glass) and its bright rim. */
    val glass get() = if (dark) 0x17FFFFFF else 0x99FFFFFF.toInt()
    val glassStrong get() = if (dark) 0x26FFFFFF else 0xD9FFFFFF.toInt()
    val glassRim get() = if (dark) 0x33FFFFFF else 0xF2FFFFFF.toInt()

    /** A dark block holding white text (the letter to find, number badges): navy, or a brighter blue on the dark look. */
    val prompt get() = if (dark) 0xFF4A5BC4.toInt() else Palette.NAVY

    /** A solid card over a dimmed screen (the "New sticker!" card), where glass would look grey. */
    val card get() = if (dark) 0xFF232B5E.toInt() else 0xFFFDFDFF.toInt()

    /** The soft shadow under glass cards. */
    val shadow get() = if (dark) 0x66000000 else 0x24283C8C

    /** Empty part of a progress bar; also a "not yet" face (locked sticker, step done). */
    val track get() = if (dark) 0x26FFFFFF else 0x1A1B2240
    val muted get() = if (dark) 0xFF2B3262.toInt() else 0xFFCBD4EC.toInt()

    /** The four soft blobs drifting behind every screen. Each screen tints the first one. */
    fun blobs(tint: Int): IntArray = if (dark) {
        intArrayOf(0x8C3B5BFF.toInt(), 0x6BFF5E96, 0x6114C8A0, 0x808B5CF6.toInt())
    } else {
        intArrayOf((tint and 0x00FFFFFF) or 0xE6000000.toInt(), 0xCCFFB38A.toInt(), 0xD9A6F0C6.toInt(), 0xCCC9A7FF.toInt())
    }

    /** The big "Today's games" card: blue into purple. */
    val heroFrom get() = if (dark) 0xD92F6BFF.toInt() else 0xFF2F6BFF.toInt()
    val heroTo get() = if (dark) 0xD98B5CF6.toInt() else 0xFF8B5CF6.toInt()
}

/** Baloo 2: round and friendly, with Devanagari, so English, Hindi and Marathi match. */
object Fonts {
    private var regular: Typeface? = null
    private var bold: Typeface? = null

    fun load(assets: AssetManager) {
        regular = make(assets, 600)
        bold = make(assets, 800)
    }

    private fun make(assets: AssetManager, weight: Int): Typeface? = try {
        if (Build.VERSION.SDK_INT >= 26) {
            Typeface.Builder(assets, FILE).setFontVariationSettings("'wght' $weight").build()
        } else {
            Typeface.createFromAsset(assets, FILE)
        }
    } catch (e: RuntimeException) {
        null
    }

    /** Headings, letters and buttons. Falls back to the phone's heaviest font. */
    val heavy: Typeface
        get() = bold ?: if (Build.VERSION.SDK_INT >= 28) Typeface.create(Typeface.DEFAULT, 900, false)
        else Typeface.create("sans-serif-black", Typeface.NORMAL)

    /** Smaller lines under headings. */
    val medium: Typeface get() = regular ?: Typeface.DEFAULT_BOLD

    /** Old Android (7.x) can't pick a weight from the font, so text is thickened instead. */
    val needsFakeBold get() = Build.VERSION.SDK_INT < 26 && bold != null

    private const val FILE = "fonts/Baloo2.ttf"
}
