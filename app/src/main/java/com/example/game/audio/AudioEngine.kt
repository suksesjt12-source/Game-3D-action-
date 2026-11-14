package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.game.model.GameSettings
import com.example.game.model.RegionId
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SfxType {
    FOOTSTEP,
    SWORD_SWING,
    HEAVY_ATTACK,
    HIT_IMPACT,
    PARRY_CLANG,
    DODGE,
    JUMP,
    LAND,
    ARROW_SHOT,
    ENEMY_ATTACK,
    ENEMY_DEATH,
    BOSS_INTRO,
    CHEST_OPEN,
    ITEM_PICKUP,
    UI_CLICK,
    QUEST_COMPLETE,
    LEVEL_UP
}

enum class MusicTheme {
    MAIN_MENU,
    VILLAGE,
    EXPLORATION,
    COMBAT,
    BOSS
}

/**
 * Pure Kotlin procedural synthesizer & mixer outputting live fantasy music,
 * biome ambience (wind/birds/water/forge/dungeon), and crisp zero-asset SFX via AudioTrack.
 */
class AudioEngine {
    private val sampleRate = 22050

    @Volatile private var running = false
    @Volatile private var paused = false
    @Volatile private var currentMusic = MusicTheme.MAIN_MENU
    @Volatile private var currentRegion = RegionId.DAWNREACH_VILLAGE
    @Volatile private var isRaining = false
    @Volatile private var inDungeon = false

    @Volatile private var masterVol = 0.85f
    @Volatile private var musicVol = 0.70f
    @Volatile private var sfxVol = 0.90f
    @Volatile private var ambientVol = 0.75f

    private data class ActiveSfx(
        val samples: ShortArray,
        var cursor: Int = 0,
        val gain: Float = 1.0f
    )

    private val sfxLock = Any()
    private val activeSfx = mutableListOf<ActiveSfx>()
    private val sfxBank = mutableMapOf<SfxType, ShortArray>()

    private var mixerThread: Thread? = null

    fun start() {
        if (running) return
        running = true
        buildSfxBank()
        mixerThread = thread(isDaemon = true, name = "AetherfallAudioMixer") {
            runAudioLoop()
        }
    }

    fun stop() {
        running = false
        mixerThread?.join(300)
        mixerThread = null
    }

    fun setPaused(isPaused: Boolean) {
        paused = isPaused
    }

    fun applySettings(settings: GameSettings) {
        masterVol = settings.masterVolume
        musicVol = settings.musicVolume
        sfxVol = settings.sfxVolume
        ambientVol = settings.ambientVolume
    }

    fun updateEnvironment(
        theme: MusicTheme,
        region: RegionId,
        raining: Boolean,
        dungeon: Boolean
    ) {
        currentMusic = theme
        currentRegion = region
        isRaining = raining
        inDungeon = dungeon
    }

    fun playSfx(type: SfxType, gain: Float = 1.0f) {
        if (paused || masterVol <= 0.01f || sfxVol <= 0.01f) return
        val buf = sfxBank[type] ?: return
        synchronized(sfxLock) {
            if (activeSfx.size > 14) activeSfx.removeAt(0)
            activeSfx.add(ActiveSfx(buf, 0, gain))
        }
    }

    private fun buildSfxBank() {
        sfxBank[SfxType.FOOTSTEP] = synthNoiseBurst(0.08f, 340f, 0.25f)
        sfxBank[SfxType.SWORD_SWING] = synthSweep(0.16f, 680f, 190f, 0.55f, noiseMix = 0.45f)
        sfxBank[SfxType.HEAVY_ATTACK] = synthSweep(0.30f, 360f, 85f, 0.78f, noiseMix = 0.5f)
        sfxBank[SfxType.HIT_IMPACT] = synthSweep(0.14f, 210f, 55f, 0.85f, noiseMix = 0.6f)
        sfxBank[SfxType.PARRY_CLANG] = synthBellChord(0.36f, floatArrayOf(1180f, 1760f, 2410f), 0.75f)
        sfxBank[SfxType.DODGE] = synthSweep(0.18f, 440f, 180f, 0.40f, noiseMix = 0.7f)
        sfxBank[SfxType.JUMP] = synthSweep(0.14f, 180f, 410f, 0.40f, noiseMix = 0.1f)
        sfxBank[SfxType.LAND] = synthSweep(0.12f, 150f, 55f, 0.50f, noiseMix = 0.5f)
        sfxBank[SfxType.ARROW_SHOT] = synthSweep(0.13f, 920f, 310f, 0.50f, noiseMix = 0.35f)
        sfxBank[SfxType.ENEMY_ATTACK] = synthSweep(0.20f, 290f, 110f, 0.55f, noiseMix = 0.4f)
        sfxBank[SfxType.ENEMY_DEATH] = synthSweep(0.38f, 240f, 60f, 0.65f, noiseMix = 0.3f)
        sfxBank[SfxType.BOSS_INTRO] = synthBellChord(0.95f, floatArrayOf(110f, 165f, 220f, 330f), 0.85f)
        sfxBank[SfxType.CHEST_OPEN] = synthArpeggio(0.55f, floatArrayOf(440f, 554.37f, 659.25f, 880f), 0.65f)
        sfxBank[SfxType.ITEM_PICKUP] = synthArpeggio(0.26f, floatArrayOf(587.33f, 880f), 0.55f)
        sfxBank[SfxType.UI_CLICK] = synthSweep(0.05f, 740f, 520f, 0.32f, noiseMix = 0f)
        sfxBank[SfxType.QUEST_COMPLETE] = synthArpeggio(0.75f, floatArrayOf(392f, 493.88f, 587.33f, 783.99f), 0.72f)
        sfxBank[SfxType.LEVEL_UP] = synthArpeggio(0.85f, floatArrayOf(440f, 554.37f, 659.25f, 880f, 1108.73f), 0.78f)
    }

