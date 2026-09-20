package com.airplayr.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Exact vector geometry from the reference AirPlayrBottomDock AirIcon component.
 *
 * Kept independent of Material Icons so the final native dock can use the
 * same icon geometry rather than approximations.
 */
object AirPlayrIcons {
    val Home: ImageVector
        get() = ImageVector.Builder(
            name = "AirPlayrHome",
            defaultWidth = 30.dp,
            defaultHeight = 30.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.25f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
            ) {
                moveTo(4.5f, 12.7f)
                lineTo(14f, 5f)
                lineTo(23.5f, 12.7f)
                moveTo(7.5f, 11.7f)
                verticalLineTo(20.9f)
                curveTo(7.5f, 21.8f, 8.2f, 22.5f, 9.1f, 22.5f)
                horizontalLineTo(18.9f)
                curveTo(19.8f, 22.5f, 20.5f, 21.8f, 20.5f, 20.9f)
                verticalLineTo(11.7f)
                moveTo(11.5f, 22.5f)
                verticalLineTo(17.2f)
                curveTo(11.5f, 16.4f, 12.2f, 15.7f, 13f, 15.7f)
                horizontalLineTo(15f)
                curveTo(15.8f, 15.7f, 16.5f, 16.4f, 16.5f, 17.2f)
                verticalLineTo(22.5f)
            }
        }.build()

    /** Exact Material Symbols asset: music_note_2. */
    val Music: ImageVector
        get() = ImageVector.Builder(
            name = "AirPlayrMusicNote2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(127f, 793f)
                curveTo(80f, 746f, 80f, 680f, 127f, 633f)
                curveTo(174f, 586f, 240f, 586f, 287f, 633f)
                curveTo(310f, 656f, 320f, 684f, 320f, 715f)
                verticalLineTo(373f)
                lineTo(800f, 293f)
                verticalLineTo(773f)
                curveTo(800f, 839f, 753f, 886f, 687f, 886f)
                curveTo(621f, 886f, 574f, 839f, 574f, 773f)
                curveTo(574f, 707f, 621f, 660f, 687f, 660f)
                curveTo(710f, 660f, 729f, 666f, 749f, 678f)
                verticalLineTo(513f)
                lineTo(429f, 576f)
                verticalLineTo(896f)
                curveTo(429f, 962f, 382f, 1009f, 316f, 1009f)
                curveTo(250f, 1009f, 203f, 962f, 203f, 896f)
                curveTo(203f, 830f, 250f, 783f, 316f, 783f)
                curveTo(339f, 783f, 358f, 789f, 378f, 801f)
                verticalLineTo(436f)
                lineTo(429f, 426f)
                close()
            }
        }.build()

    /** Exact Material Symbols asset: artist. */
    val Artist: ImageVector
        get() = ImageVector.Builder(
            name = "AirPlayrArtist",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(740f, 400f)
                horizontalLineTo(880f)
                verticalLineTo(480f)
                horizontalLineTo(800f)
                verticalLineTo(700f)
                curveTo(800f, 742f, 771f, 771f, 729f, 771f)
                curveTo(687f, 771f, 658f, 742f, 658f, 700f)
                curveTo(658f, 658f, 687f, 629f, 729f, 629f)
                curveTo(737f, 629f, 747f, 631f, 758f, 636f)
                verticalLineTo(428f)
                horizontalLineTo(740f)
                close()
                moveTo(120f, 800f)
                verticalLineTo(688f)
                curveTo(120f, 653f, 138f, 625f, 167f, 597f)
                curveTo(229f, 566f, 293f, 550f, 360f, 550f)
                curveTo(402f, 550f, 444f, 557f, 484f, 566f)
                curveTo(464f, 578f, 448f, 595f, 432f, 615f)
                curveTo(406f, 609f, 380f, 606f, 354f, 606f)
                curveTo(297f, 606f, 242f, 620f, 187f, 646f)
                curveTo(178f, 651f, 172f, 660f, 172f, 680f)
                verticalLineTo(712f)
                horizontalLineTo(493f)
                curveTo(495f, 732f, 502f, 752f, 522f, 792f)
                horizontalLineTo(120f)
                close()
                moveTo(327f, 433f)
                curveTo(280f, 386f, 280f, 320f, 327f, 273f)
                curveTo(374f, 226f, 440f, 226f, 487f, 273f)
                curveTo(534f, 320f, 534f, 386f, 487f, 433f)
                curveTo(440f, 480f, 374f, 480f, 327f, 433f)
                close()
                moveTo(496.5f, 376.5f)
                curveTo(520f, 353f, 520f, 320f, 496.5f, 296.5f)
                curveTo(473f, 273f, 440f, 273f, 416.5f, 296.5f)
                curveTo(393f, 320f, 393f, 353f, 416.5f, 376.5f)
                curveTo(440f, 400f, 473f, 400f, 496.5f, 376.5f)
                close()
            }
        }.build()

    /** Exact Material Symbols asset: art_track. */
    val ArtTrack: ImageVector
        get() = ImageVector.Builder(
            name = "AirPlayrArtTrack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(120f, 760f)
                curveTo(87f, 760f, 63.5f, 736.5f, 63.5f, 703.5f)
                verticalLineTo(303.5f)
                curveTo(63.5f, 270.5f, 87f, 247f, 120f, 247f)
                horizontalLineTo(520f)
                curveTo(553f, 247f, 576.5f, 270.5f, 576.5f, 303.5f)
                verticalLineTo(703.5f)
                curveTo(576.5f, 736.5f, 553f, 760f, 520f, 760f)
                horizontalLineTo(120f)
                close()
                moveTo(120f, 680f)
                horizontalLineTo(520f)
                verticalLineTo(280f)
                horizontalLineTo(120f)
                close()
                moveTo(160f, 600f)
                horizontalLineTo(480f)
                lineTo(376f, 380f)
                lineTo(300f, 480f)
                lineTo(244f, 406f)
                lineTo(160f, 520f)
                close()
                moveTo(680f, 840f)
                verticalLineTo(280f)
                horizontalLineTo(760f)
                verticalLineTo(840f)
                close()
                moveTo(840f, 840f)
                verticalLineTo(280f)
                horizontalLineTo(920f)
                verticalLineTo(840f)
                close()
            }
        }.build()

    /** Library icon based on the bundled books_movies_and_music asset. */
    val Library: ImageVector
        get() = ImageVector.Builder(
            name = "AirPlayrLibrary",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 960f,
            viewportHeight = 960f,
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(160f, 880f)
                lineTo(160f, 322f)
                lineTo(560f, 162f)
                lineTo(560f, 282f)
                lineTo(640f, 282f)
                lineTo(640f, 402f)
                lineTo(560f, 402f)
                lineTo(560f, 362f)
                lineTo(200f, 362f)
                lineTo(200f, 840f)
                lineTo(407f, 840f)
                lineTo(487f, 920f)
                lineTo(160f, 920f)
                close()
                moveTo(360f, 240f)
                lineTo(520f, 178f)
                lineTo(520f, 240f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(680f, 880f)
                curveTo(597f, 880f, 540f, 823f, 540f, 740f)
                curveTo(540f, 657f, 597f, 600f, 680f, 600f)
                curveTo(763f, 600f, 820f, 657f, 820f, 740f)
                curveTo(820f, 823f, 763f, 880f, 680f, 880f)
                close()
                moveTo(630f, 800f)
                lineTo(790f, 700f)
                lineTo(630f, 600f)
                close()
            }
        }.build()

    val Video: ImageVector
        get() = ImageVector.Builder(
            name = "AirPlayrVideo",
            defaultWidth = 30.dp,
            defaultHeight = 30.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.25f,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
            ) {
                moveTo(8f, 6f)
                horizontalLineTo(20f)
                curveTo(22.2f, 6f, 24f, 7.8f, 24f, 10f)
                verticalLineTo(18f)
                curveTo(24f, 20.2f, 22.2f, 22f, 20f, 22f)
                horizontalLineTo(8f)
                curveTo(5.8f, 22f, 4f, 20.2f, 4f, 18f)
                verticalLineTo(10f)
                curveTo(4f, 7.8f, 5.8f, 6f, 8f, 6f)
            }
            path(
                fill = SolidColor(Color.White),
                pathFillType = PathFillType.NonZero,
            ) {
                moveTo(12f, 10.3f)
                lineTo(12f, 17.7f)
                lineTo(18.2f, 14f)
                close()
            }
        }.build()

    val Settings: ImageVector
        get() = ImageVector.Builder(
            name = "AirPlayrSettings",
            defaultWidth = 30.dp,
            defaultHeight = 30.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.25f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
            ) {
                moveTo(11.8f, 4.8f)
                horizontalLineTo(16.2f)
                lineTo(16.9f, 7f)
                curveTo(17.5f, 7.2f, 18.1f, 7.5f, 18.6f, 8f)
                lineTo(20.8f, 7.3f)
                lineTo(23f, 11.1f)
                lineTo(21.4f, 12.7f)
                curveTo(21.5f, 13.2f, 21.6f, 13.7f, 21.6f, 14.2f)
                curveTo(21.6f, 14.7f, 21.5f, 15.2f, 21.4f, 15.7f)
                lineTo(23f, 17.3f)
                lineTo(20.8f, 21.1f)
                lineTo(18.6f, 20.4f)
                curveTo(18.1f, 20.9f, 17.5f, 21.2f, 16.9f, 21.4f)
                lineTo(16.2f, 23.6f)
                horizontalLineTo(11.8f)
                lineTo(11.1f, 21.4f)
                curveTo(10.5f, 21.2f, 9.9f, 20.9f, 9.4f, 20.4f)
                lineTo(7.2f, 21.1f)
                lineTo(5f, 17.3f)
                lineTo(6.6f, 15.7f)
                curveTo(6.5f, 15.2f, 6.4f, 14.7f, 6.4f, 14.2f)
                curveTo(6.4f, 13.7f, 6.5f, 13.2f, 6.6f, 12.7f)
                lineTo(5f, 11.1f)
                lineTo(7.2f, 7.3f)
                lineTo(9.4f, 8f)
                curveTo(9.9f, 7.5f, 10.5f, 7.2f, 11.1f, 7f)
                close()
                moveTo(14f, 11f)
                curveTo(12.2f, 11f, 10.8f, 12.4f, 10.8f, 14.2f)
                curveTo(10.8f, 16f, 12.2f, 17.4f, 14f, 17.4f)
                curveTo(15.8f, 17.4f, 17.2f, 16f, 17.2f, 14.2f)
                curveTo(17.2f, 12.4f, 15.8f, 11f, 14f, 11f)
                close()
            }
        }.build()

    val Shuffle: ImageVector
        get() = ImageVector.Builder("AirPlayrShuffle", 26.dp, 26.dp, 26f, 26f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(4f, 7f); lineTo(7f,7f); curveTo(9.5f,7f,10.5f,19f,16f,19f); lineTo(22f,19f)
                moveTo(19f,16f); lineTo(22f,19f); lineTo(19f,22f)
                moveTo(4f,19f); lineTo(7f,19f); curveTo(8.5f,19f,9.8f,16.4f,11f,14f)
                moveTo(15f,8f); curveTo(16.1f,6.2f,17.1f,7f,18f,7f); lineTo(22f,7f)
                moveTo(19f,4f); lineTo(22f,7f); lineTo(19f,10f)
            }
        }.build()

    val Repeat: ImageVector
        get() = ImageVector.Builder("AirPlayrRepeat", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2.15f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(6f,9f); horizontalLineTo(20f); moveTo(17f,6f); lineTo(20f,9f); lineTo(17f,12f)
                moveTo(20f,19f); horizontalLineTo(6f); moveTo(9f,16f); lineTo(6f,19f); lineTo(9f,22f)
            }
        }.build()

    val RepeatOne: ImageVector
        get() = ImageVector.Builder("AirPlayrRepeatOne", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2.15f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(6f,9f); horizontalLineTo(20f); moveTo(17f,6f); lineTo(20f,9f); lineTo(17f,12f)
                moveTo(20f,19f); horizontalLineTo(6f); moveTo(9f,16f); lineTo(6f,19f); lineTo(9f,22f)
            }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(13.1f,11.7f); lineTo(14.8f,10.2f); verticalLineTo(17f); moveTo(13.3f,17f); horizontalLineTo(16.3f)
            }
        }.build()

    val Previous: ImageVector
        get() = ImageVector.Builder("AirPlayrPrevious", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2.15f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(6f,5.5f); verticalLineTo(22.5f)
            }
            path(fill = SolidColor(Color.White)) { moveTo(22f,6f); lineTo(10f,14f); lineTo(22f,22f); close() }
        }.build()

    /** Exact geometry from the uploaded SVG reference icon, used only on the full-screen player. */
    val PlayerPrevious: ImageVector
        get() = ImageVector.Builder("AirPlayrPlayerPrevious", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(20.2409f, 7.21957f)
                verticalLineTo(16.7896f)
                curveTo(20.2409f, 18.7496f, 18.1109f, 19.9796f, 16.4109f, 18.9996f)
                lineTo(12.2609f, 16.6096f)
                lineTo(8.11094f, 14.2096f)
                curveTo(6.41094f, 13.2296f, 6.41094f, 10.7796f, 8.11094f, 9.79957f)
                lineTo(12.2609f, 7.39957f)
                lineTo(16.4109f, 5.00957f)
                curveTo(18.1109f, 4.02957f, 20.2409f, 5.24957f, 20.2409f, 7.21957f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(3.76172f, 18.9303f)
                curveTo(3.35172f, 18.9303f, 3.01172f, 18.5903f, 3.01172f, 18.1803f)
                verticalLineTo(5.82031f)
                curveTo(3.01172f, 5.41031f, 3.35172f, 5.07031f, 3.76172f, 5.07031f)
                curveTo(4.17172f, 5.07031f, 4.51172f, 5.41031f, 4.51172f, 5.82031f)
                verticalLineTo(18.1803f)
                curveTo(4.51172f, 18.5903f, 4.17172f, 18.9303f, 3.76172f, 18.9303f)
                close()
            }
        }.build()

    val Next: ImageVector
        get() = ImageVector.Builder("AirPlayrNext", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2.15f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(22f,5.5f); verticalLineTo(22.5f)
            }
            path(fill = SolidColor(Color.White)) { moveTo(6f,6f); lineTo(18f,14f); lineTo(6f,22f); close() }
        }.build()

    val Rewind: ImageVector
        get() = ImageVector.Builder("AirPlayrRewind", 30.dp, 30.dp, 30f, 30f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(15f,7f); curveTo(10.6f,7f,7f,10.1f,7f,14f); curveTo(7f,17.9f,10.6f,21f,15f,21f); curveTo(19.4f,21f,23f,17.9f,23f,14f)
                moveTo(9f,10f); lineTo(5f,10f); lineTo(5f,14f)
            }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) { moveTo(13.2f,11f); lineTo(11.2f,11f); lineTo(11.2f,12.6f); lineTo(12.7f,12.6f); curveTo(14.3f,12.6f,14.6f,15.2f,12.4f,15.2f); curveTo(11.6f,15.2f,11.1f,14.7f,10.9f,14.2f) }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) { moveTo(16.8f,11f); lineTo(15.2f,11f); lineTo(15.2f,15.2f); lineTo(17f,15.2f); curveTo(18.7f,15.2f,18.8f,11f,16.8f,11f) }
        }.build()

    val Forward: ImageVector
        get() = ImageVector.Builder("AirPlayrForward", 30.dp, 30.dp, 30f, 30f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.9f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(15f,7f); curveTo(19.4f,7f,23f,10.1f,23f,14f); curveTo(23f,17.9f,19.4f,21f,15f,21f); curveTo(10.6f,21f,7f,17.9f,7f,14f)
                moveTo(21f,10f); lineTo(25f,10f); lineTo(25f,14f)
            }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) { moveTo(11.2f,11f); lineTo(13.2f,11f); lineTo(13.2f,12.6f); lineTo(11.7f,12.6f); curveTo(10.1f,12.6f,9.8f,15.2f,12f,15.2f); curveTo(12.8f,15.2f,13.3f,14.7f,13.5f,14.2f) }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.6f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) { moveTo(14.8f,11f); lineTo(16.4f,11f); lineTo(16.4f,15.2f); lineTo(14.6f,15.2f); curveTo(12.9f,15.2f,12.8f,11f,14.8f,11f) }
        }.build()

    val Play: ImageVector
        get() = ImageVector.Builder("AirPlayrPlay", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = SolidColor(Color.White)) { moveTo(9f,6f); lineTo(21f,14f); lineTo(9f,22f); close() }
        }.build()

    val More: ImageVector
        get() = ImageVector.Builder("AirPlayrMore", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 12f);
                moveTo(5f, 12f);
                arcToRelative(1.75f, 1.75f, 0f, true, false, 3.5f, 0f);
                arcToRelative(1.75f, 1.75f, 0f, true, false, -3.5f, 0f);
                moveTo(10.25f, 12f);
                arcToRelative(1.75f, 1.75f, 0f, true, false, 3.5f, 0f);
                arcToRelative(1.75f, 1.75f, 0f, true, false, -3.5f, 0f);
                moveTo(15.5f, 12f);
                arcToRelative(1.75f, 1.75f, 0f, true, false, 3.5f, 0f);
                arcToRelative(1.75f, 1.75f, 0f, true, false, -3.5f, 0f);
            }
        }.build()

    val Equalizer: ImageVector
        get() = ImageVector.Builder("AirPlayrEqualizer", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) {
                moveTo(5f, 19f); verticalLineTo(12f);
                moveTo(12f, 19f); verticalLineTo(5f);
                moveTo(19f, 19f); verticalLineTo(9f);
            }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) {
                moveTo(3.5f, 12f); horizontalLineTo(6.5f);
                moveTo(10.5f, 5f); horizontalLineTo(13.5f);
                moveTo(17.5f, 9f); horizontalLineTo(20.5f);
            }
        }.build()

    val Lyrics: ImageVector
        get() = ImageVector.Builder("AirPlayrLyrics", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(5f, 4f); horizontalLineTo(17f); curveTo(19.2f,4f,21f,5.8f,21f,8f); verticalLineTo(20f); curveTo(21f,22.2f,19.2f,24f,17f,24f); horizontalLineTo(8f); curveTo(5.8f,24f,4f,22.2f,4f,20f); verticalLineTo(6f); curveTo(4f,4.9f,4.9f,4f,5f,4f)
                moveTo(8f,11f); horizontalLineTo(16f); moveTo(8f,15f); horizontalLineTo(16f);
            }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 1.8f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) { moveTo(14f,20f); curveTo(16.3f,20f,18f,18.5f,18f,16.5f); verticalLineTo(13f); moveTo(17.5f,13f); horizontalLineTo(20.5f) }
        }.build()

    val Sleep: ImageVector
        get() = ImageVector.Builder(
            name = "AirPlayrSleep",
            defaultWidth = 30.dp,
            defaultHeight = 30.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.15f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
            ) {
                moveTo(19.6f, 7.1f)
                curveTo(17.4f, 6.6f, 14.8f, 7.4f, 13.4f, 9.5f)
                curveTo(11.1f, 13.0f, 12.2f, 17.9f, 15.8f, 20.1f)
                curveTo(17.2f, 21.0f, 18.9f, 21.4f, 20.5f, 21.1f)
                curveTo(18.9f, 23.0f, 16.8f, 24.1f, 14.5f, 24.0f)
                curveTo(9.2f, 23.8f, 5.2f, 19.5f, 5.2f, 14.2f)
                curveTo(5.2f, 9.2f, 9.0f, 5.0f, 14.0f, 4.8f)
                curveTo(16.1f, 4.7f, 18.1f, 5.5f, 19.6f, 7.1f)
            }
        }.build()

    val Queue: ImageVector
        get() = ImageVector.Builder("AirPlayrQueue", 24.dp, 24.dp, 24f, 24f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) {
                moveTo(4f, 6f); horizontalLineTo(16f);
                moveTo(4f, 11f); horizontalLineTo(16f);
                moveTo(4f, 16f); horizontalLineTo(11f);
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(17f, 14f); lineTo(22f, 17f); lineTo(17f, 20f); close();
            }
        }.build()


    val Back: ImageVector
        get() = ImageVector.Builder("AirPlayrBack", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2.15f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(20f, 14f); horizontalLineTo(7f); moveTo(12f, 8f); lineTo(6f, 14f); lineTo(12f, 20f)
            }
        }.build()

    /** Down-chevron used by the full-screen player to match the reference collapse/back control. */
    val PlayerBack: ImageVector
        get() = ImageVector.Builder("AirPlayrPlayerBack", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2.3f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(5.5f, 9f); lineTo(14f, 17f); lineTo(22.5f, 9f)
            }
        }.build()

    val VolumeDown: ImageVector
        get() = ImageVector.Builder("AirPlayrVolumeDown", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = SolidColor(Color.White)) { moveTo(5f, 11f); horizontalLineTo(9f); lineTo(14f, 7f); verticalLineTo(21f); lineTo(9f, 17f); horizontalLineTo(5f); close() }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) { moveTo(18f, 11f); curveTo(20f, 12.5f, 20f, 15.5f, 18f, 17f) }
        }.build()

    val VolumeUp: ImageVector
        get() = ImageVector.Builder("AirPlayrVolumeUp", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = SolidColor(Color.White)) { moveTo(5f, 11f); horizontalLineTo(9f); lineTo(14f, 7f); verticalLineTo(21f); lineTo(9f, 17f); horizontalLineTo(5f); close() }
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round) { moveTo(18f, 9f); curveTo(22f, 11.5f, 22f, 16.5f, 18f, 19f); moveTo(20f, 6f); curveTo(26f, 10f, 26f, 18f, 20f, 22f) }
        }.build()

    val Favorite: ImageVector
        get() = ImageVector.Builder("AirPlayrFavorite", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(14f, 23f); curveTo(13.5f, 22.5f, 5f, 17.2f, 5f, 10.8f); curveTo(5f, 7.4f, 7.2f, 5f, 10.3f, 5f); curveTo(12.1f, 5f, 13.4f, 5.9f, 14f, 7f); curveTo(14.6f, 5.9f, 15.9f, 5f, 17.7f, 5f); curveTo(20.8f, 5f, 23f, 7.4f, 23f, 10.8f); curveTo(23f, 17.2f, 14.5f, 22.5f, 14f, 23f); close()
            }
        }.build()

    val FavoriteBorder: ImageVector
        get() = ImageVector.Builder("AirPlayrFavoriteBorder", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = null, stroke = SolidColor(Color.White), strokeLineWidth = 2f, strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round, strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round) {
                moveTo(14f, 23f); curveTo(13.5f, 22.5f, 5f, 17.2f, 5f, 10.8f); curveTo(5f, 7.4f, 7.2f, 5f, 10.3f, 5f); curveTo(12.1f, 5f, 13.4f, 5.9f, 14f, 7f); curveTo(14.6f, 5.9f, 15.9f, 5f, 17.7f, 5f); curveTo(20.8f, 5f, 23f, 7.4f, 23f, 10.8f); curveTo(23f, 17.2f, 14.5f, 22.5f, 14f, 23f)
            }
        }.build()

    val Pause: ImageVector
        get() = ImageVector.Builder("AirPlayrPause", 28.dp, 28.dp, 28f, 28f).apply {
            path(fill = SolidColor(Color.White)) { moveTo(8f,6f); horizontalLineTo(12f); verticalLineTo(22f); horizontalLineTo(8f); close(); moveTo(16f,6f); horizontalLineTo(20f); verticalLineTo(22f); horizontalLineTo(16f); close() }
        }.build()
}
