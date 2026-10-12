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
tap() { adb shell input tap "$1" "$2"; sleep 2; }

adb shell am start -W -n "$PKG/$ACT" >/dev/null 2>&1 \
  || adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1
sleep 6
shot 01_onboarding

# Skip the pager -> Home (top-right text button)
tap 980 150
sleep 2
shot 02_home

# Bottom bar: Home(90) Discover(270) Camera(450) TryOn(630) Saved(810) Profile(990) @ y~1826
tap 450 1826
sleep 4
shot 03_capture_fabric
adb shell input keyevent KEYCODE_BACK
sleep 2

tap 270 1826
sleep 2
shot 04_discover

tap 630 1826
sleep 2
shot 05_tryon

tap 990 1826
sleep 2
shot 06_profile

adb logcat -d > qa-artifacts/logcat.txt || true
echo "screens:" && ls -la qa-artifacts/screens
