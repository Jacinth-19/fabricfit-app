#!/usr/bin/env bash
set -euo pipefail
mkdir -p qa-artifacts
adb shell wm size 1080x1920
adb shell wm density 420
adb shell settings put system screen_off_timeout 600000
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell input keyevent KEYCODE_HOME
# Reuse the APKs already compiled and tested by the verify job.
app_apk=$(find ci-build -name app-debug.apk -print -quit)
test_apk=$(find ci-build -name '*androidTest.apk' -print -quit)
test -n "$app_apk" && test -n "$test_apk"
adb install -r "$app_apk"
adb install -r "$test_apk"
# Actual emulator capture; no simulated app screens.
adb shell screenrecord --size 1080x1920 --bit-rate 4000000 --time-limit 180 /sdcard/ntoma-journey.mp4 &
recording_pid=$!
set +e
adb shell am instrument -w -r -e class com.ntoma.studio.FabricJourneyDeviceTest com.ntoma.studio.debug.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee qa-artifacts/device-diagnostics.txt
result=${PIPESTATUS[0]}
# am instrument may return zero for assertion failures: require JUnit's success marker.
if ! grep -q 'OK (1 test)' qa-artifacts/device-diagnostics.txt; then result=1; fi
set -e
adb shell pkill -2 screenrecord || true
wait "$recording_pid" || true
adb pull /sdcard/ntoma-journey.mp4 qa-artifacts/demo-raw.mp4 || true
adb pull /sdcard/Android/data/com.ntoma.studio.debug/files/journey/. qa-artifacts/ || true
adb logcat -d > qa-artifacts/logcat.txt
exit "$result"
