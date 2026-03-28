# WATCH MODULE GUIDE

Scope: watch-specific rules only. Follow `../AGENTS.md` for shared repo workflow and cross-project boundaries.

## OVERVIEW
`watch/` is the real wearable module. It is a small Android/Wear OS project responsible for collecting health metrics and sending them to the phone app over the Data Layer.

## STRUCTURE
```text
watch/
├── settings.gradle               # project wiring; only one real Android module here
├── build.gradle                  # top-level plugin wiring
├── gradle.properties             # project-wide Android/Gradle defaults
└── app/                          # all real watch runtime behavior lives here
    ├── build.gradle              # pairing-sensitive app config: signing, sdk, applicationId
    └── src/main/
        ├── AndroidManifest.xml   # permissions + service declaration that make runtime possible
        ├── java/com/mobile/watch/
        │   ├── MainActivity.kt   # launcher, UI shell, permission gate, ambient behavior
        │   └── ExerciseService.kt# actual runtime core: health session + phone messaging
        └── res/
```

This module is intentionally tiny. If you cannot find behavior, it is probably concentrated in `MainActivity.kt`, `ExerciseService.kt`, the manifest, or `app/build.gradle`.

## WHERE TO LOOK
- Start/stop UX, permission prompts, launcher flow, ambient/AOD behavior: `app/src/main/java/com/mobile/watch/MainActivity.kt`
- Health session lifecycle, sensor updates, foreground notification, and `/health-data` send loop: `app/src/main/java/com/mobile/watch/ExerciseService.kt`
- Why foreground health collection works at all: `app/src/main/AndroidManifest.xml`
- Pairing-sensitive config such as signing or `applicationId`: `app/build.gradle`
- Phone-side receiver for watch messages: not here — use `../mobile/android/app/src/main/java/com/mobile/WatchListenerService.kt`

## CONVENTIONS
- `watch/` is the active wearable project; `wear/` is not.
- Current package root is `com.mobile.watch`.
- Current toolchain is Java/Kotlin 17 with `compileSdk 35`, `targetSdk 35`, `minSdk 30`.
- Current UI stack uses `androidx.wear.compose.material`, not the older Material3 assumptions captured in the stale root file.
- The watch app is tightly coupled to the phone app via shared `applicationId`/signing expectations and `/health-data` messaging.

## ANTI-PATTERNS
- Do not update docs or code as if `wear/` were the real module.
- Do not break the phone/watch pairing assumptions in signing config or package identity.
- Do not treat `ExerciseService` as generic Android plumbing; it is the runtime core of this module.
- Do not expect a large hidden feature tree; most behavior is concentrated in two Kotlin files.
- Do not assume test or lint infrastructure exists here beyond Gradle project tasks.

## COMMANDS
```bash
# No wrapper script is checked in; use Android Studio or local Gradle
cd watch && gradle :app:compileDebugKotlin
cd watch && gradle :app:assembleDebug
```

## NOTES
- `mobile/android/app/src/main/java/com/mobile/WatchListenerService.kt` is the phone-side counterpart.
- If wearable guidance ever needs to expand, keep it here rather than reintroducing stale `wear/` instructions.
