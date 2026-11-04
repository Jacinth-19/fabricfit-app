#!/usr/bin/env bash
# Force Android's visible three-button SystemUI navigation bar on an emulator.
set -euo pipefail

adb wait-for-device
adb shell settings put secure navigation_mode 0

# AOSP API 33 can retain the gesture overlay after the Settings value changes.
# Select the three-button overlay explicitly; the fallback supports images that
# do not expose the SystemUI overlay category to the shell user.
adb shell cmd overlay enable-exclusive \
  --category com.android.internal.systemui.navbar \
  com.android.internal.systemui.navbar.threebutton || \
  adb shell cmd overlay enable com.android.internal.systemui.navbar.threebutton || true

# Restart SystemUI so both its window insets and visible controls use the new mode.
adb shell am force-stop com.android.systemui || true
sleep 4

actual_mode="$(adb shell settings get secure navigation_mode | tr -d '\r')"
if [[ "$actual_mode" != "0" ]]; then
  echo "Expected Android navigation_mode=0, got: $actual_mode" >&2
  exit 1
fi

echo "Android three-button navigation enabled (navigation_mode=$actual_mode)."
