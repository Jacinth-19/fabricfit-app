#!/usr/bin/env bash
# Retry wrapper for capture_fabricfit_screens.sh.
#
# The android-emulator-runner `script` input executes ONE command per line
# (it trims, drops comments, and splits on newlines), so any multi-line shell
# logic must live in a real script file instead.
#
# The emulator occasionally fails to boot cleanly on first attempt; each
# attempt here starts from a clean app state because the capture script
# runs `pm clear` at the start.
set -uo pipefail

OUT_DIR="../qa-artifacts/screens"

rm -rf "$OUT_DIR"

# The emulator runner reports the device as ready before adb has finished attaching, and a
# capture that starts in that window dies with "device offline" or "device not found". It
# then counts as a failed attempt even though the device recovers a second later. Wait for
# adb to actually see an online device before each attempt.
wait_for_device() {
  local tries="${1:-60}"
  local i state
  for i in $(seq 1 "$tries"); do
    state="$(adb get-state 2>/dev/null || true)"
    if [ "$state" = "device" ]; then
      # get-state can report online while the boot is still finishing.
      if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
        return 0
      fi
    fi
    sleep 2
  done
  echo "adb never reported a booted device (last state: ${state:-none})" >&2
  return 1
}

attempt=1
while [ "$attempt" -le 3 ]; do
  echo "--- attempt $attempt ---"
  wait_for_device 60 || { echo "device not available for attempt $attempt" >> /tmp/capfails.log; }
  if bash ../tools/qa/capture_fabricfit_screens.sh "$OUT_DIR" > /tmp/cap.log 2>&1; then
    break
  fi
  echo "Capture attempt $attempt failed"
  {
    echo "--- attempt $attempt ---"
    tail -5 /tmp/cap.log
  } >> /tmp/capfails.log
  rm -rf "$OUT_DIR"
  adb shell am force-stop com.fabricfit.app.debug 2>/dev/null || true
  adb shell input keyevent KEYCODE_WAKEUP 2>/dev/null || true
  sleep 10
  attempt=$((attempt + 1))
done

# Total capture failure: surface the last error lines as annotations so they
# are visible on the public run page (job logs require sign-in). Only emitted
# when nothing was captured, so successful runs keep a clean annotation
# stream.
if [ ! -s "$OUT_DIR/home.png" ] && [ -f /tmp/capfails.log ]; then
  while IFS= read -r l; do
    echo "::error::capture-fail: $l"
  done < /tmp/capfails.log
fi

test -s "$OUT_DIR/home.png"