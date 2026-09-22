package com.airplayr.app.core.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingThemeTest {
    @Test
    fun exposesExactlySevenThemesInSettingsOrder() {
        assertEquals(
            listOf(
                NowPlayingTheme.DEFAULT,
                NowPlayingTheme.COLOURED,
                NowPlayingTheme.GRADIENT,
                NowPlayingTheme.BLUR,
                NowPlayingTheme.GRADIENT_BLUR,
                NowPlayingTheme.GLOW,
                NowPlayingTheme.SHIMMER,
            ),
            NowPlayingTheme.values,
        )
    }

    @Test
    fun labelsAreStableAndUnknownValuesFallBackToDefault() {
        assertEquals("Default", NowPlayingTheme.label(NowPlayingTheme.DEFAULT))
        assertEquals("Coloured", NowPlayingTheme.label(NowPlayingTheme.COLOURED))
        assertEquals("Gradient", NowPlayingTheme.label(NowPlayingTheme.GRADIENT))
        assertEquals("Blur", NowPlayingTheme.label(NowPlayingTheme.BLUR))
        assertEquals("Gradient blur", NowPlayingTheme.label(NowPlayingTheme.GRADIENT_BLUR))
        assertEquals("Glow", NowPlayingTheme.label(NowPlayingTheme.GLOW))
        assertEquals("Shimmer", NowPlayingTheme.label(NowPlayingTheme.SHIMMER))
        assertTrue(NowPlayingTheme.label("invalid") == "Default")
    }
}
