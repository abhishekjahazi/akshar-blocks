package com.aksharblocks.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import java.text.DateFormat
import java.util.Date
import kotlin.math.ceil
import kotlin.math.min

/** Certificates a child earns by learning most of a section. */
object Certificates {

    /** How many letters (or numbers, pictures…) of [track] must be known for its certificate. */
    fun goal(track: Track): Int = when {
        // 100 numbers or 400 syllables would take too long: a first milestone is worth a certificate.
        track.isNumbers -> 20
        track == Track.BARAKHADI -> 60
        else -> ceil(track.letters.size * 0.8).toInt()
    }

    /** What the certificate says the child learned. */
    fun achievement(track: Track): String = when (track) {
        Track.ENGLISH -> "the English alphabet (A–Z)"
        Track.LOWER -> "small letters (a–z)"
        Track.SWAR -> "Hindi vowels (स्वर)"
        Track.VYANJAN -> "Hindi consonants (व्यंजन)"
        Track.BARAKHADI -> "the बारहखड़ी"
        Track.NUMBERS -> "numbers 1 to 20"
        Track.GINTI -> "Hindi numbers १ to २०"
        Track.MARATHI -> "Marathi letters (मराठी अक्षरे)"
        Track.COLORS -> "colors (रंग)"
        Track.SHAPES -> "shapes (आकार)"
        Track.ANIMALS -> "animals (जानवर)"
        Track.FRUITS -> "fruits and vegetables (फल-सब्ज़ी)"
        Track.BODY -> "parts of the body (शरीर)"
        Track.FAMILY -> "family (परिवार)"
    }

    /** A medal per section, so each certificate looks a little different. */
    fun medal(track: Track): String = when {
        track.isPictures -> "🏅"
        track.isNumbers -> "🥇"
        else -> "🏆"
    }
}

/**
 * Draws a certificate: a framed cream page with a medal, the child's name, what they learned
 * and the date. The same drawing is shown to the child and shared by parents as a picture.
 */
object CertificateArt {

    private const val CREAM = 0xFFFFF8E7.toInt()
    private const val GOLD = 0xFFE0A526.toInt()
    private const val MUTED = 0xFF55638C.toInt()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val art = Paint(Paint.FILTER_BITMAP_FLAG)
    private val rect = RectF()

    fun draw(canvas: Canvas, page: RectF, name: String, track: Track, earnedDay: Long) {
        val w = page.width()
        val h = page.height()
        val unit = min(w, h * 1.4f) / 100f

        fill.color = CREAM
        canvas.drawRoundRect(page, unit * 4, unit * 4, fill)
        // A double gold frame.
        line.color = GOLD
        line.strokeWidth = unit * 1.6f
        rect.set(page.left + unit * 3, page.top + unit * 3, page.right - unit * 3, page.bottom - unit * 3)
        canvas.drawRoundRect(rect, unit * 3, unit * 3, line)
        line.strokeWidth = unit * 0.6f
        rect.inset(unit * 2.2f, unit * 2.2f)
        canvas.drawRoundRect(rect, unit * 2, unit * 2, line)

        val cx = page.centerX()
        var y = page.top + h * 0.14f
        say(canvas, "CERTIFICATE", cx, y, unit * 7.5f, Palette.INK, bold = true, maxWidth = w * 0.8f)
        y += unit * 7
        say(canvas, "Akshar Blocks", cx, y, unit * 4f, MUTED, maxWidth = w * 0.8f)

        val medal = h * 0.2f
        picture(canvas, Certificates.medal(track), cx, page.top + h * 0.36f, medal)

        y = page.top + h * 0.56f
        say(canvas, "This is to say that", cx, y, unit * 4.2f, MUTED, maxWidth = w * 0.8f)
        y += unit * 10
        say(canvas, name, cx, y, unit * 10f, track.color, bold = true, maxWidth = w * 0.8f)
        y += unit * 8
        say(canvas, "has learned", cx, y, unit * 4.2f, MUTED, maxWidth = w * 0.8f)
        y += unit * 7
        say(canvas, Certificates.achievement(track), cx, y, unit * 6f, Palette.INK, bold = true, maxWidth = w * 0.84f)

        val date = DateFormat.getDateInstance(DateFormat.LONG).format(Date(earnedDay * DAY_MS + DAY_MS / 2))
        say(canvas, "⭐  $date  ⭐", cx, page.bottom - h * 0.08f, unit * 4f, MUTED, maxWidth = w * 0.8f)
    }

    /** The certificate as a picture, for sharing. */
    fun bitmap(width: Int, height: Int, name: String, track: Track, earnedDay: Long): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Palette.WHITE)
        draw(canvas, RectF(0f, 0f, width.toFloat(), height.toFloat()), name, track, earnedDay)
        return bitmap
    }

    private fun say(canvas: Canvas, value: String, cx: Float, y: Float, size: Float, color: Int, bold: Boolean = false, maxWidth: Float) {
        text.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        text.textSize = size
        text.color = color
        val measured = text.measureText(value)
        if (measured > maxWidth) text.textSize = size * maxWidth / measured
        val metrics = text.fontMetrics
        canvas.drawText(value, cx, y - (metrics.ascent + metrics.descent) / 2f, text)
    }

    private fun picture(canvas: Canvas, emoji: String, cx: Float, cy: Float, size: Float) {
        val bitmap = Art.bitmap(emoji, size)
        if (bitmap == null) {
            say(canvas, emoji, cx, cy, size * 0.8f, Palette.INK, maxWidth = size * 2)
            return
        }
        rect.set(cx - size / 2f, cy - size / 2f, cx + size / 2f, cy + size / 2f)
        canvas.drawBitmap(bitmap, null, rect, art)
    }

    /** Day numbers count local days; noon keeps the date right whatever the time zone offset. */
    private const val DAY_MS = 24L * 60 * 60 * 1000
}
