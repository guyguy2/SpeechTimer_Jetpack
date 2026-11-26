package com.happypuppy.toastmasterstimer.ui.screens

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSliderState
import com.happypuppy.toastmasterstimer.util.CustomSpeechRange
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class CustomRangeSliderSyncTest {

    private fun newState(start: Int, end: Int): RangeSliderState = RangeSliderState(
        startValue = start.toFloat(),
        endValue = end.toFloat(),
        steps = CustomSpeechRange.MAX_END_MINUTES - CustomSpeechRange.MIN_START_MINUTES - 1,
        trackRange = CustomSpeechRange.MIN_START_MINUTES.toFloat()..CustomSpeechRange.MAX_END_MINUTES.toFloat()
    )

    @Test
    fun syncRangeSliderState_upwardJumpPastCurrentEnd_updatesBothWithoutClamping() {
        val state = newState(8, 10)

        syncRangeSliderState(state, 12, 15)
        assertEquals(12f, state.startValue)
        assertEquals(15f, state.endValue)

        syncRangeSliderState(state, 20, 30)
        assertEquals(20f, state.startValue)
        assertEquals(30f, state.endValue)

        syncRangeSliderState(state, 59, 60)
        assertEquals(59f, state.startValue)
        assertEquals(60f, state.endValue)
    }

    @Test
    fun syncRangeSliderState_downwardJumpBelowCurrentStart_updatesBothWithoutClamping() {
        val state = newState(20, 30)

        syncRangeSliderState(state, 12, 15)
        assertEquals(12f, state.startValue)
        assertEquals(15f, state.endValue)

        syncRangeSliderState(state, 3, 5)
        assertEquals(3f, state.startValue)
        assertEquals(5f, state.endValue)

        syncRangeSliderState(state, 1, 2)
        assertEquals(1f, state.startValue)
        assertEquals(2f, state.endValue)
    }

    @Test
    fun syncRangeSliderState_overlappingRanges_updatesBothAccurately() {
        val state = newState(8, 10)

        syncRangeSliderState(state, 9, 15)
        assertEquals(9f, state.startValue)
        assertEquals(15f, state.endValue)

        syncRangeSliderState(state, 5, 11)
        assertEquals(5f, state.startValue)
        assertEquals(11f, state.endValue)
    }
}
