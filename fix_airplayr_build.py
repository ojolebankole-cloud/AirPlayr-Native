from pathlib import Path

root = Path("android/app/src/main/java/com/airplayr/app")

def replace(path, old, new):
    p = root / path
    s = p.read_text()
    if old not in s:
        print("SKIP:", path, "pattern not found")
        return
    p.write_text(s.replace(old, new, 1))
    print("FIXED:", path)

# -------------------------------------------------
# MusicScreen LazyListScope receiver fix
# -------------------------------------------------

replace(
"ui/screens/MusicScreen.kt",
"""): LazyListScope {
    if (filteredSongs.isEmpty()) {""",
""") {
    if (filteredSongs.isEmpty()) {"""
)

replace(
"ui/screens/MusicScreen.kt",
"""private fun musicSongItems(
    filteredSongs:""",
"""private fun LazyListScope.musicSongItems(
    filteredSongs:"""
)


# -------------------------------------------------
# Missing imports - icons
# -------------------------------------------------

replace(
"ui/components/AirPlayrSecondaryComponents.kt",
"import androidx.compose.material.icons.filled.MoreVert",
"""import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistAdd"""
)

replace(
"ui/screens/SettingsScreen.kt",
"import androidx.compose.material.icons.filled.Refresh",
"""import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check"""
)


# -------------------------------------------------
# HistoryFavoriteActionSheet missing final brace
# -------------------------------------------------

p = root / "ui/components/HistoryFavoriteActionSheet.kt"
s = p.read_text()

if not s.rstrip().endswith("}"):
    p.write_text(s.rstrip() + "\n}\n")
    print("FIXED HistoryFavoriteActionSheet closing brace")
else:
    print("HistoryFavoriteActionSheet brace already present")


# -------------------------------------------------
# Search menu type mismatch
# -------------------------------------------------

replace(
"ui/screens/SearchScreen.kt",
"List<List<Any>>",
"List<List<AirPlayrMediaMenuAction>>"
)


# -------------------------------------------------
# Add missing imports where wildcard was removed
# -------------------------------------------------

replace(
"ui/screens/PlayerScreen.kt",
"package com.airplayr.app.ui.screens",
"""package com.airplayr.app.ui.screens

import com.airplayr.app.core.data.media.MediaType"""
)

print("Patch pass completed")
