package com.kirtigames.abcd

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat

/**
 * Screens: Home (pick English / Hindi vowels / Hindi consonants) → that track's
 * game menu → a game. Back always goes up one level.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var speaker: Speaker
    private lateinit var stars: StarBank

    /** The track whose menu or game is showing; null on the home screen. */
    private var track: Track? = null
    private var inGame = false

    private val goBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            val current = track
            if (inGame && current != null) showTrack(current) else showHome()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the (hidden) system bars; GameView keeps content clear of cutouts.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        speaker = Speaker(this)
        stars = StarBank(this)
        onBackPressedDispatcher.addCallback(this, goBack)
        showHome()
        speaker.say("Let's play!")
    }

    private fun showHome() {
        track = null
        inGame = false
        goBack.isEnabled = false
        speaker.stop()
        setContentView(HomeView(this, speaker, stars).apply { onPick = ::showTrack })
    }

    private fun showTrack(track: Track) {
        this.track = track
        inGame = false
        goBack.isEnabled = true
        speaker.stop()
        setContentView(TrackMenuView(this, speaker, stars, track).apply {
            onPick = { mode -> startGame(track, mode) }
            onInstallVoice = ::installVoice
            onHome = ::showHome
        })
    }

    private fun startGame(track: Track, mode: GameMode) {
        val view = when (mode) {
            GameMode.LEARN -> LearnView(this, speaker, stars, track)
            GameMode.FIND -> FindView(this, speaker, stars, track)
            GameMode.BALLOONS -> BalloonView(this, speaker, stars, track)
            GameMode.MATCH -> MatchView(this, speaker, stars, track)
        }
        view.onHome = { showTrack(track) }
        inGame = true
        goBack.isEnabled = true
        setContentView(view)
    }

    /** Opens the voice download screen, or the text-to-speech settings if that isn't available. */
    private fun installVoice() {
        try {
            startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
        } catch (_: ActivityNotFoundException) {
            try {
                startActivity(Intent("com.android.settings.TTS_SETTINGS"))
            } catch (_: ActivityNotFoundException) {
                // Nothing to open on this phone; the banner stays as a hint.
            }
        }
    }

    override fun onPause() {
        super.onPause()
        speaker.stop()
    }

    override fun onDestroy() {
        speaker.shutdown()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        window.insetsController?.apply {
            hide(WindowInsets.Type.systemBars())
            systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