    private fun synthSweep(
        duration: Float,
        startHz: Float,
        endHz: Float,
        amp: Float,
        noiseMix: Float
    ): ShortArray {
        val count = (duration * sampleRate).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        val rng = Random(42)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val freq = startHz + (endHz - startHz) * t
            phase += 2.0 * PI * freq / sampleRate
            val env = exp(-t * 4.5f)
            val tone = sin(phase).toFloat()
            val noise = (rng.nextFloat() * 2f - 1f)
            val s = ((tone * (1f - noiseMix) + noise * noiseMix) * env * amp).coerceIn(-1f, 1f)
            out[i] = (s * 32767f).toInt().toShort()
        }
        return out
    }

    private fun synthNoiseBurst(duration: Float, filterHz: Float, amp: Float): ShortArray {
        val count = (duration * sampleRate).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val rng = Random(13)
        var lp = 0f
        val alpha = (filterHz / sampleRate).coerceIn(0.02f, 0.5f)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val env = exp(-t * 8f)
            val raw = rng.nextFloat() * 2f - 1f
            lp += alpha * (raw - lp)
            out[i] = ((lp * env * amp).coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
        return out
    }

    private fun synthBellChord(duration: Float, freqs: FloatArray, amp: Float): ShortArray {
        val count = (duration * sampleRate).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / count
            val env = exp(-t * 3.2f)
            var sum = 0f
            for (f in freqs) {
                sum += sin(2.0 * PI * f * (i.toDouble() / sampleRate)).toFloat()
            }
            val s = ((sum / freqs.size) * env * amp).coerceIn(-1f, 1f)
            out[i] = (s * 32767f).toInt().toShort()
        }
        return out
    }

    private fun synthArpeggio(duration: Float, notes: FloatArray, amp: Float): ShortArray {
        val count = (duration * sampleRate).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val seg = (count / notes.size).coerceAtLeast(1)
        for (i in 0 until count) {
            val idx = (i / seg).coerceAtMost(notes.lastIndex)
            val localT = (i % seg).toFloat() / seg
            val env = exp(-localT * 3.8f)
            val f = notes[idx]
            val tone = sin(2.0 * PI * f * (i.toDouble() / sampleRate)).toFloat() +
                0.35f * sin(4.0 * PI * f * (i.toDouble() / sampleRate)).toFloat()
            val s = (tone * 0.7f * env * amp).coerceIn(-1f, 1f)
            out[i] = (s * 32767f).toInt().toShort()
        }
        return out
    }

    private fun runAudioLoop() {
        val bufferFrames = 1024
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(bufferFrames * 2)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (_: Exception) {
            return
        }

        try {
            track.play()
        } catch (_: Exception) {
            track.release()
            return
        }

        val mixBuf = ShortArray(bufferFrames)
        val rng = Random(99)
        var musicTime = 0.0
        var lpWind = 0f
        var lpWater = 0f

        // Pentatonic / Dorian motif scales per theme
        val menuNotes = floatArrayOf(220f, 261.63f, 293.66f, 329.63f, 392f, 440f)
        val villageNotes = floatArrayOf(261.63f, 293.66f, 329.63f, 392f, 440f, 523.25f)
        val exploreNotes = floatArrayOf(196f, 220f, 261.63f, 293.66f, 329.63f, 392f)
        val combatNotes = floatArrayOf(146.83f, 174.61f, 196f, 220f, 261.63f, 293.66f)
        val bossNotes = floatArrayOf(110f, 130.81f, 146.83f, 155.56f, 196f, 220f)

        while (running) {
            if (paused) {
                try {
                    Thread.sleep(50)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            val mVol = masterVol * musicVol * 0.22f
            val aVol = masterVol * ambientVol * 0.16f
            val sVol = masterVol * sfxVol

            val scale = when (currentMusic) {
                MusicTheme.MAIN_MENU -> menuNotes
                MusicTheme.VILLAGE -> villageNotes
                MusicTheme.EXPLORATION -> exploreNotes
                MusicTheme.COMBAT -> combatNotes
                MusicTheme.BOSS -> bossNotes
            }
            val beatRate = when (currentMusic) {
                MusicTheme.BOSS -> 2.8
                MusicTheme.COMBAT -> 2.3
                MusicTheme.VILLAGE -> 1.15
                else -> 0.85
            }

            for (i in 0 until bufferFrames) {
                musicTime += 1.0 / sampleRate
                val beatPos = musicTime * beatRate
                val step = beatPos.toInt()
                val frac = (beatPos - step).toFloat()

                // Procedural music pad + melody pluck
                var musicSample = 0f
                if (mVol > 0.002f) {
                    val rootFreq = scale[(step / 4) % 3] * 0.5f
                    val fifthFreq = rootFreq * 1.5f
                    val pad = 0.45f * sin(2.0 * PI * rootFreq * musicTime).toFloat() +
                        0.30f * sin(2.0 * PI * fifthFreq * musicTime).toFloat()

                    val noteIdx = ((step * 3 + (step / 4)) % scale.size)
                    val melodyFreq = scale[noteIdx]
                    val pluckEnv = exp(-frac * if (currentMusic == MusicTheme.BOSS) 4.0f else 3.0f)
                    val melody = sin(2.0 * PI * melodyFreq * musicTime).toFloat() * pluckEnv * 0.55f

                    val drumEnv = if (currentMusic == MusicTheme.COMBAT || currentMusic == MusicTheme.BOSS) {
                        exp(-frac * 10f) * sin(2.0 * PI * (68f - frac * 30f) * musicTime).toFloat() * 0.65f
                    } else 0f

                    musicSample = (pad + melody + drumEnv) * mVol
                }

                // Biome & weather ambient layer
                var ambSample = 0f
                if (aVol > 0.002f) {
                    val white = rng.nextFloat() * 2f - 1f
                    lpWind += 0.025f * (white - lpWind)
                    lpWater += 0.11f * (white - lpWater)

                    val regionAmb = when {
                        inDungeon -> sin(2.0 * PI * 58.0 * musicTime).toFloat() * 0.25f + lpWind * 0.5f
                        currentRegion == RegionId.SILVERLAKE_VALLEY -> lpWater * 0.85f
                        currentRegion == RegionId.ASHEN_HIGHLANDS -> lpWind * 1.1f + sin(2.0 * PI * 64.0 * musicTime).toFloat() * 0.2f
                        currentRegion == RegionId.GREENVEIL_FOREST -> {
                            // Subtle bird chirp pulses in forest
                            val chirpGate = if ((musicTime % 4.2) < 0.12) sin(2.0 * PI * 1950.0 * musicTime).toFloat() * 0.12f else 0f
                            lpWind * 0.65f + chirpGate
                        }
                        else -> lpWind * 0.45f
                    }
                    val rainNoise = if (isRaining) lpWater * 0.9f else 0f
                    ambSample = (regionAmb + rainNoise) * aVol
                }

                // Active SFX mixing
                var sfxSample = 0f
                synchronized(sfxLock) {
                    var idx = activeSfx.size - 1
                    while (idx >= 0) {
                        val sfx = activeSfx[idx]
                        if (sfx.cursor < sfx.samples.size) {
                            sfxSample += (sfx.samples[sfx.cursor].toFloat() / 32767f) * sfx.gain * sVol
                            sfx.cursor++
                        } else {
                            activeSfx.removeAt(idx)
                        }
                        idx--
                    }
                }

                val total = (musicSample + ambSample + sfxSample).coerceIn(-0.98f, 0.98f)
                mixBuf[i] = (total * 32767f).toInt().toShort()
            }

            try {
                track.write(mixBuf, 0, bufferFrames)
            } catch (_: Exception) {
                break
            }
        }

        try {
            track.stop()
            track.release()
        } catch (_: Exception) {
        }
    }
}
