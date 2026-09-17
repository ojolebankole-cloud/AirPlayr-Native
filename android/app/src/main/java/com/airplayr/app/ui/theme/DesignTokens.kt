package com.airplayr.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AirPlayrSpacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 28.dp
    val xxxl: Dp = 40.dp
}

object AirPlayrRadii {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 18.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val pill: Dp = 999.dp
}

object AirPlayrElevation {
    // reference Android shadow contract: elevation 4 (soft), elevation 8 (medium).
    val none: Dp = 0.dp
    val soft: Dp = 4.dp
    val medium: Dp = 8.dp
}


object AirPlayrMotion {
    const val sheetOpenMs = 320
    const val sheetCloseMs = 220
    const val fadeMs = 180
}
