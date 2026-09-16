# AirPlayr Phases 16–18 — Device Validation

This document is the final physical-device gate. Static source checks cannot prove Android rendering, audio output, Bluetooth controls, ANR behavior, or MediaStore permission dialogs.

## Phase 16 — Visual

Run `tools/parity/capture_ui_goldens.sh` with an Android phone connected by ADB. Capture the required screens at the same device resolution used for the Expo reference. Compare:

- home shell and header
- feature cards
- music list/grid
- player artwork/control hierarchy
- EQ
- speed & pitch
- sleep timer
- lyrics
- settings
- drawer
- mini-player/dock

Pay particular attention to exact icon geometry, typography, spacing, surface/container colors, elevation, sheet radius, glass treatment, and safe-area placement.

## Phase 17 — Interaction

Exercise the Expo action inventory on-device:

- bottom dock navigation
- mini-player open
- drawer open/drag-dismiss
- song tap/long-press/favorite/more
- player seek/previous/play/next/shuffle/repeat
- lyrics line seek
- sheet open/dismiss/backdrop dismiss/swipe dismiss
- settings selection and persistence after process restart
- metadata save and library refresh
- Android Share/Delete/SAF/Open-With flows

## Phase 18 — Playback torture

Use `tools/playback/adb_torture_test.sh` for lifecycle/log capture while manually executing:

- play/pause repeatedly
- rapid seek
- next/previous churn
- queue changes
- shuffle/repeat changes
- lock/unlock
- background/foreground
- force-stop/relaunch
- notification controls
- Bluetooth/headset disconnect/reconnect
- incoming interruption if available
- EQ and speed/pitch changes during playback
- sleep timer during playback

Release gate: no crash, no ANR, no unexpected playback stop, correct queue restoration, correct notification/session state, and no progressive memory/CPU instability.
