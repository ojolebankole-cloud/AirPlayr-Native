from pathlib import Path

ROOT = Path("android/app/src/main/java/com/airplayr/app")

def patch(rel, fn):
    p = ROOT / rel
    s = p.read_text()
    new = fn(s)
    if new == s:
        raise SystemExit(f"STOP: no change made to {rel}")
    p.write_text(new)
    print("FIXED:", rel)

# ------------------------------------------------------------
# 1. HistoryFavoriteActionSheet.kt
# Actual source is exactly one closing brace short.
# ------------------------------------------------------------
def history_fix(s):
    opens = s.count("{")
    closes = s.count("}")
    if opens != closes + 1:
        raise SystemExit(
            f"STOP: unexpected HistoryFavoriteActionSheet brace count: "
            f"{opens} '{{' vs {closes} '}}'"
        )
    return s.rstrip() + "\n}\n"

patch(
    "ui/components/HistoryFavoriteActionSheet.kt",
    history_fix
)

# ------------------------------------------------------------
# 2. ExternalVideoScreen.kt
# Trailing lambdas were binding against the final Boolean
# parameters instead of onClick.
# ------------------------------------------------------------
def external_video_fix(s):
    old = '''VideoRoundControl(Icons.Default.Replay10, "Back 10") { seek(-10_000L) }
                    VideoRoundControl(if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow, "Play/Pause", { player?.let { if (it.isPlaying) it.pause() else it.play(); touch() } }, 78.dp, true)
                    VideoRoundControl(Icons.Default.Forward10, "Forward 10") { seek(10_000L) }'''

    new = '''VideoRoundControl(
                        Icons.Default.Replay10,
                        "Back 10",
                        onClick = { seek(-10_000L) },
                    )
                    VideoRoundControl(
                        if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                        "Play/Pause",
                        onClick = { player?.let { if (it.isPlaying) it.pause() else it.play(); touch() } },
                        size = 78.dp,
                        primary = true,
                    )
                    VideoRoundControl(
                        Icons.Default.Forward10,
                        "Forward 10",
                        onClick = { seek(10_000L) },
                    )'''

    if old not in s:
        raise SystemExit(
            "STOP: ExternalVideoScreen VideoRoundControl block "
            "does not match the inspected source."
        )

    s = s.replace(old, new, 1)

    old2 = '''VideoBottomButton(Icons.Default.Replay10, "Replay 10") { seek(-10_000L) }
                        VideoBottomButton(if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow, if (player?.isPlaying == true) "Pause" else "Play", { player?.let { if (it.isPlaying) it.pause() else it.play(); touch() } }, prominent = true, playing = player?.isPlaying == true)
                        VideoBottomButton(Icons.Default.Forward10, "Forward 10") { seek(10_000L) }
                        VideoBottomButton(Icons.Default.AspectRatio, fitMode.label) {'''

    new2 = '''VideoBottomButton(
                            Icons.Default.Replay10,
                            "Replay 10",
                            onClick = { seek(-10_000L) },
                        )
                        VideoBottomButton(
                            if (player?.isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (player?.isPlaying == true) "Pause" else "Play",
                            onClick = { player?.let { if (it.isPlaying) it.pause() else it.play(); touch() } },
                            prominent = true,
                            playing = player?.isPlaying == true,
                        )
                        VideoBottomButton(
                            Icons.Default.Forward10,
                            "Forward 10",
                            onClick = { seek(10_000L) },
                        )
                        VideoBottomButton(
                            Icons.Default.AspectRatio,
                            fitMode.label,
                            onClick = {'''

    if old2 not in s:
        raise SystemExit(
            "STOP: ExternalVideoScreen VideoBottomButton block "
            "does not match the inspected source."
        )

    return s.replace(old2, new2, 1)

patch(
    "ui/screens/ExternalVideoScreen.kt",
    external_video_fix
)

# ------------------------------------------------------------
# 3. MusicScreen.kt
# toPx() must execute inside Density scope.
# ------------------------------------------------------------
def music_fix(s):
    s = s.replace(
        "import androidx.compose.ui.unit.toPx\n",
        "",
        1,
    )

    if "import androidx.compose.ui.platform.LocalDensity\n" not in s:
        marker = "import androidx.compose.ui.unit.dp\n"
        if marker not in s:
            raise SystemExit("STOP: MusicScreen import marker not found.")
        s = s.replace(
            marker,
            marker + "import androidx.compose.ui.platform.LocalDensity\n",
            1,
        )

    old = "val dragRangePx = (trackHeight - thumbHeight).toPx().coerceAtLeast(1f)"
    new = "val dragRangePx = with(LocalDensity.current) { (trackHeight - thumbHeight).toPx() }.coerceAtLeast(1f)"

    if s.count(old) != 2:
        raise SystemExit(
            f"STOP: expected 2 MusicScreen dragRangePx expressions, "
            f"found {s.count(old)}"
        )

    return s.replace(old, new)

