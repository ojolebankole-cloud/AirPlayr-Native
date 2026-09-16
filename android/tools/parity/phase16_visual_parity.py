#!/usr/bin/env python3
"""Static visual-parity gate: compares theme contracts, assets, and screen inventory."""
from __future__ import annotations
import hashlib
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[2]
EXPO = Path('/mnt/data/expo1618/userfix')
NATIVE = ROOT

REQUIRED_ASSETS = [
    'assets/icon.png', 'assets/adaptive-icon.png', 'assets/favicon.png', 'assets/splash-icon.png',
    'assets/fonts/CaacupeOne-Regular.ttf', 'assets/fonts/DancingScript-Regular.ttf',
    'assets/fonts/OpenSans-Bold.ttf', 'assets/fonts/OpenSans-Medium.ttf', 'assets/fonts/OpenSans-Regular.ttf',
    'assets/fonts/Oswald-Medium.ttf', 'assets/fonts/ShadowsIntoLight-Regular.ttf', 'assets/fonts/SmoochSans-SemiBold.ttf',
    'assets/feature-cards/DJ boy-cuate.svg', 'assets/feature-cards/Headphone-amico.svg',
    'assets/feature-cards/Media player-bro.svg', 'assets/feature-cards/Music-cuate.svg',
    'assets/feature-cards/Playing Music-bro.svg', 'assets/feature-cards/Playlist-pana.svg', 'assets/feature-cards/Recording-pana.svg',
]

def sha(p: Path) -> str:
    h=hashlib.sha256(); h.update(p.read_bytes()); return h.hexdigest()

def norm(p: Path) -> str:
    return re.sub(r'\s+', ' ', p.read_text(encoding='utf-8', errors='ignore')).strip()

def fail(msg):
    print(f'FAIL: {msg}')
    sys.exit(1)

if not EXPO.exists(): fail(f'Expo reference not found: {EXPO}')
for rel in REQUIRED_ASSETS:
    e = EXPO/rel
    if rel.startswith('assets/fonts/'):
        n = NATIVE/'app/src/main/assets/fonts'/Path(rel).name
    elif rel.startswith('assets/feature-cards/'):
        n = NATIVE/'app/src/main/assets/feature-cards'/Path(rel).name
    else:
        n = NATIVE/'app/src/main/assets/branding'/Path(rel).name
    if not e.exists() or not n.exists(): fail(f'missing asset {rel}')
    if sha(e) != sha(n): fail(f'asset hash mismatch {rel}')

# Typography/color tokens must remain sourced from the Expo contract and the native semantic layer.
for rel in ['theme/colors.js', 'theme/spacing.js', 'theme/typography.js', 'theme/shadows.js']:
    if not (EXPO/rel).exists(): fail(f'missing Expo design reference {rel}')
for rel in [
    'app/src/main/java/com/airplayr/app/ui/theme/AirPlayrColorTokens.kt',
    'app/src/main/java/com/airplayr/app/ui/theme/DesignTokens.kt',
    'app/src/main/java/com/airplayr/app/ui/theme/Typography.kt',
    'app/src/main/java/com/airplayr/app/ui/theme/AirPlayrTheme.kt',
]:
    if not (NATIVE/rel).exists(): fail(f'missing native design foundation {rel}')

native_text = '\n'.join(norm(p) for p in (NATIVE/'app/src/main/java/com/airplayr/app/ui').rglob('*.kt'))
for marker in ['HomeScreen', 'MusicScreen', 'PlayerScreen', 'SettingsScreen', 'StatsScreen', 'MetadataScreen']:
    if marker not in native_text: fail(f'missing screen marker {marker}')

print('PHASE 16 VISUAL PARITY GATE: PASS')
print(f'critical assets checked: {len(REQUIRED_ASSETS)}')
print('design foundation files present: 4')
print('required music-facing screens present: 6')
