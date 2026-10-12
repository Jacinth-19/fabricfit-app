#!/usr/bin/env bash
# Capture real device screenshots of the Heritage Luxe screens for the README.
# Runs inside the CI emulator (1080x1920 @420dpi); pure adb, no instrumentation.
set -eux
mkdir -p qa-artifacts/screens

PKG=com.ntoma.studio.debug
ACT=com.ntoma.studio.MainActivity

adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put system screen_off_timeout 600000
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard || true

# Pre-grant so no permission dialogs photobomb the captures.
adb shell pm grant "$PKG" android.permission.CAMERA || true
adb shell pm grant "$PKG" android.permission.POST_NOTIFICATIONS || true

app_apk=$(find app/build/outputs/apk/debug -name 'app-debug.apk' -print -quit)
test -n "$app_apk"
adb install -r "$app_apk"

shot() { # name
  adb shell screencap -p "/sdcard/$1.png"
  adb pull "/sdcard/$1.png" "qa-artifacts/screens/$1.png"
  echo "captured $1"
}

# Tap the centre of the UI node whose text or content-desc equals $1.
tap_node() { # label
  local label="$1" attempt xml line nums x1 y1 x2 y2
  for attempt in 1 2 3 4; do
    adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1 || true
    xml=$(adb shell cat /sdcard/ui.xml 2>/dev/null)
    line=$(printf '%s' "$xml" | tr '>' '\n' \
      | grep -F "=\"$label\"" \
      | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | head -1)
    if [ -n "$line" ]; then
      nums=$(printf '%s' "$line" | grep -o '[0-9]\+')
      # shellcheck disable=SC2086
      set -- $nums
      x1=$1; y1=$2; x2=$3; y2=$4
      adb shell input tap $(( (x1 + x2) / 2 )) $(( (y1 + y2) / 2 ))
      echo "tapped '$label' at centre $(( (x1 + x2) / 2 )),$(( (y1 + y2) / 2 ))"
      sleep 2
      return 0
    fi
    sleep 1
  done
  echo "!! could not locate '$label' in UI hierarchy"
  return 1
}

adb shell am start -W -n "$PKG/$ACT" >/dev/null 2>&1 \
  || adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 6
shot 01_onboarding

tap_node "Skip for now"
sleep 2
shot 02_home

tap_node "Camera"        # raised centre camera tab
sleep 4
shot 03_capture_fabric
adb shell input keyevent KEYCODE_BACK
sleep 2

tap_node "Discover"
sleep 2
shot 04_discover

tap_node "Try On"
sleep 2
shot 05_tryon

tap_node "Profile"
sleep 2
shot 06_profile

adb logcat -d > qa-artifacts/logcat.txt || true
echo "screens:" && ls -la qa-artifacts/screens
