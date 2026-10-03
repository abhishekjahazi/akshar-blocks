#!/usr/bin/env bash
# Smoke test for a release build on an emulator (run by CI; see .github/workflows/tests.yml).
# Installs the APK, starts the app, checks it is still running, then lets the Android monkey
# tap around like a small child. Fails if the app crashes or stops.
#
# 1.1 and 1.2 shipped with a crash at start that only release builds had (R8 removed
# constructors the ads SDK needs); this catches that kind of problem before a release.
set -euo pipefail

APK="${1:-app/build/outputs/apk/release/app-release.apk}"
PKG=com.aksharblocks.app

crashes() { adb logcat -d -b crash 2>/dev/null | grep -c "$PKG" || true; }
fail() {
  echo "FAILED: $1"
  adb logcat -d -b crash || true
  exit 1
}

adb install -r "$APK"
adb logcat -c || true
adb logcat -b crash -c || true

adb shell am start -W -n "$PKG/.MainActivity"
sleep 15
[ -n "$(adb shell pidof "$PKG" | tr -d '\r')" ] || fail "the app is not running 15 seconds after it started"
[ "$(crashes)" = "0" ] || fail "the app crashed at start"

# Random taps and swipes through the games. The parent area stays closed behind its question.
adb shell monkey -p "$PKG" --pct-syskeys 0 --throttle 150 -s 42 -v 600 || fail "the monkey stopped on a crash or freeze"
sleep 2
[ "$(crashes)" = "0" ] || fail "the app crashed while being played"
[ -n "$(adb shell pidof "$PKG" | tr -d '\r')" ] || fail "the app was not running after being played"

echo "OK: the release build started and survived 600 random taps."
