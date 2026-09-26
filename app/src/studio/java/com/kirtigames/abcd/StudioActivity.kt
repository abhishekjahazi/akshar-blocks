package com.kirtigames.abcd

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File

/**
 * Voice Studio (studio build only): record the voice script one line at a time. Each take is
 * saved under the exact file name the app looks for, so nothing needs renaming. Takes play in
 * this build's games straight away.
 */
class StudioActivity : AppCompatActivity() {

    private val scripts: Map<String, List<VoiceScript.Line>> by lazy { VoiceScript.lines() }
    private var language = "en"
    private var index = 0

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private val recording get() = recorder != null

    private lateinit var progressText: TextView
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var sectionText: TextView
    private lateinit var lineText: TextView
    private lateinit var fileText: TextView
    private lateinit var statusText: TextView
    private lateinit var recordButton: MaterialButton
    private lateinit var playButton: MaterialButton

    private val prefs by lazy { getSharedPreferences("studio", MODE_PRIVATE) }
    private val lines: List<VoiceScript.Line> get() = scripts.getValue(language)
    private val line: VoiceScript.Line get() = lines[index]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        language = prefs.getString("language", "en") ?: "en"
        index = prefs.getInt("index_$language", 0).coerceIn(0, lines.lastIndex)
        setContentView(buildScreen())
        show()
        if (!hasMic()) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), MIC_REQUEST)
    }

    // --- Screen -----------------------------------------------------------------

    private fun buildScreen(): ScrollView {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(24))
        }
        content.addView(text("Voice Studio", 26f, bold = true))

        content.addView(MaterialButtonToggleGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
            layoutParams = full(top = 12)
            val english = outlined("English")
            val hindi = outlined("हिंदी")
            addView(english, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            addView(hindi, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            check(if (language == "en") english.id else hindi.id)
            addOnButtonCheckedListener { _, id, checked ->
                if (!checked) return@addOnButtonCheckedListener
                switchLanguage(if (id == english.id) "en" else "hi")
            }
        })

        progressText = text("", 15f, muted = true, top = 12)
        content.addView(progressText)
        progressBar = LinearProgressIndicator(this).apply {
            trackThickness = dp(8)
            trackCornerRadius = dp(4)
            layoutParams = full(top = 6)
        }
        content.addView(progressBar)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
        sectionText = text("", 14f, muted = true).apply { gravity = Gravity.CENTER }
        lineText = text("", 38f, bold = true, top = 12).apply { gravity = Gravity.CENTER }
        fileText = text("", 13f, muted = true, top = 12).apply {
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
        }
        statusText = text("", 16f, bold = true, top = 8).apply { gravity = Gravity.CENTER }
        card.addView(sectionText)
        card.addView(lineText)
        card.addView(fileText)
        card.addView(statusText)
        content.addView(MaterialCardView(this).apply {
            radius = dp(20).toFloat()
            cardElevation = 0f
            minimumHeight = dp(220)
            addView(card)
            layoutParams = full(top = 16)
        })

        recordButton = MaterialButton(this).apply {
            textSize = 22f
            minimumHeight = dp(84)
            setOnClickListener { if (recording) stopRecording() else startRecording() }
            layoutParams = full(top = 20)
        }
        content.addView(recordButton)

        playButton = outlined("▶  Listen").apply { setOnClickListener { play() } }
        content.addView(row(
            outlined("◀  Back").apply { setOnClickListener { go(index - 1) } },
            playButton,
            outlined("Next  ▶").apply { setOnClickListener { go(index + 1) } },
        ))
        content.addView(row(
            outlined("Next not recorded").apply { setOnClickListener { nextMissing() } },
            outlined("Export to Downloads").apply { setOnClickListener { export() } },
        ))
        content.addView(text(
            "Tip: tap Record, wait a moment, say the line warmly, then tap Stop. It plays back so you can " +
                "check it; record again to replace it. Open \"ABCD (studio)\" to hear your takes in the games.",
            14f, muted = true, top = 16,
        ))

        return ScrollView(this).apply {
            setBackgroundColor(Palette.ICE)
            addView(content)
            ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
                view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
                insets
            }
        }
    }

    /** Refreshes everything on screen for the current line. */
    private fun show() {
        val done = lines.count { takeFile(it).exists() }
        progressText.text = "$done of ${lines.size} recorded  ·  line ${index + 1}"
        progressBar.max = lines.size
        progressBar.progress = done
        sectionText.text = line.section
        lineText.text = line.text
        fileText.text = "${line.file}.m4a"
        val recorded = takeFile(line).exists()
        statusText.text = when {
            recording -> "● Recording…"
            recorded -> "✓ Recorded"
            else -> "Not recorded yet"
        }
        statusText.setTextColor(if (recording) Palette.TOMATO else if (recorded) Palette.GRASS else 0xFF55638C.toInt())
        recordButton.text = if (recording) "■  Stop" else if (recorded) "●  Record again" else "●  Record"
        recordButton.setBackgroundColor(if (recording) Palette.INK else Palette.TOMATO)
        playButton.isEnabled = recorded && !recording
    }

    // --- Recording --------------------------------------------------------------

    private fun takeFolder() = File(getExternalFilesDir("voice"), language).apply { mkdirs() }
    private fun takeFile(l: VoiceScript.Line) = File(takeFolder(), "${l.file}.m4a")
    private fun tempFile() = File(takeFolder(), "recording.tmp")

    private fun startRecording() {
        if (!hasMic()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), MIC_REQUEST)
            return
        }
        stopPlayback()
        try {
            recorder = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioChannels(1)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96_000)
                // Record to a temporary file so a failed retake never destroys a good one.
                setOutputFile(tempFile().absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            toast("Couldn't start recording: ${e.message}")
        }
        show()
    }

    private fun stopRecording() {
        val active = recorder ?: return
        recorder = null
        val saved = try {
            active.stop()
            true
        } catch (e: RuntimeException) {
            false // Stopped too quickly: nothing usable was recorded.
        } finally {
            active.release()
        }
        if (saved) {
            val target = takeFile(line)
            target.delete()
            tempFile().renameTo(target)
            Voice.reload()
            show()
            play()
        } else {
            tempFile().delete()
            toast("Too short – try again")
            show()
        }
    }

    private fun play() {
        val file = takeFile(line)
        if (!file.exists()) return
        stopPlayback()
        player = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener { stopPlayback() }
            prepare()
            start()
        }
    }

    private fun stopPlayback() {
        player?.release()
        player = null
    }

    // --- Moving around -------------------------------------------------------------

    private fun go(i: Int) {
        if (recording) stopRecording()
        index = i.coerceIn(0, lines.lastIndex)
        prefs.edit().putInt("index_$language", index).apply()
        stopPlayback()
        show()
    }

    private fun nextMissing() {
        val next = (1..lines.size).map { (index + it) % lines.size }.firstOrNull { !takeFile(lines[it]).exists() }
        if (next == null) toast("Everything is recorded!") else go(next)
    }

    private fun switchLanguage(to: String) {
        if (to == language) return
        if (recording) stopRecording()
        language = to
        prefs.edit().putString("language", language).apply()
        index = prefs.getInt("index_$language", 0).coerceIn(0, lines.lastIndex)
        stopPlayback()
        show()
    }

    /** Copies every take to Downloads/ABCD voice/<language>/ so it can be copied to a computer. */
    private fun export() {
        if (Build.VERSION.SDK_INT < 29) {
            toast("On this Android version, copy Android/data/$packageName/files/voice over USB instead.")
            return
        }
        var count = 0
        for (lang in listOf("en", "hi")) {
            val folder = "${android.os.Environment.DIRECTORY_DOWNLOADS}/ABCD voice/$lang/"
            val files = File(getExternalFilesDir("voice"), lang).listFiles { f -> f.extension == "m4a" }.orEmpty()
            for (file in files) {
                // Replace an earlier export of the same take.
                contentResolver.delete(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                    arrayOf(file.name, folder),
                )
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                }
                val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: continue
                contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                count++
            }
        }
        toast("Exported $count recordings to Downloads/ABCD voice")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (requestCode == MIC_REQUEST && !hasMic()) toast("The studio needs the microphone to record.")
    }

    override fun onPause() {
        super.onPause()
        if (recording) stopRecording()
        stopPlayback()
    }

    // --- Small helpers ------------------------------------------------------------

    private fun hasMic() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun text(value: String, sizeSp: Float, bold: Boolean = false, muted: Boolean = false, top: Int = 0) =
        TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(if (muted) 0xFF55638C.toInt() else Palette.INK)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            layoutParams = full(top = top)
        }

    private fun outlined(label: String) =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            id = android.view.View.generateViewId()
            text = label
        }

    private fun row(vararg buttons: MaterialButton) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = full(top = 10)
        buttons.forEachIndexed { i, button ->
            addView(button, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                if (i > 0) marginStart = dp(8)
            })
        }
    }

    private fun full(top: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(top) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    private companion object {
        const val MIC_REQUEST = 7
    }
}
