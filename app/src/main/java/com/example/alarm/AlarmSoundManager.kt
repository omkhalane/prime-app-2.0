package com.example.alarm

import android.content.Context
import android.database.Cursor
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.model.RingtoneOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

object AlarmSoundManager {

    private var mediaPlayer: MediaPlayer? = null
    private var syntheticToneJob: Job? = null
    private var vibratorJob: Job? = null
    private var vibrator: Vibrator? = null
    private var fadeJob: Job? = null
    @Volatile private var outputGain: Float = 0.85f
    @Volatile private var activeRingtoneId: String = "zen_crystal"
    @Volatile private var muted: Boolean = false
    @Volatile var isSessionSoundEnabled: Boolean = true
    private val scope = CoroutineScope(Dispatchers.Default)

    val BUILT_IN_RINGTONES = listOf(
        RingtoneOption("zen_crystal", "Zen Crystal Chime", null, true),
        RingtoneOption("morning_breeze", "Morning Breeze", null, true),
        RingtoneOption("cosmic_sunrise", "Cosmic Sunrise", null, true),
        RingtoneOption("classic_bell", "Classic Digital Bell", null, true),
        RingtoneOption("cyber_pulse", "Cyber Pulse", null, true),
        RingtoneOption("gentle_harp", "Gentle Harp Arpeggio", null, true)
    )

    fun getAvailableRingtones(context: Context): List<RingtoneOption> {
        val list = mutableListOf<RingtoneOption>()
        list.addAll(BUILT_IN_RINGTONES)

        // Query real device system alarm ringtones
        try {
            val ringtoneManager = RingtoneManager(context).apply {
                setType(RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
            }
            val cursor: Cursor? = ringtoneManager.cursor
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = ringtoneManager.getRingtoneUri(cursor.position)
                    if (uri != null) {
                        list.add(
                            RingtoneOption(
                                id = "sys_${uri.hashCode()}",
                                title = title ?: "Device Alarm Ringtone",
                                uriString = uri.toString(),
                                isSynthetic = false
                            )
                        )
                    }
                }
            }
        } catch (_: Throwable) {
            // Fallback gracefully to built-in tones
        }

