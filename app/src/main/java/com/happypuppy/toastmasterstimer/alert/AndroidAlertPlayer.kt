package com.happypuppy.toastmasterstimer.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plays timer cues as alarm-class output: vibration uses the ALARM usage so it is not dropped as
 * touch feedback when the app is not in front, and sound uses ALARM attributes with transient
 * audio focus so other audio ducks while the cue plays. Cues still only work while the process
 * is alive (foreground-only by design).
 */
class AndroidAlertPlayer(private val context: Context) : AlertPlayer {

    private val _isSoundPlaying = MutableStateFlow(false)
    override val isSoundPlaying: StateFlow<Boolean> = _isSoundPlaying.asStateFlow()

    private var activeRingtone: Ringtone? = null
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                handler.post { stopSound() }
            }
        }
    }

    private val releaseFocusWhenDone = object : Runnable {
        override fun run() {
            if (activeRingtone?.isPlaying == true) {
                handler.postDelayed(this, FOCUS_POLL_MS)
            } else {
                activeRingtone = null
                _isSoundPlaying.value = false
                abandonFocus()
            }
        }
    }

    private val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    override fun vibrate(vibration: AlertVibration) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator ?: return
        val effect = when (vibration) {
            AlertVibration.RED_PHASE -> VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1)
            AlertVibration.PHASE_CHANGE -> VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
            AlertVibration.PREVIEW -> VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrateAsAlarm(vibrator, effect)
    }

    private fun vibrateAsAlarm(vibrator: Vibrator, effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(
                effect,
                VibrationAttributes.Builder().setUsage(VibrationAttributes.USAGE_ALARM).build()
            )
        } else {
            // minSdk 31: VibrationAttributes arrives in API 33; AudioAttributes carries the usage before that.
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, alarmAudioAttributes)
        }
    }

    override fun playSound() {
        try {
            stopSound()
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notification)
            if (ringtone == null) {
                _isSoundPlaying.value = false
                return
            }
            ringtone.audioAttributes = alarmAudioAttributes
            if (!requestFocus()) {
                _isSoundPlaying.value = false
                return
            }
            activeRingtone = ringtone
            ringtone.play()
            _isSoundPlaying.value = true
            handler.postDelayed(releaseFocusWhenDone, FOCUS_POLL_MS)
        } catch (_: Exception) {
            activeRingtone = null
            _isSoundPlaying.value = false
            abandonFocus()
        }
    }

    override fun stopSound() {
        handler.removeCallbacks(releaseFocusWhenDone)
        try {
            activeRingtone?.stop()
        } catch (_: Exception) {
            // Ignore stop failures on already-released ringtones
        }
        activeRingtone = null
        _isSoundPlaying.value = false
        abandonFocus()
    }

    private fun requestFocus(): Boolean {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return false
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(alarmAudioAttributes)
            .setOnAudioFocusChangeListener(audioFocusChangeListener, handler)
            .build()
        val result = audioManager.requestAudioFocus(request)
        return if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            focusRequest = request
            true
        } else {
            focusRequest = null
            false
        }
    }

    private fun abandonFocus() {
        val request = focusRequest ?: return
        focusRequest = null
        context.getSystemService(AudioManager::class.java)?.abandonAudioFocusRequest(request)
    }

    private companion object {
        const val FOCUS_POLL_MS = 250L
    }
}
