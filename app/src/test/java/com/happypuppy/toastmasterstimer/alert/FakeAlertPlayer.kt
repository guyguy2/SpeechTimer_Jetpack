package com.happypuppy.toastmasterstimer.alert

class FakeAlertPlayer : AlertPlayer {

    /** Every call in order, e.g. "vibrate:RED_PHASE", "playSound", "stopSound". */
    val calls = mutableListOf<String>()

    override fun vibrate(vibration: AlertVibration) {
        calls += "vibrate:$vibration"
    }

    override fun playSound() {
        calls += "playSound"
    }

    override fun stopSound() {
        calls += "stopSound"
    }
}
