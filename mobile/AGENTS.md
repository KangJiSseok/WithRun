# mobile/AGENTS.md

## OVERVIEW

`mobile/` is the Android phone host that bridges Wear OS data to the web runtime inside `react-native-webview`.

Android-only watch support is required in this surface because native bridge code uses Android Wearable APIs.

## WHERE TO LOOK

- `app/(tabs)/watch.tsx` - WebView host, bridge envelope injection, command handoff.
- `hooks/use-watch-native-events.ts` - native event subscription and command dispatch wrapper.
- `modules/wear-health/index.ts` - JS interface for `WearHealth` native module.
- `android/app/src/main/java/com/wear/wearable/WearDataListenerService.kt` - update/ack ingress.
- `android/app/src/main/java/com/wear/wearable/WearHealthModule.kt` - command send to watch.
- `android/app/src/main/java/com/wear/wearable/WearDataPaths.kt` - phone-side path constants.

## CONTRACTS

Literal path constants must match watch and web docs:
- `/running-health/update`
- `/running-health/command`
- `/running-health/ack`

## ANTI-PATTERNS

- Do not change path literals in one surface only.
- Do not claim iOS watch integration support for this bridge.
- Do not assume a repository script bundles web assets into `android_asset/watch-web`.
