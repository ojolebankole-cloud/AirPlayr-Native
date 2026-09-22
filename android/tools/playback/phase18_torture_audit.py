#!/usr/bin/env python3
"""Final playback torture/static robustness audit."""
from __future__ import annotations
from pathlib import Path
import sys, re
ROOT = Path(__file__).resolve().parents[2]
SRC = ROOT/'app/src/main/java/com/airplayr/app'
service = (SRC/'core/playback/service/AirPlayrPlaybackService.kt').read_text(encoding='utf-8')
controller = (SRC/'ui/playback/PlaybackController.kt').read_text(encoding='utf-8')
monitor = (SRC/'core/qa/PlaybackHealthMonitor.kt').read_text(encoding='utf-8')
checks = {
    'MediaLibrarySession': 'MediaLibrarySession' in service,
    'MediaButtonReceiver': 'MediaButtonReceiver' in (ROOT/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8'),
    'playback resumption': 'onPlaybackResumption' in service,
    'queue persistence': 'persistSnapshot' in service and 'restoreQueue' in service,
    'actual seek API': 'seekTo(positionMs' in controller,
    'health monitor': 'PlaybackHealthMonitor' in monitor,
    'player error accounting': 'onPlayerError' in monitor,
    'seek accounting': 'DISCONTINUITY_REASON_SEEK' in monitor,
    'service cleanup': 'override fun onDestroy()' in service and 'effects.release()' in service,
}
failed = [k for k,v in checks.items() if not v]
if failed:
    print('FAIL:', ', '.join(failed)); sys.exit(1)
print('PHASE 18 PLAYBACK TORTURE STATIC GATE: PASS')
for k in checks: print('PASS:', k)
print('NOTE: hardware playback/ANR tests still require a real Android device.')
