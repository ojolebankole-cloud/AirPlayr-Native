package com.airplayr.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Height currently occupied by the app-level floating bottom controls. */
val LocalAirPlayrBottomOverlayHeight = staticCompositionLocalOf { 0.dp }

/** Existing screen breathing room plus the measured floating-control height. */
@Composable
fun airPlayrBottomContentPadding(base: Dp = 24.dp): Dp =
    base + LocalAirPlayrBottomOverlayHeight.current

/** Bottom inset for floating play controls so they clear the measured mini-player when present. */
@Composable
fun airPlayrFloatingPlayBottomPadding(base: Dp = 24.dp): Dp =
    base + LocalAirPlayrBottomOverlayHeight.current
