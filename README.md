<p align="center">
  <img src="store/feature-graphic-1024x500.png" alt="Akshar Blocks – English and Hindi letters, बारहखड़ी and numbers, learned by playing" width="720">
</p>

<h1 align="center">Akshar Blocks</h1>

<p align="center">
  An offline Android learning app that teaches children aged 3–7 <b>English ABC</b>, <b>Hindi letters</b> (स्वर, व्यंजन, बारहखड़ी) and <b>numbers</b> through play.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Android-24%2B-3DDC84?logo=android&logoColor=white" alt="Android 7.0+">
  <img src="https://img.shields.io/badge/offline-100%25-2E7BFF" alt="Works offline">
  <a href="https://github.com/abhishekjahazi/akshar-blocks/actions/workflows/tests.yml"><img src="https://github.com/abhishekjahazi/akshar-blocks/actions/workflows/tests.yml/badge.svg" alt="Tests"></a>
  <img src="https://img.shields.io/badge/unit%20tests-81-22B35E" alt="81 unit tests">
</p>

---

## Screenshots

| Home: 7 learning tracks | Game modes for a track | Tracing with stroke order |
|:---:|:---:|:---:|
| <img src="docs/screenshots/1-home.png" alt="Home screen with seven learning tracks" width="230"> | <img src="docs/screenshots/2-games.png" alt="English track with its game modes" width="230"> | <img src="docs/screenshots/3-trace.png" alt="Tracing the letter A with numbered strokes" width="230"> |
| **First words: build s-u-n** | **Memory: find the pairs** | **बारहखड़ी: add the vowel sign** |
| <img src="docs/screenshots/4-words.png" alt="Building the word sun from letter blocks" width="230"> | <img src="docs/screenshots/5-memory.png" alt="Memory game with two cards turned over" width="230"> | <img src="docs/screenshots/6-barakhadi.png" alt="Barakhadi game: ष plus which vowel sign makes षू" width="230"> |
| **Today's games: a daily path** | **Report card for parents** | **Daily play-time limit** |
| <img src="docs/screenshots/7-todays-games.png" alt="Today's games: four steps, one ticked" width="230"> | <img src="docs/screenshots/8-report-card.png" alt="Report card with weekly play chart and letter map" width="230"> | <img src="docs/screenshots/9-rest.png" alt="Time to rest screen with a moon" width="230"> |

## Features

**Seven learning tracks**

| Track | What the child learns |
|---|---|
| English ABC | Capital letters A–Z with a picture for each (A for Apple) |
| Small letters | a–z, matched to their capitals |
| स्वर (Swar) | Hindi vowels अ – अः |
| व्यंजन (Vyanjan) | Hindi consonants क – ज्ञ |
| बारहखड़ी (Barakhadi) | Consonant + vowel sign (क का कि की …) |
| Numbers | 1–100, with counting games |
| गिनती (Ginti) | Hindi numerals १–१०० |

**Nine game modes**: Learn, Trace (follow the strokes with a finger), Find it, Balloon pop, Match the picture, Memory pairs, First words (build c-a-t or ज-ल from letter blocks), Count, and Build a syllable (बारहखड़ी).

**Voice in English and Hindi.** Every instruction and letter name is spoken. 1,100+ pre-recorded clips ship with the app, and text-to-speech covers anything else, so it works with no internet.

**Today's games.** One big button on Home starts a short daily path of four games picked from the child's progress: Learn while letters are new, then Trace, plus a practice game that changes every day. English, Hindi and numbers take turns, and the next section opens once the first is half learned.

**Learns with the child.** Adaptive practice brings back letters a child gets wrong, and tracks which letters get mixed up (b ↔ d, ब ↔ व).

**Rewards.** Stars, a sticker album (a new sticker every 5 stars) and a daily streak.

**Parent area**, behind a typed maths question so children can't open it:
- Up to 4 child profiles, each with their own progress.
- A daily play-time limit (15–60 minutes), with a friendly "time to rest" screen.
- A **report card** for each child: level per track, a colour-coded letter map, a weekly play-time chart, letters that need practice, and a Share button.

**Private by design.** No ads, no accounts, no internet permission, no analytics. All progress stays on the device and backups are turned off. The app follows Google Play's Families policy.

**Phones and tablets**, in portrait and landscape.

## Tech stack

| | |
|---|---|
| Language | Kotlin |
| Platform | Android SDK (min API 24, target API 37), Android Gradle Plugin 9 |
| UI | Custom `Canvas` game engine (`GameView`) with animation, plus Material Components for the parent area |
| Audio | `MediaPlayer` for bundled voice clips, Android `TextToSpeech` as fallback, `AudioTrack` for sound effects generated in code |
| Content | Plain TSV files for letters, tracing strokes and words, so new content needs no code changes |
| Art | [Noto Emoji](https://github.com/googlefonts/noto-emoji) images (Apache 2.0), bundled for consistent look on every device |
| Testing | JUnit: 81 unit tests covering content, tracing, rewards, reports, the daily path, play-time limits and voice clips |
| Tools | Android Studio, Gradle, Claude Code (AI-assisted development) |

## Project structure

```
app/src/main/
├── java/com/aksharblocks/app/
│   ├── MainActivity.kt        # Screen navigation (home, tracks, games, rest)
│   ├── GameView.kt            # Base game engine: drawing, animation, taps
│   ├── LearnView.kt, TraceView.kt, FindView.kt, BalloonView.kt,
│   │   MatchView.kt, MemoryView.kt, WordsView.kt, CountView.kt,
│   │   BarakhadiView.kt       # One class per game mode
│   ├── Alphabet.kt, Lang.kt   # Tracks, letters, and English/Hindi phrases
│   ├── Voice.kt, Speaker.kt   # Bundled voice clips with TTS fallback
│   ├── Profiles.kt, Rewards.kt, PlayTime.kt
│   ├── ParentActivity.kt, ParentGate.kt, ReportCard.kt
│   └── ...
└── assets/
    ├── tracks/     # Letters and pictures for each track (TSV)
    ├── tracing/    # Stroke paths for tracing (TSV)
    ├── words/      # Word lists for "First words" (TSV)
    ├── art/        # Noto Emoji images
    └── voice/      # English and Hindi voice clips (.m4a)
```

## Build and run

Requirements: Android Studio (latest), JDK 17+, an Android device or emulator on Android 7.0+.

```bash
git clone https://github.com/abhishekjahazi/akshar-blocks.git
cd akshar-blocks

./gradlew installDebug        # build and install on a connected device
./gradlew testDebugUnitTest   # run the unit tests
```

Release builds need a signing key in `keystore.properties`, which is not part of this repository.

## Status

Version 1.0, being prepared for release on Google Play.

## Credits

- Emoji art: [Noto Emoji](https://github.com/googlefonts/noto-emoji) by Google, Apache License 2.0 (see `app/src/main/assets/art/LICENSE-noto-emoji.txt`).
- Voices: generated with Google's on-device text-to-speech voices.

---

<p align="center">Made by <a href="https://github.com/abhishekjahazi">Abhisek Jahaji</a></p>