        return list
    }

    /**
     * Start continuous ringing for an active alarm (supports looping & vibration).
     */
    fun startAlarmRinging(context: Context, ringtoneId: String, ringtoneUriString: String?, vibrationEnabled: Boolean) {
        stopPlayback(context)
        activeRingtoneId = ringtoneId
        muted = false
        val prefs = context.getSharedPreferences("prime_focus_prefs", Context.MODE_PRIVATE)
        val targetGain = prefs.getInt("clock_alarm_volume", 85).coerceIn(0, 100) / 100f
        val fadeIn = prefs.getBoolean("clock_alarm_fade_in", true)
        outputGain = if (fadeIn) 0f else targetGain

        if (vibrationEnabled) {
            startVibration(context)
        }

        if (ringtoneUriString != null && ringtoneUriString.isNotBlank()) {
            try {
                val uri = Uri.parse(ringtoneUriString)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = true
                    setVolume(if (muted || fadeIn) 0f else outputGain, if (muted || fadeIn) 0f else outputGain)
                    prepare()
                    start()
                }
                if (fadeIn) {
                    fadeJob?.cancel()
                    fadeJob = scope.launch {
                        for (step in 1..12) {
                            if (!isActive || muted) break
                            val gain = targetGain * step / 12f
                            outputGain = gain
                            mediaPlayer?.setVolume(gain, gain)
                            delay(500L)
                        }
                    }
                }
                return
            } catch (_: Throwable) {
                // Release a partially initialized player before falling back.
                try { mediaPlayer?.release() } catch (_: Throwable) { }
                mediaPlayer = null
            }
        }

        startSyntheticToneLoop(ringtoneId)
        if (fadeIn) {
            fadeJob?.cancel()
            fadeJob = scope.launch {
                for (step in 1..12) {
                    if (!isActive || muted) break
                    outputGain = targetGain * step / 12f
                    delay(500L)
                }
            }
        }
    }

    /**
     * Preview a ringtone for a brief period (e.g., 5 seconds) or until stopped.
     */
    fun previewRingtone(context: Context, option: RingtoneOption, onFinished: (() -> Unit)? = null) {
        stopPlayback(context)

        if (!option.isSynthetic && option.uriString != null) {
            try {
                val uri = Uri.parse(option.uriString)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(context, uri)
                    isLooping = false
                    setOnCompletionListener {
                        stopPlayback(context)
                        onFinished?.invoke()
                    }
                    prepare()
                    start()
                }
                return
            } catch (_: Throwable) {
                // Fallback
            }
        }

        syntheticToneJob = scope.launch {
            playSyntheticToneOnce(option.id)
            delay(4000L)
            onFinished?.invoke()
        }
    }

    /** Mute/unmute alarm audio without disabling vibration or dismissing the alarm. */
    fun setMuted(context: Context, isMuted: Boolean) {
        muted = isMuted
        if (isMuted) {
            fadeJob?.cancel()
            fadeJob = null
        } else {
            outputGain = context.getSharedPreferences("prime_focus_prefs", Context.MODE_PRIVATE)
                .getInt("clock_alarm_volume", 85).coerceIn(0, 100) / 100f
        }
        mediaPlayer?.setVolume(if (isMuted) 0f else outputGain, if (isMuted) 0f else outputGain)
        if (mediaPlayer == null) {
            if (isMuted) {
                syntheticToneJob?.cancel()
                syntheticToneJob = null
            } else if (syntheticToneJob == null) {
                startSyntheticToneLoop(activeRingtoneId)
            }
        }
    }

    fun stopPlayback(context: Context) {
        try {
            fadeJob?.cancel()
            fadeJob = null
            syntheticToneJob?.cancel()
            syntheticToneJob = null

            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null

            stopVibration(context)
        } catch (_: Throwable) { }
    }

    private fun startVibration(context: Context) {
        try {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vibrator = vib

            val pattern = longArrayOf(0, 500, 250, 500, 250, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 means repeat
            } else {
                @Suppress("DEPRECATION")
                vib?.vibrate(pattern, 0)
            }
        } catch (_: Throwable) { }
    }

    private fun stopVibration(context: Context) {
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Throwable) { }
    }

    /**
     * Loops synthetic tone continuously until cancelled
     */
    private fun startSyntheticToneLoop(toneId: String) {
        syntheticToneJob?.cancel()
        syntheticToneJob = scope.launch {
            while (isActive) {
                playSyntheticToneOnce(toneId)
                delay(1200L)
            }
        }
    }

    /**
     * Synthesize pleasing harmonic tones using PCM AudioTrack (pure Kotlin synthesis).
     */
    private fun playSyntheticToneOnce(toneId: String) {
        val notes = when (toneId) {
            "morning_breeze" -> doubleArrayOf(392.0, 523.25, 659.25, 783.99, 1046.50) // G4, C5, E5, G5, C6
            "cosmic_sunrise" -> doubleArrayOf(440.0, 554.37, 659.25, 880.0, 1108.73) // A4, C#5, E5, A5, C#6
            "classic_bell" -> doubleArrayOf(880.0, 880.0, 1174.66, 880.0) // Urgent bell
            "cyber_pulse" -> doubleArrayOf(600.0, 900.0, 600.0, 900.0, 1200.0)
            "gentle_harp" -> doubleArrayOf(523.25, 587.33, 659.25, 783.99, 987.77, 1046.50)
            else -> doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // Zen Crystal (C5, E5, G5, C6)
        }

        val sampleRate = 22050
        val noteDuration = 0.22
        val numSamplesPerNote = (noteDuration * sampleRate).toInt()

        val totalSamples = numSamplesPerNote * notes.size
        val audioData = ShortArray(totalSamples)

        for (i in notes.indices) {
            val freq = notes[i]
            val startIndex = i * numSamplesPerNote
            for (j in 0 until numSamplesPerNote) {
                val t = j.toDouble() / sampleRate
                // Attack and Decay envelope
                val envelope = when {
                    j < 200 -> j / 200.0
                    else -> (1.0 - (j.toDouble() / numSamplesPerNote)).coerceAtLeast(0.0)
                }
                // Fundamental + rich 2nd harmonic
                val sample = (sin(2.0 * PI * freq * t) * 0.7 + sin(4.0 * PI * freq * t) * 0.3) * envelope
                audioData[startIndex + j] = (sample * 28000 * outputGain * if (muted) 0f else 1f).toInt().toShort()
            }
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(audioData.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(audioData, 0, audioData.size)
            audioTrack.play()
            Thread.sleep((noteDuration * notes.size * 1000).toLong() + 200)
            audioTrack.stop()
            audioTrack.release()
        } catch (_: Throwable) { }
    }

    /**
     * Play a quick pleasant chime for Pomodoro session transitions (focus complete, break finished).
     */
    fun playSessionTransitionChime() {
        if (!isSessionSoundEnabled) return
        scope.launch {
            playSyntheticToneOnce("zen_crystal")
        }
    }
}
