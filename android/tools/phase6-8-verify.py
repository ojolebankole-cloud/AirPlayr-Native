from pathlib import Path
import re, sys
ROOT = Path(__file__).resolve().parents[1]
FAIL = []

def check(ok, msg):
    print(('PASS ' if ok else 'FAIL ') + msg)
    if not ok: FAIL.append(msg)

def read(rel):
    p = ROOT / rel
    if not p.exists():
        check(False, f'missing {rel}')
        return ''
    return p.read_text(encoding='utf-8')

# Phase 6 — EQ lifecycle
fx = read('app/src/main/java/com/airplayr/app/core/playback/dsp/AudioEffectsEngine.kt')
svc = read('app/src/main/java/com/airplayr/app/core/playback/service/AirPlayrPlaybackService.kt')
check('sessionId == audioSessionId && hasAnyEffect()' in fx,
      'EQ attachment is idempotent for the same Android audio session')
attach_block = re.search(r'fun attach\(audioSessionId: Int\): EffectInfo \{(.*?)\n    \}', fx, re.S)
check(attach_block is not None and 'release()' in attach_block.group(1) and 'hasAnyEffect()' in attach_block.group(1),
      'new audio sessions release old effects before recreation')
check('fun applyState(state: DspState): EffectInfo' in fx and 'if (!hasAnyEffect()) return info()' in fx,
      'DSP state application does not recreate effects')
check('localPlayer.addListener(localAudioSessionListener)' in svc and 'localEffects.attach(audioSessionId)' in svc,
      'EQ follows the real local ExoPlayer audio-session lifecycle')
check('secondaryPlayer.addListener(secondaryAudioSessionListener)' in svc and 'secondaryEffects.attach(audioSessionId)' in svc,
      'EQ follows the secondary crossfade ExoPlayer audio-session lifecycle')
collector = svc[svc.find('dsp.state.collect'): svc.find('override fun onGetSession')]
check('applyDspTo(localPlayer, state)' in collector and 'applyDspTo(secondaryPlayer, state)' in collector,
      'saved DSP state is reapplied to both playback slots when the service observes/restores DSP state')
check('localPlayer.removeListener(localAudioSessionListener)' in svc and 'secondaryPlayer.removeListener(secondaryAudioSessionListener)' in svc and 'localEffects.release()' in svc and 'secondaryEffects.release()' in svc,
      'EQ listeners/effects are cleaned up on service destruction')

# Phase 7 — Android media system
manifest = read('app/src/main/AndroidManifest.xml')
check('androidx.media3.session.MediaButtonReceiver' in manifest and 'android.intent.action.MEDIA_BUTTON' in manifest,
      'Media3 MediaButtonReceiver is registered for external media buttons')
check('android:foregroundServiceType="mediaPlayback"' in manifest and 'FOREGROUND_SERVICE_MEDIA_PLAYBACK' in manifest,
      'playback service has media-playback foreground-service contract')
check(': MediaLibraryService()' in svc or ': MediaLibraryService' in svc,
      'single Media3 MediaLibraryService owns playback/session')
check('override fun onPlaybackResumption(' in svc and 'MediaItemsWithStartPosition' in svc,
      'Media3 playback resumption callback restores the saved queue/position')
check('setMediaNotificationProvider(' in svc and 'DefaultMediaNotificationProvider.Builder' in svc,
      'Media3 automatic MediaStyle notification provider is configured')
check('setChannelId(MEDIA_NOTIFICATION_CHANNEL_ID)' in svc and 'setNotificationId(MEDIA_NOTIFICATION_ID)' in svc,
      'media notification channel/id are stable and configured')
check('queueStore.read()' in svc and 'saved?.positionMs' in svc and 'saved?.shuffle' in svc and 'saved?.repeatMode' in svc,
      'system resumption reads position, queue and playback mode from persisted state')
check('setSessionActivity(sessionActivity)' in svc,
      'notification tap opens the existing MainActivity')

# Phase 8 — Material You / dynamic theme
theme = read('app/src/main/java/com/airplayr/app/ui/theme/AirPlayrTheme.kt')
tokens = read('app/src/main/java/com/airplayr/app/ui/theme/AirPlayrColorTokens.kt')
check('dynamicLightColorScheme(context)' in theme and 'dynamicDarkColorScheme(context)' in theme,
      'Android 12+ system dynamic palettes are used')
check('Build.VERSION.SDK_INT >= Build.VERSION_CODES.S' in theme,
      'dynamic colors are gated to Android 12/S+')
check('Lifecycle.Event.ON_RESUME' in theme and 'refreshTick++' in theme,
      'dynamic palette is refreshed when the app becomes active')
check('textPrimary = scheme.onSurface' in tokens and 'textSecondary = scheme.onSurfaceVariant' in tokens and 'textMuted = scheme.outline' in tokens,
      'text semantics follow Material onSurface/onSurfaceVariant/outline')
check('primaryContainer = scheme.primaryContainer' in tokens and 'secondaryContainer = scheme.secondaryContainer' in tokens and 'tertiaryContainer = scheme.tertiaryContainer' in tokens,
      'container roles preserve the system semantic palette')
check('LocalAirPlayrColors provides airColors' in theme,
      'AirPlayr semantic colors are provided to the whole Compose tree')

# Combined static integrity
check(not ('import androidx.media3.session.MediaSession\nimport androidx.media3.session.MediaSession' in svc),
      'no duplicate MediaSession import remains')

if FAIL:
    print(f'\nRESULT: FAIL ({len(FAIL)} checks)')
    sys.exit(1)
print('\nRESULT: PASS')
