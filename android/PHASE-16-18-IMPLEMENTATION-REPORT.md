# AirPlayr Android Native — Phases 16–18

Reference: Expo `AirPlayr-user-visible-fixed-2026-09-14.zip`
Baseline: Android Native Milestone 5
Video-player UI remains intentionally excluded per project scope.

## Phase 16 — Visual parity
- Added stable Compose test tags for Home, Music, Player, Settings, Drawer/Dock/mini-player and core player actions.
- Added a static visual parity gate comparing critical Expo branding/font assets, design foundation presence, and music-facing screen inventory.
- Added an ADB screenshot capture harness for device golden collection.
- Kept the existing AirPlayr Material/dynamic theme and custom icon foundation intact.

## Phase 17 — Interaction parity
- Added a machine-readable interaction contract listing root/music routes, player actions, More-sheet actions and settings domains.
- Added deterministic parity tests for the route/action contract.
- Preserved the single Media3 playback controller/session architecture.
- Added stable semantics/test tags to key controls so Compose UI automation can identify them without text matching.

## Phase 18 — Playback torture
- Added `PlaybackHealthMonitor` to the playback service. It records stalls, player errors, media transitions and seek events without blindly auto-restarting playback.
- The monitor is attached to the authoritative service player and released with the player lifecycle.
- Added deterministic torture scenarios for repeated seeking and queue churn.
- Added an ADB lifecycle/log capture script for repeated foreground/background/relaunch stress and media-session inspection.

## Verification
- Phase 16 visual parity static gate: PASS
- Phase 17 interaction parity static gate: PASS
- Phase 18 playback torture static gate: PASS
- Python tooling syntax: PASS
- Existing Phases 0–15 gates: rechecked below and must remain PASS
- ZIP integrity: verified after packaging

## Honest limitation
A static environment cannot certify physical Android rendering, actual audio playback, Bluetooth/headset behavior, notification controls, ANR freedom, or device-specific DSP. The supplied device validation scripts are the final execution path for those checks.
