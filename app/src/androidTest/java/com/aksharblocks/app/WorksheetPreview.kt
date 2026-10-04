package com.aksharblocks.app

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Writes every worksheet as a PDF and the first page of each as a PNG, to look at:
 *   adb pull /sdcard/Android/data/com.aksharblocks.app.debug/files/worksheets
 */
@RunWith(AndroidJUnit4::class)
class WorksheetPreview {

    @Test
    fun makeWorksheets() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.getExternalFilesDir(null), "worksheets").apply { mkdirs() }
        for (sheet in Worksheet.entries) {
            val pdf = File(folder, sheet.fileName)
            pdf.outputStream().use { WorksheetPdf.write(sheet, it) }
            PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                assertEquals(sheet.pages.size, renderer.pageCount)
                renderer.openPage(0).use { page ->
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    File(folder, sheet.fileName.replace(".pdf", ".png")).outputStream().use {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                }
            }
        }
    }
}
