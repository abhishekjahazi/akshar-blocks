package com.aksharblocks.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.FileOutputStream

/**
 * Prints a tracing worksheet through Android's print screen, which can also save it as a PDF.
 * Like [BackupActivity], it exists because the parent area closes as soon as anything covers
 * it; this screen has no layout of its own and goes back to the parent area when done.
 */
class WorksheetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return
        val sheet = Worksheet.entries.firstOrNull { it.name == intent.getStringExtra(EXTRA_SHEET) }
        val printer = getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (sheet == null || printer == null) {
            Toast.makeText(this, "Printing is not available on this phone", Toast.LENGTH_LONG).show()
            backToParentArea()
            return
        }
        printer.print(sheet.label, Adapter(sheet), PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
    }

    /** Hands the worksheet to the print screen. The pages are always A4, scaled by the printer. */
    private inner class Adapter(private val sheet: Worksheet) : PrintDocumentAdapter() {

        override fun onLayout(
            oldAttributes: PrintAttributes?, newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?, callback: LayoutResultCallback, extras: Bundle?,
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder(sheet.fileName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(sheet.pages.size)
                .build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }

        override fun onWrite(
            pages: Array<out PageRange>?, destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?, callback: WriteResultCallback,
        ) {
            try {
                FileOutputStream(destination.fileDescriptor).use { WorksheetPdf.write(sheet, it) }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            }
        }

        override fun onFinish() = backToParentArea()
    }

    private fun backToParentArea() {
        if (isFinishing) return
        startActivity(Intent(this, ParentActivity::class.java))
        finish()
    }

    companion object {
        private const val EXTRA_SHEET = "sheet"

        fun print(context: Context, sheet: Worksheet) =
            context.startActivity(Intent(context, WorksheetActivity::class.java).putExtra(EXTRA_SHEET, sheet.name))
    }
}
