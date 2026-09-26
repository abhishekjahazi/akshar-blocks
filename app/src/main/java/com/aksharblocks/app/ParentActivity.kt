package com.aksharblocks.app

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputFilter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.switchmaterial.SwitchMaterial

/** Grown-ups only (reached through [ParentGate]): children, their progress, and settings. */
class ParentActivity : AppCompatActivity() {

    private lateinit var profiles: ProfileStore
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        profiles = ProfileStore(this)

        content = MaxWidthColumn(this, dp(640)).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(32))
        }
        // A readable column on tablets: at most 640 dp wide, centered.
        val column = android.widget.FrameLayout(this).apply {
            addView(content, android.widget.FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL,
            ))
        }
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Palette.ICE)
            addView(column)
        }
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        setContentView(scroll)
        render()
    }

    /** Rebuilds the whole screen; it's short, so this keeps every section in sync. */
    private fun render() {
        content.removeAllViews()

        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(MaterialButton(this@ParentActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "Back to games"
                setOnClickListener { finish() }
            })
        })
        content.addView(text("Parent area", 30f, bold = true, top = 12))
        content.addView(text("Progress and settings for each child. Everything stays on this phone.", 16f, muted = true, top = 4))

        section("Children")
        val all = profiles.all()
        for (profile in all) content.addView(childCard(profile, canRemove = all.size > 1))
        if (profiles.canAdd) {
            content.addView(MaterialButton(this).apply {
                text = "Add a child"
                setOnClickListener { editChild(null) }
                layoutParams = spaced(top = 8)
            })
        }

        section("Voice speed")
        content.addView(voiceSpeed())

        section("Sounds")
        content.addView(SwitchMaterial(this).apply {
            text = "Game sounds (dings, pops and cheers)"
            textSize = 16f
            isChecked = Settings(this@ParentActivity).soundEffects
            setOnCheckedChangeListener { _, on ->
                Settings(this@ParentActivity).soundEffects = on
                Sounds.enabled = on
            }
            layoutParams = spaced(top = 4)
        })

        section("Daily play time")
        content.addView(playLimit())

        section("About")
        content.addView(text(
            "Akshar Blocks ${versionName()}\nNo ads. No accounts. No internet. " +
                "Stars and progress are saved only on this phone and are deleted if the app is uninstalled.\n\n" +
                "Pictures: Noto Emoji by Google, used under the Apache License 2.0.",
            15f, muted = true, top = 4,
        ))
    }

    private fun childCard(profile: Profile, canRemove: Boolean): View {
        val player = Player(this, profile)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
        }
        body.addView(text("${profile.avatar}  ${profile.name}", 22f, bold = true))
        val streak = player.streak
        val summary = buildString {
            append("⭐ ${player.stars} stars  ·  ${Stickers.unlocked(player.stars)} of ${Stickers.all.size} stickers")
            if (streak >= 2) append("  ·  🔥 $streak days in a row")
        }
        body.addView(text(summary, 16f, muted = true, top = 2))

        for (track in Track.entries) {
            val report = player.report(track)
            body.addView(text(track.label, 17f, bold = true, top = 14))
            if (!report.played) {
                body.addView(text("Not played yet", 15f, muted = true, top = 2))
                continue
            }
            val unit = when {
                track == Track.BARAKHADI -> "syllables"
                track.isNumbers -> "numbers"
                else -> "letters"
            }
            body.addView(text("${report.known} of ${report.total} $unit known", 15f, top = 2))
            body.addView(LinearProgressIndicator(this).apply {
                max = report.total
                progress = report.known
                trackCornerRadius = dp(4)
                trackThickness = dp(8)
                setIndicatorColor(track.color)
                layoutParams = spaced(top = 6)
            })
            if (report.practice.isNotEmpty()) {
                body.addView(text("Needs practice:  ${report.practice.joinToString("  ")}", 15f, top = 6))
                body.addView(text("Find it and Pictures bring these up more often.", 13f, muted = true, top = 0))
            }
            if (report.mixUps.isNotEmpty()) {
                body.addView(text("Often mixes up:  ${report.mixUps.joinToString(",  ") { "${it.first} and ${it.second}" }}", 15f, top = 2))
            }
        }

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = spaced(top = 12)
        }
        actions.addView(textButton("Edit") { editChild(profile) })
        actions.addView(textButton("Reset progress") {
            confirm("Reset ${profile.name}'s progress?", "Stars and letter results will be cleared.") {
                player.reset()
                render()
            }
        })
        if (canRemove) {
            actions.addView(textButton("Remove") {
                confirm("Remove ${profile.name}?", "Their stars and progress will be deleted.") {
                    profiles.remove(profile.id)
                    render()
                }
            })
        }
        body.addView(actions)

        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = 0f
            strokeWidth = dp(1)
            strokeColor = 0xFFD6E0F5.toInt()
            addView(body)
            layoutParams = spaced(top = 10)
        }
    }

    /** Add a child (profile == null) or change a child's name and picture. */
    private fun editChild(profile: Profile?) {
        var avatar = profile?.avatar ?: ProfileStore.AVATARS.first { a -> profiles.all().none { it.avatar == a } }
        val name = EditText(this).apply {
            hint = "Child's name"
            setText(profile?.name.orEmpty())
            filters = arrayOf(InputFilter.LengthFilter(ProfileStore.MAX_NAME_LENGTH))
            setSingleLine()
        }
        // Two rows of four so the buttons stay large enough for emoji.
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        ProfileStore.AVATARS.chunked(4).forEach { chunk ->
            val group = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = spaced(top = 6)
            }
            chunk.forEach { emoji ->
                group.addView(MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    text = emoji
                    textSize = 22f
                    isCheckable = true
                    isChecked = emoji == avatar
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = dp(6)
                    }
                    setOnClickListener {
                        avatar = emoji
                        // Only one picture can be chosen across both rows.
                        for (i in 0 until rows.childCount) {
                            val other = rows.getChildAt(i) as LinearLayout
                            for (j in 0 until other.childCount) {
                                val button = other.getChildAt(j) as MaterialButton
                                button.isChecked = button.text == emoji
                            }
                        }
                    }
                })
            }
            rows.addView(group)
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(8), dp(24), 0)
            addView(name)
            addView(text("Picture", 15f, muted = true, top = 12))
            addView(rows)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(if (profile == null) "Add a child" else "Edit ${profile.name}")
            .setView(form)
            .setPositiveButton("Save") { _, _ ->
                val typed = name.text.toString()
                if (profile == null) {
                    profiles.add(typed, avatar)?.let { profiles.currentId = it.id }
                } else {
                    profiles.update(profile.copy(name = typed, avatar = avatar))
                }
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun voiceSpeed(): View {
        val settings = Settings(this)
        return RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            layoutParams = spaced(top = 4)
            val normal = RadioButton(this@ParentActivity).apply {
                id = View.generateViewId()
                text = "Normal"
                textSize = 16f
            }
            val slow = RadioButton(this@ParentActivity).apply {
                id = View.generateViewId()
                text = "Slower (for the youngest children)"
                textSize = 16f
            }
            addView(normal)
            addView(slow)
            check(if (settings.slowVoice) slow.id else normal.id)
            setOnCheckedChangeListener { _, checked -> settings.slowVoice = checked == slow.id }
        }
    }

    /** Daily limit (all children together): off or 15–60 minutes, and today's play so far. */
    private fun playLimit(): View {
        val settings = Settings(this)
        val playTime = PlayTime(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = spaced(top = 4)
        }
        val minutesToday = playTime.secondsToday / 60
        column.addView(text("Played today: $minutesToday min", 15f, muted = true))
        column.addView(RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            val buttons = PlayLimit.CHOICES.associateWith { minutes ->
                RadioButton(this@ParentActivity).apply {
                    id = View.generateViewId()
                    text = if (minutes == 0) "No limit" else "$minutes minutes a day"
                    textSize = 16f
                }
            }
            buttons.values.forEach { addView(it) }
            buttons[settings.dailyLimitMinutes]?.let { check(it.id) }
            setOnCheckedChangeListener { _, checked ->
                settings.dailyLimitMinutes = buttons.entries.first { it.value.id == checked }.key
            }
        })
        column.addView(text(
            "When time is up, the app shows a calm \"Time to rest\" screen until tomorrow. " +
                "Grown-ups can add 10 minutes from the 🔒 on that screen.",
            14f, muted = true, top = 4,
        ))
        return column
    }

    private fun confirm(title: String, message: String, onYes: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onYes() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun section(title: String) {
        content.addView(text(title, 20f, bold = true, top = 28))
    }

    private fun text(value: String, sizeSp: Float, bold: Boolean = false, muted: Boolean = false, top: Int = 0) =
        TextView(this).apply {
            text = value
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            setTextColor(if (muted) 0xFF55638C.toInt() else Palette.INK)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(0f, 1.15f)
            layoutParams = spaced(top = top)
        }

    private fun textButton(label: String, onClick: () -> Unit) =
        MaterialButton(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun spaced(top: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(top) }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun versionName(): String =
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
}

/** Settings a parent can change. */
class Settings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var slowVoice: Boolean
        get() = prefs.getBoolean("slow_voice", false)
        set(value) = prefs.edit().putBoolean("slow_voice", value).apply()

    var soundEffects: Boolean
        get() = prefs.getBoolean("sound_effects", true)
        set(value) = prefs.edit().putBoolean("sound_effects", value).apply()

    /** 0 means no limit. */
    var dailyLimitMinutes: Int
        get() = prefs.getInt("daily_limit_minutes", 0)
        set(value) = prefs.edit().putInt("daily_limit_minutes", value).apply()
}

/** A vertical column that is never wider than [maxWidth] px, so text stays readable on tablets. */
private class MaxWidthColumn(context: Context, private val maxWidth: Int) : LinearLayout(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val available = MeasureSpec.getSize(widthMeasureSpec)
        val spec = if (available > maxWidth) MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.EXACTLY) else widthMeasureSpec
        super.onMeasure(spec, heightMeasureSpec)
    }
}
