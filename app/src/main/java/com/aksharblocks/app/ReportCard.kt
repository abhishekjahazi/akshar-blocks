package com.aksharblocks.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.text.DateFormat
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/**
 * A child's report card for parents: stars and streak, a week of play time, and for each
 * section a level, a progress bar, a colour-coded letter map, and what needs practice.
 */
object ReportCard {

    private const val MUTED = 0xFF55638C.toInt()
    private const val LINE = 0xFFD6E0F5.toInt()

    fun build(context: Context, profile: Profile): LinearLayout {
        val player = Player(context, profile)
        val page = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Palette.ICE)
            setPadding(dp(context, 16), dp(context, 16), dp(context, 16), dp(context, 24))
        }

        // Who, when, and the headline numbers.
        page.addView(text(context, "Report card", 15f, color = MUTED))
        page.addView(text(context, "${profile.avatar}  ${profile.name}", 28f, bold = true, top = 2))
        page.addView(text(context, DateFormat.getDateInstance(DateFormat.LONG).format(Date()), 14f, color = MUTED, top = 2))
        val streak = player.streak
        page.addView(text(
            context,
            buildString {
                append("⭐ ${player.stars} stars   🎁 ${Stickers.unlocked(player.stars)} stickers")
                if (streak >= 2) append("   🔥 $streak days in a row")
            },
            16f, top = 10,
        ))
        val certificates = player.certificates().keys
        if (certificates.isNotEmpty()) {
            page.addView(text(context, "🏆 Certificates: ${certificates.joinToString(", ") { Certificates.achievement(it) }}", 15f, top = 6))
        }

        page.addView(card(context) {
            addView(text(context, "This week", 17f, bold = true))
            val minutes = player.minutesByDay(7)
            addView(text(context, "${minutes.sum()} minutes of play", 14f, color = MUTED, top = 2))
            addView(WeekChart(context, minutes).apply { layoutParams = full(context, top = 10, height = 120) })
        })

        val notTried = ArrayList<String>()
        for (track in Track.entries) {
            val report = player.report(track)
            if (!report.played) {
                notTried += track.label
                continue
            }
            val stats = player.stats(track)
            val share = if (report.total == 0) 0f else report.known / report.total.toFloat()
            page.addView(card(context) {
                addView(text(context, track.label, 18f, bold = true))
                val unit = when {
                    track == Track.BARAKHADI -> "syllables"
                    track.isNumbers -> "numbers"
                    track.isPictures -> "pictures"
                    else -> "letters"
                }
                addView(text(context, "${Report.level(share)}  ·  ${report.known} of ${report.total} $unit known", 15f, top = 2))
                addView(LinearProgressIndicator(context).apply {
                    max = report.total
                    progress = report.known
                    trackThickness = dp(context, 8)
                    trackCornerRadius = dp(context, 4)
                    setIndicatorColor(track.color)
                    layoutParams = full(context, top = 8)
                })
                // The letter map shows every letter; the बारहखड़ी has too many (408) to be readable.
                if (track != Track.BARAKHADI) {
                    val all = track.letters.map { it.symbol to Report.mastery(it.symbol, stats) }
                    // 100 numbers would make a very long card: show up to the highest one tried.
                    val tiles = if (track.isNumbers) all.take(numbersShown(all.map { it.second })) else all
                    addView(LetterMap(context, tiles).apply { layoutParams = full(context, top = 12) })
                    if (tiles.size < all.size) {
                        addView(text(context, "Showing ${tiles.first().first}–${tiles.last().first} of ${all.size}", 13f, color = MUTED, top = 6))
                    }
                }
                if (report.practice.isNotEmpty()) {
                    addView(text(context, "Needs practice:  ${report.practice.joinToString("  ")}", 15f, bold = true, top = 10))
                    addView(text(context, "Tip: play Find it and Trace with these — the games already bring them up more often.", 13f, color = MUTED))
                }
                if (report.mixUps.isNotEmpty()) {
                    val pairs = report.mixUps.joinToString(",  ") { "${it.first} ↔ ${it.second}" }
                    addView(text(context, "Often mixes up:  $pairs", 15f, bold = true, top = 8))
                    addView(text(context, "Tip: point to both, say each sound, and trace them one after the other.", 13f, color = MUTED))
                }
            })
        }
        if (notTried.isNotEmpty()) {
            page.addView(text(context, "Not tried yet: ${notTried.joinToString(", ")}", 14f, color = MUTED, top = 14))
        }
        page.addView(legend(context))
        page.addView(text(context, "Made with Akshar Blocks", 12f, color = MUTED, top = 16).apply { gravity = Gravity.CENTER })
        return page
    }

    /** How many numbers the map shows: up to the last one tried, in whole rows of ten, at least 20. */
    internal fun numbersShown(mastery: List<Mastery>): Int {
        val last = mastery.indexOfLast { it != Mastery.NOT_YET }
        val rounded = (last / 10 + 1) * 10
        return rounded.coerceIn(minOf(20, mastery.size), mastery.size)
    }

    private fun legend(context: Context) = text(
        context, "Letter map:  🟩 knows it   🟨 learning   🟥 needs practice   ⬜ not tried yet",
        13f, color = MUTED, top = 14,
    )

    /** A white rounded card; [content] fills its body. */
    private fun card(context: Context, content: LinearLayout.() -> Unit): View {
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 16))
            content()
        }
        return MaterialCardView(context).apply {
            radius = dp(context, 16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(context, 1)
            strokeColor = LINE
            addView(body)
            layoutParams = full(context, top = 12)
        }
    }

    private fun text(context: Context, value: String, sizeSp: Float, bold: Boolean = false, color: Int = Palette.INK, top: Int = 0) =
        TextView(context).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(0f, 1.1f)
            layoutParams = full(context, top = top)
        }

    private fun full(context: Context, top: Int, height: Int = -1) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        if (height > 0) dp(context, height) else ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(context, top) }

    private fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()

    /** Every letter as a small tile coloured by how well the child knows it. */
    private class LetterMap(context: Context, private val tiles: List<Pair<String, Mastery>>) : View(context) {
        private val density = resources.displayMetrics.density
        private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        private val rect = RectF()
        private val gap = 5 * density
        private val target = 40 * density

        private fun columns(width: Int) = max(1, ((width + gap) / (target + gap)).toInt())

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            val cols = columns(width)
            val side = (width - gap * (cols - 1)) / cols
            val rows = (tiles.size + cols - 1) / cols
            setMeasuredDimension(width, (rows * side + (rows - 1) * gap).toInt().coerceAtLeast(0))
        }

        override fun onDraw(canvas: Canvas) {
            val cols = columns(width)
            val side = (width - gap * (cols - 1)) / cols
            tiles.forEachIndexed { i, (symbol, mastery) ->
                val x = (i % cols) * (side + gap)
                val y = (i / cols) * (side + gap)
                rect.set(x, y, x + side, y + side)
                fill.color = when (mastery) {
                    Mastery.KNOWN -> Palette.GRASS
                    Mastery.LEARNING -> Palette.SUN
                    Mastery.PRACTICE -> Palette.TOMATO
                    Mastery.NOT_YET -> 0xFFE3E9F7.toInt()
                }
                canvas.drawRoundRect(rect, side * 0.22f, side * 0.22f, fill)
                label.color = when (mastery) {
                    Mastery.LEARNING -> Palette.INK
                    Mastery.NOT_YET -> 0xFF9AAAD0.toInt()
                    else -> Palette.WHITE
                }
                label.textSize = side * 0.5f
                val width = label.measureText(symbol)
                if (width > side * 0.86f) label.textSize *= side * 0.86f / width
                val metrics = label.fontMetrics
                canvas.drawText(symbol, rect.centerX(), rect.centerY() - (metrics.ascent + metrics.descent) / 2f, label)
            }
        }
    }

    /** Minutes played on each of the last seven days, with weekday names. */
    private class WeekChart(context: Context, private val minutes: List<Int>) : View(context) {
        private val density = resources.displayMetrics.density
        private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
        private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = MUTED
            textSize = 12 * density
        }
        private val value = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            color = Palette.INK
            textSize = 12 * density
            typeface = Typeface.DEFAULT_BOLD
        }
        private val rect = RectF()

        override fun onDraw(canvas: Canvas) {
            val names = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val today = todayNumber()
            val slot = width / minutes.size.toFloat()
            val barWidth = min(slot * 0.55f, 28 * density)
            val bottom = height - 18 * density
            val top = 16 * density
            val highest = max(1, minutes.maxOrNull() ?: 1)
            minutes.forEachIndexed { i, m ->
                val cx = slot * (i + 0.5f)
                val h = (bottom - top) * m / highest
                rect.set(cx - barWidth / 2f, bottom - max(h, 3 * density), cx + barWidth / 2f, bottom)
                bar.color = if (i == minutes.lastIndex) Palette.OCEAN else 0xFF9DC0FF.toInt()
                canvas.drawRoundRect(rect, 6 * density, 6 * density, bar)
                if (m > 0) canvas.drawText("$m", cx, rect.top - 4 * density, value)
                // Day numbers count from 1 Jan 1970, a Thursday.
                val day = today - (minutes.lastIndex - i)
                val weekday = ((day + 4) % 7).toInt()
                canvas.drawText(if (i == minutes.lastIndex) "Today" else names[weekday], cx, height - 4 * density, label)
            }
        }
    }
}
