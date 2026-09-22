from pathlib import Path

root = Path("android/app/src/main/java/com/airplayr/app")

def add_import(file, marker, imports):
    p = root / file
    s = p.read_text()
    if imports.strip() in s:
        return
    s = s.replace(marker, marker + "\n" + imports, 1)
    p.write_text(s)
    print("imports:", file)


# History sheet brace repair
p = root / "ui/components/HistoryFavoriteActionSheet.kt"
s = p.read_text()
if "HistoryFavoriteLyricsSheet" not in s:
    s = s.rstrip() + "\n}\n"
    p.write_text(s)
    print("brace repair")


# Music imports
add_import(
"ui/screens/MusicScreen.kt",
"package com.airplayr.app.ui.screens",
"""import androidx.compose.ui.unit.toPx
import androidx.compose.foundation.layout.fillMaxHeight"""
)


# Theme luminance
add_import(
"ui/theme/AirPlayrTheme.kt",
"package com.airplayr.app.ui.theme",
"import androidx.compose.ui.graphics.luminance"
)


# Stats imports
add_import(
"ui/screens/StatsScreen.kt",
"package com.airplayr.app.ui.screens",
"""import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.QueueMusic"""
)


# Video align
add_import(
"ui/screens/VideoScreen.kt",
"package com.airplayr.app.ui.screens",
"import androidx.compose.foundation.layout.align"
)


# Search type fix
p = root / "ui/screens/SearchScreen.kt"
s = p.read_text()
s = s.replace("List<List<Any>>", "List<List<AirPlayrMediaMenuAction>>")
p.write_text(s)
print("Search type pass")


# Stats coroutine scope if missing
print("repair pass complete")
