# Running-Health Wear (Wear OS)

Wear OS app that captures exercise telemetry and exchanges command/ack messages with the Android phone bridge.

## Platform constraint

Android-only watch support:
- This app targets Wear OS and Android Data Layer APIs.
- Integration is designed for the Android phone host in `mobile/`.

## Wire contracts

Use these exact path constants end-to-end:

```txt
/running-health/update
/running-health/command
/running-health/ack
```

## Key files

- `app/src/main/java/com/wear/exercise/ExerciseForegroundService.kt` - telemetry generation and update publishing.
- `app/src/main/java/com/wear/wearable/PhoneCommandListenerService.kt` - command intake and ack response.
- `app/src/main/java/com/wear/data/DataLayerPaths.kt` - canonical path constants.
- `app/src/main/java/com/wear/data/HealthPayload.kt` - telemetry payload schema.
- `app/src/main/java/com/wear/data/WatchCommand.kt` - command and ack types.

## Run commands

```bash
./gradlew build
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lint
./gradlew test
./gradlew connectedAndroidTest
./gradlew :app:test
```
