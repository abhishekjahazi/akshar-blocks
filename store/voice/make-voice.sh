#!/usr/bin/env bash
# Makes the app's built-in voice from the phone's text-to-speech.
#
# 1. With the phone connected:
#      gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aksharblocks.app.VoiceMaker
#    (says every script line on the phone and saves WAV files there)
# 2. Then run this script from the project folder:
#      FFMPEG=/path/to/ffmpeg ADB=/path/to/adb store/voice/make-voice.sh
#    It pulls the WAVs, trims silence, compresses them to small AAC (.m4a) files and puts
#    them in app/src/main/assets/voice/<en|hi>/.
set -euo pipefail
shopt -s nullglob  # a run may have new lines in only one language

FFMPEG="${FFMPEG:-ffmpeg}"
ADB="${ADB:-adb}"
WORK="${WORK:-build/voice-wav}"
OUT="app/src/main/assets/voice"

rm -rf "$WORK" && mkdir -p "$WORK"
MSYS_NO_PATHCONV=1 "$ADB" pull /sdcard/Android/data/com.aksharblocks.app.debug/files/voice-wav/. "$WORK" >/dev/null
mkdir -p "$WORK/en" "$WORK/hi" "$WORK/mr"

# Trim silence at both ends (keeping a hair of it so words aren't clipped), mono, 24 kHz,
# 40 kbps AAC: small, and clear enough for speech.
FILTER="silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.03,areverse,silenceremove=start_periods=1:start_threshold=-50dB:start_silence=0.06,areverse"

count=0
for language in en hi mr; do
  mkdir -p "$OUT/$language"
  for wav in "$WORK/$language"/*.wav; do
    name=$(basename "$wav" .wav)
    "$FFMPEG" -nostdin -y -loglevel error -i "$wav" -af "$FILTER" -ac 1 -ar 24000 -c:a aac -b:a 40k "$OUT/$language/$name.m4a"
    count=$((count + 1))
  done
done
echo "Made $count voice clips in $OUT"
du -sh "$OUT"
