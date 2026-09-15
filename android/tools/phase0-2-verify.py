#!/usr/bin/env python3
from pathlib import Path
import hashlib, re, sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT if (ROOT / "app").exists() else ROOT / "android"
MAIN = ANDROID / "app/src/main/java/com/airplayr/app/MainActivity.kt"
THEME = ANDROID / "app/src/main/java/com/airplayr/app/ui/theme/AirPlayrTheme.kt"
TYPO = ANDROID / "app/src/main/java/com/airplayr/app/ui/theme/Typography.kt"
TOKENS = ANDROID / "app/src/main/java/com/airplayr/app/ui/theme/DesignTokens.kt"
COLORS = ANDROID / "app/src/main/java/com/airplayr/app/ui/theme/AirPlayrColorTokens.kt"
GLASS = ANDROID / "app/src/main/java/com/airplayr/app/ui/components/LiquidGlass.kt"
STARTUP = ANDROID / "app/src/main/java/com/airplayr/app/ui/permissions/StartupPermissionFlow.kt"
GRADLE = ANDROID / "app/build.gradle.kts"
MANIFEST = ANDROID / "app/src/main/AndroidManifest.xml"

def sha256(p):
    h = hashlib.sha256()
    with p.open("rb") as f:
        for chunk in iter(lambda: f.read(1024*1024), b""):
            h.update(chunk)
    return h.hexdigest()

def must(cond, msg, failures):
    print(("PASS " if cond else "FAIL ") + msg)
    if not cond: failures.append(msg)

failures=[]
hash_file=ROOT/"PHASE-0-REFERENCE-ASSET-HASHES.txt"
must(hash_file.exists(), "reference asset hash manifest exists", failures)
if hash_file.exists():
    for line in hash_file.read_text(encoding="utf-8").splitlines():
        if not line.strip(): continue
        expected, rel = line.split(maxsplit=1)
        if rel in {"assets/icon.png","assets/favicon.png","assets/splash-icon.png","assets/adaptive-icon.png"}:
            mapped = {
                "assets/icon.png": "android/app/src/main/assets/branding/icon.png",
                "assets/favicon.png": "android/app/src/main/assets/branding/favicon.png",
                "assets/splash-icon.png": "android/app/src/main/assets/branding/splash-icon.png",
                "assets/adaptive-icon.png": "android/app/src/main/assets/branding/adaptive-icon.png",
            }[rel]
            p = ANDROID / mapped.split("android/",1)[-1] if mapped.startswith("android/") else ROOT / mapped
        else:
            p = ANDROID/"app/src/main"/rel
        must(p.exists(), f"reference asset present: {rel}", failures)
        if p.exists():
            must(sha256(p)==expected, f"reference asset SHA-256 exact: {rel}", failures)

main=MAIN.read_text(encoding="utf-8")
gradle=GRADLE.read_text(encoding="utf-8")
manifest=MANIFEST.read_text(encoding="utf-8")
theme=THEME.read_text(encoding="utf-8")
typo=TYPO.read_text(encoding="utf-8")
tokens=TOKENS.read_text(encoding="utf-8")
colors=COLORS.read_text(encoding="utf-8")
glass=GLASS.read_text(encoding="utf-8")
startup=STARTUP.read_text(encoding="utf-8")

must("installSplashScreen()" in main, "splash is installed before super.onCreate", failures)
must(main.rfind("installSplashScreen()") < main.index("super.onCreate(savedInstanceState)"), "splash installation call precedes super.onCreate", failures)
must('applicationId = "com.airplayr.app"' in gradle, "application ID remains com.airplayr.app", failures)
must('versionName = "1.0.0"' in gradle, "native version matches Expo 1.0.0", failures)
must('android:theme="@style/Theme.App.SplashScreen"' in manifest, "launcher/activity use splash theme", failures)
must('windowSplashScreenBackground' in (ANDROID/"app/src/main/res/values/styles.xml").read_text(), "splash background configured", failures)
must('windowSplashScreenAnimatedIcon' in (ANDROID/"app/src/main/res/values/styles.xml").read_text(), "splash icon configured", failures)
must("LaunchedEffect(Unit)" in startup, "startup permission flow executes once per composition", failures)
for marker in ["Step.NEARBY","Step.NOTIFICATIONS","Step.AUDIO","Step.VIDEO","Step.COMPLETE"]:
    must(marker in startup, f"startup step present: {marker}", failures)
must("LiquidGlassSurface" not in startup, "no extra in-app permission card overlays the Expo system-dialog flow", failures)
for marker in ["AirPlayrColors","AirPlayrSpacing","AirPlayrRadii","AirPlayrElevation"]:
    must(marker in tokens or marker in colors, f"design token layer contains {marker}", failures)
fonts_text=(ANDROID/"app/src/main/java/com/airplayr/app/ui/theme/Fonts.kt").read_text()
for marker in ["OpenSans","SmoochSans","Oswald","DancingScript","ShadowsIntoLight","CaacupeOne"]:
    must(marker in typo or marker in fonts_text, f"font family contract present: {marker}", failures)
for marker in ["primary","surfaceContainerHighest","outlineVariant","accentSoft","trackMax","pillActive"]:
    must(marker in colors, f"semantic color token present: {marker}", failures)
for marker in ["backgroundBlurRadiusDp","surfaceAlpha","borderAlpha","shadow"]:
    must(marker in glass, f"glass contract present: {marker}", failures)
must("LocalAirPlayrColors provides airColors" in theme, "full AirPlayr semantic palette is supplied to Compose", failures)
must('delay(2000L)' in (ANDROID/"app/src/main/java/com/airplayr/app/ui/AirPlayrApp.kt").read_text(), "What's New timing matches Expo post-permission delay", failures)

# Ensure no Expo runtime was introduced into native application tree.
for bad in ["package.json","node_modules","expo-router","react-native"]:
    matches=list((ANDROID/"app").rglob(bad))
    must(not matches, f"native app contains no Expo/React runtime artifact: {bad}", failures)

print("\\nRESULT:", "PASS" if not failures else f"FAIL ({len(failures)} issues)")
sys.exit(0 if not failures else 1)
