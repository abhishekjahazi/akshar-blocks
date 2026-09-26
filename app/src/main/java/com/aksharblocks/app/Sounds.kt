package com.aksharblocks.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class Sound { TAP, POP, RIGHT, WRONG, FANFARE }

/**
 * Short game sounds, synthesised when the app starts (no audio files, nothing to license).
 * Kept quiet and gentle: the voice stays the main thing children hear.
 */
object Sounds {

    /** Parents can turn sounds off in the parent area. */
    @Volatile
    var enabled = true

    private val tracks = HashMap<Sound, AudioTrack>()

    fun play(sound: Sound) {
        if (!enabled) return
        try {
            val track = tracks.getOrPut(sound) { build(Synth.samples(sound)) }
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) track.stop()
            track.playbackHeadPosition = 0
            track.play()
        } catch (e: RuntimeException) {
            // A missing or busy audio device must never stop the game.
            Log.w("Sounds", "Could not play $sound", e)
        }
    }

    fun release() {
        tracks.values.forEach { it.release() }
        tracks.clear()
    }

    private fun build(samples: ShortArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(Synth.RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }
}

/** Plain-Kotlin sound shapes, so they can be checked in tests. */
object Synth {

    const val RATE = 22050

    fun samples(sound: Sound): ShortArray = when (sound) {
        // A soft click.
        Sound.TAP -> tone(listOf(Note(880f, 0.04f)), volume = 0.18f, decay = 60f)
        // A quick falling blip, like a bubble popping.
        Sound.POP -> sweep(from = 900f, to = 300f, seconds = 0.09f, volume = 0.3f)
        // Two rising bell notes.
        Sound.RIGHT -> tone(listOf(Note(1046.5f, 0.12f), Note(1318.5f, 0.28f)), volume = 0.3f, decay = 9f)
        // A low, gentle "boop" – never a harsh buzzer.
        Sound.WRONG -> sweep(from = 330f, to = 220f, seconds = 0.22f, volume = 0.22f)
        // C E G C, for a new sticker.
        Sound.FANFARE -> tone(
            listOf(Note(523.3f, 0.11f), Note(659.3f, 0.11f), Note(784f, 0.11f), Note(1046.5f, 0.4f)),
            volume = 0.3f, decay = 5f,
        )
    }

    data class Note(val hz: Float, val seconds: Float)

    /** Notes one after another; each rings like a small bell (fast attack, smooth fade). */
    private fun tone(notes: List<Note>, volume: Float, decay: Float): ShortArray {
        val out = ArrayList<Short>()
        for (note in notes) {
            val n = (note.seconds * RATE).toInt()
            for (i in 0 until n) {
                val t = i / RATE.toFloat()
                val attack = minOf(1f, t / 0.005f)
                val envelope = attack * exp(-decay * t)
                // A little of the octave above makes it bell-like.
                val wave = sin(2 * PI * note.hz * t) * 0.8 + sin(4 * PI * note.hz * t) * 0.2
                out += (wave * envelope * volume * Short.MAX_VALUE).toInt().toShort()
            }
        }
        return out.toShortArray()
    }

    /** A pitch glide with a smooth fade. */
    private fun sweep(from: Float, to: Float, seconds: Float, volume: Float): ShortArray {
        val n = (seconds * RATE).toInt()
        var phase = 0.0
        return ShortArray(n) { i ->
            val progress = i / n.toFloat()
            val hz = from + (to - from) * progress
            phase += 2 * PI * hz / RATE
            val envelope = minOf(1f, i / (0.004f * RATE)) * (1f - progress)
            (sin(phase) * envelope * volume * Short.MAX_VALUE).toInt().toShort()
        }
    }
}
