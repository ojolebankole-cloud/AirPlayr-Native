package com.airplayr.app.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.airplayr.app.R

/** Exact font assets shipped by the reference app app. */
object AirPlayrFonts {
    val OpenSans = FontFamily(
        Font(R.font.open_sans_regular, FontWeight.Normal),
        Font(R.font.open_sans_medium, FontWeight.Medium),
        Font(R.font.open_sans_bold, FontWeight.Bold),
    )

    val OpenSansRegular = FontFamily(Font(R.font.open_sans_regular, FontWeight.Normal))
    val OpenSansMedium = FontFamily(Font(R.font.open_sans_medium, FontWeight.Medium))
    val OpenSansBold = FontFamily(Font(R.font.open_sans_bold, FontWeight.Bold))

    val SmoochSans = FontFamily(
        Font(R.font.smooch_sans_semibold, FontWeight.SemiBold),
    )

    val Oswald = FontFamily(
        Font(R.font.oswald_medium, FontWeight.Medium),
    )

    val DancingScript = FontFamily(
        Font(R.font.dancing_script_regular, FontWeight.Normal),
    )

    val ShadowsIntoLight = FontFamily(
        Font(R.font.shadows_into_light_regular, FontWeight.Normal),
    )

    val CaacupeOne = FontFamily(
        Font(R.font.caacupe_one_regular, FontWeight.Normal),
    )
}
