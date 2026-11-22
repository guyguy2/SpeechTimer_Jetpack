package com.happypuppy.toastmasterstimer.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class DateConvertersTest {

    private val converters = DateConverters()

    @Test
    fun roundTrip_epochZero() {
        val date = converters.fromTimestamp(0L)
        assertEquals(Date(0L), date)
        assertEquals(0L, converters.dateToTimestamp(date))
    }

    @Test
    fun roundTrip_positiveTimestamp() {
        val timestamp = 1_763_848_608_123L
        val date = converters.fromTimestamp(timestamp)
        assertEquals(Date(timestamp), date)
        assertEquals(timestamp, converters.dateToTimestamp(date))
    }

    @Test
    fun nullValues_returnNullInBothDirections() {
        assertNull(converters.fromTimestamp(null))
        assertNull(converters.dateToTimestamp(null))
    }
}
