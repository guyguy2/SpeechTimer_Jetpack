package com.happypuppy.toastmasterstimer.alert

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.VibratorManager

class AndroidAlertPlayer(private val context: Context) : AlertPlayer {

    private var activeRingtone: Ringtone? = null

    override fun vibrate(vibration: AlertVibration) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        when (vibration) {
            AlertVibration.RED_PHASE -> vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 300, 100, 300), -1)
            )
            AlertVibration.PHASE_CHANGE -> vibrator?.vibrate(
                VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    override fun playSound() {
        try {
            activeRingtone?.stop()
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notification)
            activeRingtone = ringtone
            ringtone?.play()
        } catch (_: Exception) {
            // Ignore devices without a default notification tone
        }
    }

    override fun stopSound() {
        activeRingtone?.stop()
        activeRingtone = null
    }
}
