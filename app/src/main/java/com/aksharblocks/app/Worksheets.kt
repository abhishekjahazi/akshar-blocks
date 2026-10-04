package com.aksharblocks.app

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/** A tracing worksheet a parent can print: one letter per row, written once, then to trace. */
enum class Worksheet(val label: String, val title: String, val track: Track, val count: Int) {
    CAPITALS("English A–Z", "Trace the letters A to Z", Track.ENGLISH, 26),
    SMALL("English a–z", "Trace the small letters a to z", Track.LOWER, 26),
    SWAR("हिंदी स्वर (अ–अः)", "स्वर लिखो (अ से अः)", Track.SWAR, 13),
    VYANJAN("हिंदी व्यंजन (क–ज्ञ)", "व्यंजन लिखो (क से ज्ञ)", Track.VYANJAN, 36),
    NUMBERS("Numbers 1–20", "Trace the numbers 1 to 20", Track.NUMBERS, 20),
    GINTI("हिंदी गिनती (१–२०)", "गिनती लिखो (१ से २०)", Track.GINTI, 20);

    val symbols: List<String> get() = track.letters.take(count).map { it.symbol }

    /** The symbols on each page. */
    val pages: List<List<String>> get() = symbols.chunked(ROWS_PER_PAGE)

    val fileName get() = "akshar-blocks-${name.lowercase()}.pdf"

    companion object {
        const val ROWS_PER_PAGE = 7
    }
}

/**
 * Draws worksheets as an A4 PDF: a title and a line for the child's name, then rows with the
 * letter in dark ink followed by light dotted copies to trace, on handwriting lines.
 */
object WorksheetPdf {

    // A4 in PDF points (1/72 inch).
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f
    private const val HEADER_H = 92f
    private const val PER_ROW = 5

    fun write(sheet: Worksheet, out: OutputStream) {
        val pdf = PdfDocument()
        try {
            sheet.pages.forEachIndexed { index, symbols ->
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, index + 1).create())
                drawPage(page.canvas, sheet, symbols, index + 1, sheet.pages.size)
                pdf.finishPage(page)
            }
            pdf.writeTo(out)
        } finally {
            pdf.close()
        }
    }

    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Palette.NAVY
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF55638C.toInt()
        textSize = 11f
        typeface = Typeface.SANS_SERIF
    }
    private val dotted = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF9AA3BD.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 1.3f
        pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val faint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x14000000
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFC9D2EA.toInt()
        strokeWidth = 0.8f
    }
    private val midLine = Paint(line).apply { pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f) }

    private fun drawPage(canvas: Canvas, sheet: Worksheet, symbols: List<String>, page: Int, pages: Int) {
        val right = PAGE_W - MARGIN

        // Header: title, the child's name line, and where it's from.
        ink.textAlign = Paint.Align.LEFT
        ink.textSize = 22f
        canvas.drawText(sheet.title, MARGIN, MARGIN + 22f, ink)
        ink.textAlign = Paint.Align.CENTER
        small.textAlign = Paint.Align.LEFT
        canvas.drawText("Name / नाम:", MARGIN, MARGIN + 54f, small)
        canvas.drawLine(MARGIN + 74f, MARGIN + 56f, MARGIN + 300f, MARGIN + 56f, line)
        small.textAlign = Paint.Align.RIGHT
        canvas.drawText("Akshar Blocks  ·  $page / $pages", right, MARGIN + 54f, small)

        val top = MARGIN + HEADER_H
        val rowH = (PAGE_H - MARGIN - top) / Worksheet.ROWS_PER_PAGE
        val cellW = (right - MARGIN) / PER_ROW
        symbols.forEachIndexed { row, symbol ->
            val rowTop = top + rowH * row
            // Handwriting lines: top, dashed middle, baseline.
            val lineTop = rowTop + rowH * 0.12f
            val baseline = rowTop + rowH * 0.78f
            canvas.drawLine(MARGIN, lineTop, right, lineTop, line)
            canvas.drawLine(MARGIN, (lineTop + baseline) / 2f, right, (lineTop + baseline) / 2f, midLine)
            canvas.drawLine(MARGIN, baseline, right, baseline, line)

            // Capitals (and the Hindi top line) reach about 0.72 of the text size above the baseline.
            val size = fitSize(symbol, (baseline - lineTop) / 0.72f, cellW * 0.86f)
            for (i in 0 until PER_ROW) {
                val cx = MARGIN + cellW * (i + 0.5f)
                if (i == 0) {
                    ink.textSize = size
                    canvas.drawText(symbol, cx, baseline, ink)
                } else {
                    faint.textSize = size
                    dotted.textSize = size
                    canvas.drawText(symbol, cx, baseline, faint)
                    canvas.drawText(symbol, cx, baseline, dotted)
                }
            }
        }
    }

    /** [size], made smaller when [symbol] would be wider than [maxWidth] (like "20" or "ज्ञ"). */
    private fun fitSize(symbol: String, size: Float, maxWidth: Float): Float {
        ink.textSize = size
        val width = ink.measureText(symbol)
        return if (width > maxWidth) size * maxWidth / width else size
    }
}
