package com.aksharblocks.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves or restores a backup through Android's file picker, then goes back to the parent area.
 *
 * The parent area itself can't wait for the picker: it closes as soon as anything covers it
 * (noHistory, so a child never finds it open). This screen has no layout of its own; it is
 * only reachable from the parent area, after the grown-ups question.
 */
class BackupActivity : AppCompatActivity() {

    private val save = registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) finishWith(write(uri))
        else backToParentArea()
    }

    private val open = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) finishWith(read(uri))
        else backToParentArea()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Only on first start: after a rotation the picker is already open.
        if (savedInstanceState != null) return
        when (intent.getStringExtra(EXTRA_ACTION)) {
            ACTION_SAVE -> save.launch("akshar-blocks-backup-${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())}.txt")
            ACTION_RESTORE -> open.launch(arrayOf("text/plain", "application/octet-stream", "*/*"))
            else -> finish()
        }
    }

    private fun write(uri: Uri): String = try {
        contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(Backup.export(this)) }
        "Progress saved"
    } catch (e: Exception) {
        "Could not save the backup"
    }

    private fun read(uri: Uri): String = try {
        val text = contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
        val children = Backup.restore(this, text)
        "Progress restored ($children ${if (children == 1) "child" else "children"})"
    } catch (e: IllegalArgumentException) {
        "That file is not an Akshar Blocks backup"
    } catch (e: Exception) {
        "Could not read the backup"
    }

    private fun finishWith(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        backToParentArea()
    }

    private fun backToParentArea() {
        startActivity(Intent(this, ParentActivity::class.java))
        finish()
    }

    companion object {
        private const val EXTRA_ACTION = "action"
        private const val ACTION_SAVE = "save"
        private const val ACTION_RESTORE = "restore"

        fun save(context: Context) =
            context.startActivity(Intent(context, BackupActivity::class.java).putExtra(EXTRA_ACTION, ACTION_SAVE))

        fun restore(context: Context) =
            context.startActivity(Intent(context, BackupActivity::class.java).putExtra(EXTRA_ACTION, ACTION_RESTORE))
    }
}
