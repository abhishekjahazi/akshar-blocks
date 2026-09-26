package com.aksharblocks.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Screens: Home (pick English / Hindi vowels / Hindi consonants) → that track's
 * game menu → a game. Back always goes up one level. Grown-ups reach
 * [ParentActivity] from Home through the [ParentGate].
 */
class MainActivity : AppCompatActivity() {

    private enum class Screen { HOME, CHILDREN, ALBUM, TRACK, GAME }

    private lateinit var speaker: Speaker
    private lateinit var profiles: ProfileStore
    private lateinit var player: Player

    private var screen = Screen.HOME

    /** The track whose menu or game is showing. */
    private var track: Track? = null

    private val goBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            val current = track
            if (screen == Screen.GAME && current != null) showTrack(current) else showHome()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the (hidden) system bars; GameView keeps content clear of cutouts.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        speaker = Speaker(this)
        profiles = ProfileStore(this)
        player = Player(this, profiles.current())
        onBackPressedDispatcher.addCallback(this, goBack)
        showHome()
        greet()
    }

    override fun onResume() {
        super.onResume()
        Settings(this).let {
            speaker.slow = it.slowVoice
            Sounds.enabled = it.soundEffects
        }
        // A parent may have renamed, added or removed children in the parent area.
        val current = profiles.current()
        if (current != player.profile) {
            player = Player(this, current)
            if (screen == Screen.HOME || screen == Screen.CHILDREN) showHome()
        }
    }

    private fun greet() {
        val name = player.profile.name
        val hindiName = name.any { it in 'ऀ'..'ॿ' }
        speaker.say(if (hindiName) CommonWords.helloHindi(name) else CommonWords.hello(name), if (hindiName) Hindi.locale else English.locale)
    }

    private fun showHome() {
        screen = Screen.HOME
        track = null
        goBack.isEnabled = false
        speaker.stop()
        setContentView(HomeView(this, speaker, player).apply {
            onPick = ::showTrack
            onParent = { ParentGate.show(this@MainActivity) { startActivity(Intent(this@MainActivity, ParentActivity::class.java)) } }
            onChild = ::showChildren
            onAlbum = ::showAlbum
        })
    }

    private fun showAlbum() {
        screen = Screen.ALBUM
        goBack.isEnabled = true
        setContentView(AlbumView(this, speaker, player).apply { onHome = ::showHome })
    }

    /** "Who is playing?" when there are several children; with one, just say hello. */
    private fun showChildren() {
        val all = profiles.all()
        if (all.size < 2) {
            greet()
            return
        }
        screen = Screen.CHILDREN
        goBack.isEnabled = true
        setContentView(ProfilePickerView(this, speaker, player, all).apply {
            onPick = { profile ->
                profiles.currentId = profile.id
                player = Player(this@MainActivity, profile)
                showHome()
                greet()
            }
            onHome = ::showHome
        })
    }

    private fun showTrack(track: Track) {
        screen = Screen.TRACK
        this.track = track
        goBack.isEnabled = true
        speaker.stop()
        setContentView(TrackMenuView(this, speaker, player, track).apply {
            onPick = { mode -> startGame(track, mode) }
            onHome = ::showHome
        })
    }

    private fun startGame(track: Track, mode: GameMode) {
        val view = when (mode) {
            GameMode.LEARN ->
                if (track == Track.BARAKHADI) BarakhadiView(this, speaker, player, track)
                else LearnView(this, speaker, player, track)
            GameMode.BUILD -> MatraGameView(this, speaker, player, track)
            GameMode.COUNT -> CountView(this, speaker, player, track)
            GameMode.TRACE -> TraceView(this, speaker, player, track)
            GameMode.FIND -> FindView(this, speaker, player, track)
            GameMode.BALLOONS -> BalloonView(this, speaker, player, track)
            GameMode.MATCH -> MatchView(this, speaker, player, track)
        }
        view.onHome = { showTrack(track) }
        screen = Screen.GAME
        goBack.isEnabled = true
        setContentView(view)
    }

    override fun onPause() {
        super.onPause()
        speaker.stop()
    }

    override fun onDestroy() {
        speaker.shutdown()
        if (isFinishing) Sounds.release()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }

    private fun hideSystemUi() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
