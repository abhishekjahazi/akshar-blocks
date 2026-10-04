package com.aksharblocks.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.AdView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Screens: Home (pick English / Hindi vowels / Hindi consonants) → that track's
 * game menu → a game. Back always goes up one level. Grown-ups reach
 * [ParentActivity] from Home through the [ParentGate].
 */
class MainActivity : AppCompatActivity() {

    private enum class Screen { HOME, CHILDREN, ALBUM, TRACK, GAME, PATH, RHYMES, CERTIFICATE, REST, WORLD, MATHS, SHOP }

    private lateinit var speaker: Speaker
    private lateinit var profiles: ProfileStore
    private lateinit var player: Player

    private var screen = Screen.HOME

    /** The track whose menu or game is showing. */
    private var track: Track? = null

    /** The step of today's path being played, or null outside the path. */
    private var pathStep: Int? = null

    /** The banner under Home, made once and moved back in each time Home is shown. */
    private var banner: AdView? = null

    /** True while a rhyme is playing, so Back returns to the list of rhymes. */
    private var inRhyme = false

    /** True while a maths game is playing, so Back returns to the maths menu. */
    private var inMaths = false

    /** Counts up for each game shown, so a late "step finished" timer can tell it's stale. */
    private var gameToken = 0

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
            when {
                screen == Screen.GAME && inRhyme -> showRhymes()
                screen == Screen.GAME && inMaths -> showMaths()
                screen == Screen.GAME && pathStep != null -> showPath()
                screen == Screen.GAME && current != null -> showTrack(current)
                screen == Screen.TRACK && current != null && current.isPictures -> showWorld()
                screen == Screen.ALBUM -> showShop()
                else -> showHome()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the (hidden) system bars; GameView keeps content clear of cutouts.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        speaker = Speaker(this)
        speaker.onSpeaking = Music::duck
        profiles = ProfileStore(this)
        player = Player(this, profiles.current())
        onBackPressedDispatcher.addCallback(this, goBack)
        showHome()
        if (screen == Screen.HOME) greet()
    }

    override fun onResume() {
        super.onResume()
        Settings(this).let {
            speaker.slow = it.slowVoice
            Sounds.enabled = it.soundEffects
            Music.enabled = it.music
        }
        lastTick = SystemClock.elapsedRealtime()
        ticker.post(tick)
        banner?.resume()
        if (screen != Screen.REST) Music.play()
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
            player.addPlaySeconds(ms / 1000)
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
        pathStep = null
        // Back simply leaves the app from here.
        goBack.isEnabled = false
        speaker.stop()
        Music.pause()
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
        if (certificateFirst(::showHome)) return
        // Back from the rest screen (a new day or a parent's extra time): music again.
        if (screen == Screen.REST) Music.play()
        screen = Screen.HOME
        track = null
        pathStep = null
        goBack.isEnabled = false
        speaker.stop()
        val home = HomeView(this, speaker, player).apply {
            onPick = ::showTrack
            onParent = { ParentGate.show(this@MainActivity) { startActivity(Intent(this@MainActivity, ParentActivity::class.java)) } }
            onChild = ::showChildren
            onAlbum = ::showShop
            onPath = { showPath() }
            onName = ::showName
            onRhymes = ::showRhymes
            onWorld = ::showWorld
            onMaths = ::showMaths
            pathDone = player.pathDone
        }
        setContentView(withBanner(home))
    }

