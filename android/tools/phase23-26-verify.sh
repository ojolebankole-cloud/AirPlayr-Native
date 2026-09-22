#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
fail=0
pass(){ echo "PASS: $1"; }
fail(){ echo "FAIL: $1"; fail=$((fail+1)); }

# Phase 23: exact AirPlayr custom icon contract for the primary shell/player controls.
test -f app/src/main/java/com/airplayr/app/ui/theme/AirPlayrIcons.kt && pass 'AirPlayr custom icon library present' || fail 'AirPlayr custom icon library missing'
for sym in Home Music Video Settings Shuffle Repeat Previous Next Rewind Forward Play Pause More Equalizer Lyrics Sleep Queue Back VolumeDown VolumeUp Favorite FavoriteBorder; do
  grep -q "val $sym:" app/src/main/java/com/airplayr/app/ui/theme/AirPlayrIcons.kt && pass "custom icon: $sym" || fail "custom icon missing: $sym"
done
# Primary Home quick actions use AirPlayr icons where the reference uses the same family.
grep -q 'Triple("Shuffle", AirPlayrIcons.Shuffle' app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt && pass 'Home shuffle uses custom AirPlayr icon' || fail 'Home shuffle icon mismatch'
grep -q 'Triple("Favorites", AirPlayrIcons.FavoriteBorder' app/src/main/java/com/airplayr/app/ui/screens/HomeScreen.kt && pass 'Home favorite uses custom AirPlayr icon' || fail 'Home favorite icon mismatch'

# Phase 24: shared card primitives and reference dimensions.
test -f app/src/main/java/com/airplayr/app/ui/components/AirPlayrCard.kt && pass 'shared AirPlayr card primitive present' || fail 'shared card primitive missing'
grep -q 'const val radiusDp = 27f' app/src/main/java/com/airplayr/app/ui/theme/FeatureCardTokens.kt && pass 'feature-card 27dp radius contract' || fail 'feature-card radius contract missing'
grep -q 'const val shadowElevationDp = 10f' app/src/main/java/com/airplayr/app/ui/theme/FeatureCardTokens.kt && pass 'feature-card elevation contract' || fail 'feature-card elevation contract missing'
grep -q 'height(272.dp)' app/src/main/java/com/airplayr/app/ui/components/FeatureCard.kt && pass 'feature-card reference height' || fail 'feature-card height mismatch'
grep -q 'fontSize = 21.sp' app/src/main/java/com/airplayr/app/ui/components/FeatureCard.kt && pass 'feature-card title typography' || fail 'feature-card title typography missing'
for f in HomeScreen.kt MusicScreen.kt StatsScreen.kt SettingsScreen.kt; do
  grep -q 'AirPlayrCard' "app/src/main/java/com/airplayr/app/ui/screens/$f" && pass "shared card usage: $f" || fail "shared card usage missing: $f"
done

# Phase 25: root canvas is not globally blurred; explicit glass retains tonal/border/shadow contract.
grep -q 'backgroundSecondaryBlurRadiusDp' app/src/main/java/com/airplayr/app/ui/components/LiquidGlass.kt && pass 'glass blur contract retained' || fail 'glass blur contract missing'
grep -q 'surfaceContainer.copy(alpha = AirPlayrGlass.surfaceAlpha)' app/src/main/java/com/airplayr/app/ui/components/LiquidGlass.kt && pass 'glass surface alpha contract' || fail 'glass surface alpha missing'
grep -q 'outlineVariant.copy(alpha = AirPlayrGlass.borderAlpha)' app/src/main/java/com/airplayr/app/ui/components/LiquidGlass.kt && pass 'glass border contract' || fail 'glass border missing'
grep -q 'ambientColor = Color(0xFF171426)' app/src/main/java/com/airplayr/app/ui/components/LiquidGlass.kt && pass 'glass shadow color contract' || fail 'glass shadow color missing'

# Phase 26: shell geometry and drawer.
grep -q 'RoundedCornerShape(30.dp)' app/src/main/java/com/airplayr/app/ui/components/AirPlayrBottomDock.kt && pass 'dock 30dp shell radius' || fail 'dock shell radius missing'
grep -q 'height(72.dp)' app/src/main/java/com/airplayr/app/ui/components/AirPlayrBottomDock.kt && pass 'dock 72dp nav row' || fail 'dock nav height missing'
grep -q 'ArtworkBackdrop' app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt && pass 'drawer artwork-backed header' || fail 'drawer artwork header missing'
grep -q 'scale = 1.08f' app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt && pass 'drawer artwork scale contract' || fail 'drawer artwork scale missing'
grep -q 'width(drawerWidth)' app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt && pass 'drawer computed width' || fail 'drawer width missing'
grep -q '0.87f' app/src/main/java/com/airplayr/app/ui/components/MenuDrawer.kt && pass 'drawer 87% width rule' || fail 'drawer 87% rule missing'

echo
if [[ "$fail" -eq 0 ]]; then echo 'RESULT: PASS'; else echo "RESULT: FAIL ($fail issues)"; exit 1; fi
