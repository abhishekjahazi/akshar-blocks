package com.aksharblocks.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Screens: Home (pick English / Hindi vowels / Hindi consonants) → that track's
 * game menu → a game. Back always goes up one level. Grown-ups reach
 * [ParentActivity] from Home through the [ParentGate].
 */
class MainActivity : AppCompatActivity() {

    private enum class Screen { HOME, CHILDREN, ALBUM, TRACK, GAME, REST }

    private lateinit var speaker: Speaker
    private lateinit var profiles: ProfileStore
    private lateinit var player: Player

    private var screen = Screen.HOME

    /** The track whose menu or game is showing. */
    private var track: Track? = null

    // Daily play-time limit: time is counted while the app is on screen, checked every few seconds.
    private val playTime by lazy { PlayTime(this) }
    private val ticker = Handler(Looper.getMainLooper())
    private var lastTick = 0L
    private var carryMs = 0L
    private val tick = object : Runnable {
        override fun run() {
            countPlayTime()
            checkPlayLimit()
            ticker.postDelayed(this, TICK_MS)
        }
    }

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
        lastTick = SystemClock.elapsedRealtime()
        ticker.post(tick)
        // A parent may have renamed, added or removed children in the parent area.
        val current = profiles.current()
        if (current != player.profile) {
            player = Player(this, current)
            if (screen == Screen.HOME || screen == Screen.CHILDREN) showHome()
        }
    }

    private fun countPlayTime() {
        val now = SystemClock.elapsedRealtime()
        if (lastTick > 0) {
            val ms = now - lastTick + carryMs
            playTime.add(ms / 1000)
            carryMs = ms % 1000
        }
        lastTick = now
    }

    private fun checkPlayLimit() {
        val over = PlayLimit.isOver(Settings(this).dailyLimitMinutes, playTime.secondsToday, playTime.extraMinutesToday)
        if (over && screen != Screen.REST) showRest()
        // A new day (or a parent's extra time) lifts the rest screen.
        if (!over && screen == Screen.REST) showHome()
    }

    private fun showRest() {
        screen = Screen.REST
        track = null
        // Back simply leaves the app from here.
        goBack.isEnabled = false
        speaker.stop()
        setContentView(RestView(this, speaker, player).apply { onParent = ::restGate })
    }

    /** A grown-up can give a little more time or open the parent area. */
    private fun restGate() {
        ParentGate.show(this) {
            MaterialAlertDialogBuilder(this)
                .setTitle("Play time is over for today")
                .setPositiveButton("10 more minutes") { _, _ ->
                    playTime.addExtraMinutes(10)
                    showHome()
                }
                .setNeutralButton("Parent area") { _, _ -> startActivity(Intent(this, ParentActivity::class.java)) }
                .setNegativeButton("Cancel", null)
                .show()
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
            GameMode.MEMORY -> MemoryView(this, speaker, player, track)
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
        countPlayTime()
        lastTick = 0L
        ticker.removeCallbacks(tick)
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

    private companion object {
        const val TICK_MS = 5_000L
    }
}