    /** Home with the ad banner under it (only Home has an ad; games never do). */
    private fun withBanner(home: HomeView): android.view.View {
        val ad = banner ?: Ads.banner(this)?.also { made ->
            banner = made
            // The first ad holds up the screen for a moment: ask once Home's welcome is over.
            ticker.postDelayed({ Ads.load(made) }, FIRST_AD_DELAY_MS)
        } ?: return home
        (ad.parent as? ViewGroup)?.removeView(ad)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Palette.SKY)
            addView(home, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(ad)
        }
    }

    /** The sticker album, reached from the shop. */
    private fun showAlbum() {
        screen = Screen.ALBUM
        goBack.isEnabled = true
        speaker.stop()
        setContentView(AlbumView(this, speaker, player).apply { onHome = ::showShop })
    }

    /** The reward shop: dress up the child's animal with stars. */
    private fun showShop() {
        screen = Screen.SHOP
        goBack.isEnabled = true
        speaker.stop()
        setContentView(ShopView(this, speaker, player).apply {
            onHome = ::showHome
            onAlbum = ::showAlbum
        })
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
        if (certificateFirst { showTrack(track) }) return
        screen = Screen.TRACK
        this.track = track
        pathStep = null
        goBack.isEnabled = true
        speaker.stop()
        setContentView(TrackMenuView(this, speaker, player, track).apply {
            onPick = { mode -> startGame(track, mode) }
            onHome = if (track.isPictures) ::showWorld else ::showHome
        })
    }

    /** Colors, shapes, animals, fruits, body and family, as big picture cards. */
    private fun showWorld() {
        if (certificateFirst(::showWorld)) return
        screen = Screen.WORLD
        track = null
        pathStep = null
        goBack.isEnabled = true
        speaker.stop()
        val cards = Track.entries.filter { it.isPictures }.map { t ->
            MenuCard(t.label, t.subtitle, t.color, t.letters.first().symbol) { showTrack(t) }
        }
        setContentView(CardMenuView(this, speaker, player, CommonWords.WORLD, Palette.MINT, cards, CommonWords.WORLD_ASK).apply {
            onHome = ::showHome
        })
    }

    /** The maths games. */
    private fun showMaths() {
        screen = Screen.MATHS
        track = null
        pathStep = null
        inMaths = false
        goBack.isEnabled = true
        speaker.stop()
        val cards = MathGame.entries.map { game ->
            MenuCard(game.label, game.hindi, game.color, game.emoji) { startMaths(game) }
        }
        setContentView(CardMenuView(this, speaker, player, CommonWords.MATHS, Palette.PEACH, cards, MathWords.MENU_ASK).apply {
            onHome = ::showHome
        })
    }

    private fun startMaths(game: MathGame) {
        speaker.stop()
        val view = MathView(this, speaker, player, game)
        view.onHome = ::showMaths
        showGame(view)
        inMaths = true
    }

    private fun makeGame(track: Track, mode: GameMode, start: Int = 0): GameView = when (mode) {
        GameMode.LEARN ->
            if (track == Track.BARAKHADI) BarakhadiView(this, speaker, player, track)
            else LearnView(this, speaker, player, track, start)
        GameMode.BUILD -> MatraGameView(this, speaker, player, track)
        GameMode.COUNT -> CountView(this, speaker, player, track)
        GameMode.MEMORY -> MemoryView(this, speaker, player, track)
        GameMode.WORDS -> WordsView(this, speaker, player, track)
        GameMode.TRACE -> TraceView(this, speaker, player, track, start)
        GameMode.FIND -> FindView(this, speaker, player, track)
        GameMode.SOUNDS -> FindView(this, speaker, player, track, bySound = true)
        GameMode.BALLOONS -> BalloonView(this, speaker, player, track)
        GameMode.MATCH -> MatchView(this, speaker, player, track)
    }

    private fun startGame(track: Track, mode: GameMode) {
        val view = makeGame(track, mode)
        view.onHome = { showTrack(track) }
        showGame(view)
    }

    private fun showGame(view: GameView) {
        gameToken++
        inRhyme = false
        inMaths = false
        screen = Screen.GAME
        goBack.isEnabled = true
        setContentView(view)
    }

    private fun showRhymes() {
        screen = Screen.RHYMES
        track = null
        pathStep = null
        inRhyme = false
        goBack.isEnabled = true
        speaker.stop()
        setContentView(RhymesView(this, speaker, player).apply {
            onPick = ::showRhyme
            onHome = ::showHome
        })
    }

    private fun showRhyme(rhyme: Rhyme) {
        speaker.stop()
        val view = RhymeView(this, speaker, player, rhyme)
        view.onHome = ::showRhymes
        showGame(view)
        inRhyme = true
    }

    /**
     * When the child has just learned enough of a section, their certificate comes first, then
     * [then]. Returns false when there is no new certificate.
     */
    private fun certificateFirst(then: () -> Unit): Boolean {
        val earned = player.newCertificate() ?: return false
        screen = Screen.CERTIFICATE
        track = null
        pathStep = null
        goBack.isEnabled = true
        speaker.stop()
        setContentView(CertificateView(this, speaker, player, earned).apply { onDone = then })
        return true
    }

    /** Trace the playing child's own name. */
    private fun showName() {
        val letters = NameLetters.of(player.profile.name)
        if (letters.isEmpty()) return
        val view = TraceView(this, speaker, player, Track.ENGLISH, name = letters)
        view.onHome = ::showHome
        showGame(view)
        track = null
        pathStep = null
    }

    /** Today's games: the list of steps, with the next one ready to tap. */
    private fun showPath(justFinished: Boolean = false) {
        if (certificateFirst { showPath(justFinished) }) return
        screen = Screen.PATH
        track = null
        pathStep = null
        goBack.isEnabled = true
        speaker.stop()
        setContentView(PathView(this, speaker, player, player.todaysPath(), player.pathDone, justFinished).apply {
            onPick = ::startPathStep
            onHome = ::showHome
        })
    }

    /** Plays one step of today's path; enough stars finish it and lead back to the path. */
    private fun startPathStep(index: Int) {
        val step = player.todaysPath()[index]
        // Learn and Trace pick up at the first letter this child doesn't know yet.
        val view = makeGame(step.track, step.mode, start = player.firstUnknown(step.track))
        var points = 0
        view.onHome = { showPath() }
        showGame(view)
        track = step.track
        pathStep = index
        val token = gameToken
        view.onPoint = {
            points++
            if (points == step.goal) {
                player.finishPathStep(index)
                // Let the game's own praise play first.
                ticker.postDelayed({ if (gameToken == token && screen == Screen.GAME) showPath(justFinished = true) }, STEP_DONE_DELAY_MS)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        countPlayTime()
        lastTick = 0L
        ticker.removeCallbacks(tick)
        banner?.pause()
        speaker.stop()
        Music.pause()
    }

    override fun onDestroy() {
        banner?.destroy()
        speaker.shutdown()
        if (isFinishing) {
            Sounds.release()
            Music.release()
        }
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
        const val STEP_DONE_DELAY_MS = 2_800L
        const val FIRST_AD_DELAY_MS = 4_000L
    }
}