patch(
    "ui/screens/MusicScreen.kt",
    music_fix
)

# ------------------------------------------------------------
# 4. LibraryScreen.kt
# Missing AirPlayrIcons import + explicit Intent.type.
# ------------------------------------------------------------
def library_fix(s):
    if "import com.airplayr.app.ui.theme.AirPlayrIcons\n" not in s:
        marker = "import com.airplayr.app.ui.theme.LocalAirPlayrColors\n"
        if marker not in s:
            raise SystemExit("STOP: LibraryScreen theme import marker not found.")
        s = s.replace(
            marker,
            "import com.airplayr.app.ui.theme.AirPlayrIcons\n" + marker,
            1,
        )

    old = 'type = item.mimeType ?: if (isVideo) "video/*" else "audio/*"'
    new = 'this.type = item.mimeType ?: if (isVideo) "video/*" else "audio/*"'

    if old not in s:
        raise SystemExit("STOP: LibraryScreen Intent.type assignment not found.")

    return s.replace(old, new, 1)

patch(
    "ui/screens/LibraryScreen.kt",
    library_fix
)

# ------------------------------------------------------------
# 5. VideoScreen.kt
# Previous repair added an invalid align import.
# Modifier.align is supplied by the appropriate layout scope.
# ------------------------------------------------------------
def video_fix(s):
    bad = "import androidx.compose.foundation.layout.align\n"
    if bad not in s:
        raise SystemExit(
            "STOP: expected invalid VideoScreen align import was not found."
        )
    return s.replace(bad, "", 1)

patch(
    "ui/screens/VideoScreen.kt",
    video_fix
)

# ------------------------------------------------------------
# 6. Shared ActionGroup / ActionButton
# These are referenced by ParityScreens but do not exist
# anywhere in the inspected ZIP.
# Add them to the existing shared component file.
# ------------------------------------------------------------
def secondary_fix(s):
    if "fun ActionGroup(" in s or "fun ActionButton(" in s:
        raise SystemExit(
            "STOP: ActionGroup/ActionButton unexpectedly already exist."
        )

    marker = "@Composable\nfun AirPlayrSectionEmpty"

    if marker not in s:
        raise SystemExit(
            "STOP: AirPlayrSectionEmpty insertion point not found."
        )

    block = '''@Composable
fun ActionGroup(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

@Composable
fun ActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    destructive: Boolean = false,
) {
    val c = LocalAirPlayrColors.current

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = c.surfaceContainerHigh,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (destructive) c.error else c.primary,
                modifier = Modifier.size(22.dp),
            )

            Text(
                label,
                color = if (destructive) c.error else c.textPrimary,
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
            )
        }
    }
}

'''

    return s.replace(marker, block + marker, 1)

patch(
    "ui/components/AirPlayrSecondaryComponents.kt",
    secondary_fix
)

# ------------------------------------------------------------
# 7. SearchScreen.kt
# SearchGenericRow is genuinely missing from the inspected ZIP.
# Add only the missing generic non-media row.
# ------------------------------------------------------------
def search_fix(s):
    if "fun SearchGenericRow(" in s:
        raise SystemExit(
            "STOP: SearchGenericRow unexpectedly already exists."
        )

    marker = "@Composable\nprivate fun SearchContentRow"

    if marker not in s:
        raise SystemExit(
            "STOP: SearchContentRow insertion point not found."
        )

    block = '''@Composable
private fun SearchGenericRow(
    entry: SearchEntry,
    colors: com.airplayr.app.ui.theme.AirPlayrColors,
    onClick: () -> Unit,
) {
    val icon = when (entry.type) {
        "artist" -> AirPlayrIcons.Artist
        "album" -> AirPlayrIcons.AlbumDisc
        "playlist" -> Icons.Default.QueueMusic
        "genre" -> AirPlayrIcons.Music
        "video" -> AirPlayrIcons.Video
        else -> AirPlayrIcons.Music
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = colors.primary,
            modifier = Modifier.size(42.dp),
        )

        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                entry.title,
                color = colors.textPrimary,
                fontFamily = AirPlayrFonts.SmoochSans,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                entry.subtitle,
                color = colors.textSecondary,
                fontFamily = AirPlayrFonts.OpenSans,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

'''

    return s.replace(marker, block + marker, 1)

patch(
    "ui/screens/SearchScreen.kt",
    search_fix
)

# Remove only the temporary repair scripts created during this repair attempt.
for name in ("fix_airplayr_build.py", "fix_airplayr_build2.py"):
    p = Path(name)
    if p.exists():
        p.unlink()
        print("REMOVED:", name)

print()
print("TARGETED AIRPLAYR BUILD REPAIR COMPLETE.")
print("Only the identified compile defects were changed.")
