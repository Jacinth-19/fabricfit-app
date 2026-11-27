#!/usr/bin/env bash
# Capture genuine FabricFit app UI on the GitHub Actions Android emulator.
# Run from FabricFit/ after :app:assembleDebug has completed.
set -euo pipefail

PKG="com.fabricfit.app.debug"
ACTIVITY="com.fabricfit.app.MainActivity"
OUT_DIR="${1:-../qa-artifacts/screens}"
APK="app/build/outputs/apk/debug/app-debug.apk"

mkdir -p "$OUT_DIR"
test -s "$APK" || { echo "Debug APK not found: $APK" >&2; exit 1; }

# Fix the virtual device dimensions so captures stay comparable across runs.
adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put system screen_off_timeout 600000
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true

adb install -r "$APK"
adb shell pm clear "$PKG"
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true
adb shell am force-stop "$PKG" || true
adb shell am start -W -n "$PKG/$ACTIVITY"
sleep 6 # allow the branded splash and first composition to finish

dump_ui() {
  adb shell uiautomator dump /sdcard/fabricfit-window.xml >/dev/null 2>&1 || true
  adb shell cat /sdcard/fabricfit-window.xml 2>/dev/null | tr -d '\r'
}

find_bounds() {
  local label="$1" xml="$2"
  python3 -c '
import re
import sys
import xml.etree.ElementTree as ET

label = sys.argv[1]
try:
    root = ET.parse(sys.stdin).getroot()
except ET.ParseError:
    raise SystemExit(0)

matches = []
for node in root.iter("node"):
    if node.get("text") != label and node.get("content-desc") != label:
        continue
    bounds = node.get("bounds", "")
    match = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", bounds)
    if match:
        x1, y1, x2, y2 = map(int, match.groups())
        matches.append((node.get("clickable") == "true", x1, y1, x2, y2))

# Prefer the clickable merged Compose semantics node over its child Text node.
if matches:
    clickable = [item for item in matches if item[0]]
    _, x1, y1, x2, y2 = (clickable or matches)[0]
    print(f"{(x1 + x2) // 2} {(y1 + y2) // 2}")
' "$label" <<< "$xml"
}

tap_label() {
  local label="$1" attempt xml point
  for attempt in $(seq 1 15); do
    xml="$(dump_ui)"
    point="$(find_bounds "$label" "$xml")"
    if [[ -n "$point" ]]; then
      # shellcheck disable=SC2086
      adb shell input tap $point
      echo "Tapped: $label ($point)"
      sleep 2
      return 0
    fi
    sleep 1
  done
  echo "Could not find tappable UI label: $label" >&2
  return 1
}

# Block until `label` is no longer present (or the timeout expires).
#
# The onboarding screens animate out and the first frame of the destination can take a few
# seconds on a software-rendered emulator. Capturing on a fixed sleep raced it: `home.png`
# came out as a second copy of the welcome page, and the README shipped that for a month.
wait_for_label_gone() {
  local label="$1" attempt xml
  for attempt in $(seq 1 20); do
    xml="$(dump_ui)"
    if [[ -z "$(find_bounds "$label" "$xml")" ]]; then
      echo "Gone: $label (after ${attempt}s)"
      return 0
    fi
    sleep 1
  done
  echo "Label still present after timeout: $label" >&2
  return 1
}

capture() {
  local name="$1"
  adb shell screencap -p "/sdcard/fabricfit-${name}.png"
  adb pull "/sdcard/fabricfit-${name}.png" "$OUT_DIR/${name}.png"
  echo "Captured: $OUT_DIR/${name}.png"
}

# Capture the Heritage Luxe Welcome screen first, then skip to Home.
capture welcome
tap_label "Skip for now"
# Do not capture on a bare sleep: wait until onboarding is actually off screen.
wait_for_label_gone "Skip for now" || true
capture home

tap_label "Discover"
wait_for_label_gone "Featured Fabrics" || true
capture discover

tap_label "Camera"
tap_label "Got it" || true # dismiss the first-run Create coach mark
sleep 3 # the camera preview needs a moment to start on the emulator
capture create

tap_label "Try-On"
tap_label "Got it" || true # dismiss the first-run Favourites coach mark
capture favourites

tap_label "Profile"
wait_for_label_gone "Scan Results" || true
capture profile
tap_label "My Fit Closet"
wait_for_label_gone "Saved Styles" || true
sleep 1
capture my-fit-closet

printf 'Captured screens:\n'
ls -lh "$OUT_DIR"
