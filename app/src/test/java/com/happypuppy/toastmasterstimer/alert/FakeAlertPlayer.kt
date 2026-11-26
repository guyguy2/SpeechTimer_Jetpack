package com.happypuppy.toastmasterstimer.alert

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAlertPlayer : AlertPlayer {

    private val _isSoundPlaying = MutableStateFlow(false)
    override val isSoundPlaying: StateFlow<Boolean> = _isSoundPlaying.asStateFlow()

    /** Every call in order, e.g. "vibrate:RED_PHASE", "playSound", "stopSound". */
    val calls = mutableListOf<String>()

    override fun vibrate(vibration: AlertVibration) {
        calls += "vibrate:$vibration"
    }

    override fun playSound() {
        calls += "playSound"
        _isSoundPlaying.value = true
    }

    override fun stopSound() {
        calls += "stopSound"
        _isSoundPlaying.value = false
    }
}
