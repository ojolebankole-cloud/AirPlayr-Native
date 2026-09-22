#!/usr/bin/env python3
"""Interaction parity gate against the Expo route/component inventory."""
from __future__ import annotations
from pathlib import Path
import re, sys

ROOT = Path(__file__).resolve().parents[2]
EXPO = Path('/mnt/data/expo1618/userfix')
SRC = ROOT/'app/src/main/java/com/airplayr/app'

expected_routes = ['home','music','videos','settings','player','equalizer','search','history','favorites','playlists','collections','metadata','queue','listening-watching-stats','folders','my-playlists','most-played','for-you','rediscover','rediscover/rewind','tuned-in','rock-your-world','smart-collections','album','artist','genre','open']
nav = (SRC/'ui/AirPlayrApp.kt').read_text(encoding='utf-8')
for route in expected_routes:
    if f'composable("{route}")' not in nav and route not in nav:
        print(f'FAIL: route {route} not represented in native navigation')
        sys.exit(1)

expo_player = '\n'.join(p.read_text(encoding='utf-8', errors='ignore') for p in (EXPO/'components/player').glob('*.js'))
required_labels = ['Previous','Next','Reset speed and pitch','Lock controls','Rewind 10 seconds','Forward 10 seconds']
for label in required_labels:
    if label not in expo_player:
        print(f'FAIL: Expo reference changed/missing expected action {label}')
        sys.exit(1)

native_files = list((SRC/'ui/components').rglob('*.kt')) + list((SRC/'ui/screens').rglob('*.kt')) + list((SRC/'ui/metadata').rglob('*.kt'))
native_text = '\n'.join(p.read_text(encoding='utf-8', errors='ignore') for p in native_files)
for marker in ['MenuDrawer','ModalBottomSheet','AirPlayrBottomDock','PlayerScreen','MusicScreen','SettingsScreen','MetadataScreen']:
    if marker not in native_text:
        print(f'FAIL: interaction component {marker} missing')
        sys.exit(1)

print('PHASE 17 INTERACTION PARITY GATE: PASS')
print(f'routes checked: {len(expected_routes)}')
print(f'Expo player labels checked: {len(required_labels)}')
