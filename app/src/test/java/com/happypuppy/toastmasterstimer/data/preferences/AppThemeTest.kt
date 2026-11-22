package com.happypuppy.toastmasterstimer.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeTest {

    @Test
    fun fromString_mapsAllKnownValues() {
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromString("system_default"))
        assertEquals(AppTheme.LIGHT, AppTheme.fromString("light"))
        assertEquals(AppTheme.DARK, AppTheme.fromString("dark"))
    }

    @Test
    fun fromString_unknownOrBlankFallsBackToSystemDefault() {
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromString(""))
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromString("DARK"))
        assertEquals(AppTheme.SYSTEM_DEFAULT, AppTheme.fromString("sepia"))
    }
}
