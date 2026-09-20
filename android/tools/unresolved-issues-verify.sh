#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FAIL=0
pass(){ echo "PASS: $1"; }
bad(){ echo "FAIL: $1"; FAIL=1; }

THEME="$ROOT/app/src/main/java/com/airplayr/app/ui/theme/AirPlayrTheme.kt"
CONTAINER="$ROOT/app/src/main/java/com/airplayr/app/di/AppContainer.kt"
MAIN="$ROOT/app/src/main/java/com/airplayr/app/MainActivity.kt"
APP="$ROOT/app/src/main/java/com/airplayr/app/ui/AirPlayrApp.kt"
SECONDARY="$ROOT/app/src/main/java/com/airplayr/app/ui/components/AirPlayrSecondaryComponents.kt"
ART="$ROOT/app/src/main/java/com/airplayr/app/ui/components/Artwork.kt"
TOKENS="$ROOT/app/src/main/java/com/airplayr/app/ui/theme/DesignTokens.kt"
STRUCTURED="$ROOT/app/src/main/java/com/airplayr/app/core/data/StructuredRepository.kt"
HOME_VM="$ROOT/app/src/main/java/com/airplayr/app/ui/navigation/HomeViewModel.kt"
STATS="$ROOT/app/src/main/java/com/airplayr/app/core/data/PlaybackStatsRepository.kt"
DAO="$ROOT/app/src/main/java/com/airplayr/app/core/data/db/UserDataDao.kt"

if grep -q 'handler.post(refresh)' "$THEME" && ! grep -q 'postDelayed(refresh, 220L)' "$THEME"; then pass 'Wallpaper dynamic color refresh is next-frame coalesced'; else bad 'Wallpaper refresh still uses an explicit 220ms delay'; fi
if grep -q 'getSharedPreferences("airplayr_startup"' "$CONTAINER" && grep -q 'getString("theme", "system")' "$CONTAINER"; then pass 'Startup theme is synchronously mirrored'; else bad 'Synchronous startup theme mirror missing'; fi
if grep -q 'installSplashScreen()' "$MAIN" && ! grep -q 'setKeepOnScreenCondition' "$MAIN"; then pass 'Splash is no longer held by an async theme gate'; else bad 'Async splash gate still present'; fi
if grep -q 'scrimColor = androidx.compose.ui.graphics.Color.Transparent' "$APP"; then pass 'Cast sheet full-screen scrim removed'; else bad 'Cast sheet still dims the app'; fi
if grep -q 'graphicsLayer {' "$SECONDARY" && grep -q 'shadowElevation = 2.0.dp.toPx()' "$SECONDARY" && ! grep -q 'color = c.scrim.copy(alpha = 0.46f)' "$SECONDARY"; then pass 'Overflow control is transparent with 2dp shadow'; else bad 'Overflow control still has opaque background or lacks shadow'; fi
if grep -q 'fallbackIconAlpha: Float = 0.52f' "$TOKENS" && grep -q 'onSurfaceVariant.copy(alpha = com.airplayr.app.ui.theme.AirPlayrArtworkTokens.fallbackIconAlpha)' "$ART"; then pass 'Artwork fallback uses shared faint icon tone'; else bad 'Shared faint artwork fallback tone missing'; fi
if grep -q 'item.artworkUri ?: item.thumbnailUri' "$STRUCTURED" && grep -q 'filter { it.mediaType == MediaType.VIDEO.value }' "$HOME_VM" && grep -q 'filter { it.mediaType == MediaType.VIDEO }' "$HOME_VM"; then pass 'Recently-played video history retains thumbnail + video-only projection'; else bad 'Recently-played video history hardening missing'; fi
if grep -q 'mediaItemsByIds' "$DAO" && grep -q 'chunked(900)' "$STATS"; then pass 'Genre/stat media resolution handles large event sets'; else bad 'Stats media resolution is still vulnerable to bounded UI snapshots / bind limits'; fi
if grep -q 'dayCount = ' "$STATS" && grep -q 'coerceIn(1, 31)' "$STATS"; then pass 'Stats activity grid is period-aware'; else bad 'Stats activity grid still hard-codes seven days'; fi
if grep -q 'surfaceContainerLow' "$THEME" && grep -q 'arrangeLightScheme' "$THEME" && grep -q 'dynamicDarkColorScheme' "$THEME" && grep -q 'dynamicLightColorScheme' "$THEME"; then pass 'Material You dynamic light/dark tonal foundation retained'; else bad 'Dynamic Material You foundation missing'; fi

echo
if [[ "$FAIL" -eq 0 ]]; then echo 'RESULT: PASS'; else echo "RESULT: FAIL ($FAIL issues)"; exit 1; fi
