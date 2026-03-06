# wear/AGENTS.md

## OVERVIEW

`wear/` is the Wear OS producer/consumer side of the bridge: telemetry updates out, commands in, acknowledgements back.

Android-only watch support is inherent to this module because it targets Wear OS + Android Data Layer APIs. Runtime requires the paired Android phone host bridge in `mobile/`.

## WHERE TO LOOK

- `app/src/main/java/com/wear/exercise/ExerciseForegroundService.kt` - gathers exercise metrics and emits updates.
- `app/src/main/java/com/wear/wearable/PhoneCommandListenerService.kt` - receives commands and sends ACK/PONG.
- `app/src/main/java/com/wear/data/DataLayerPaths.kt` - canonical path constants.
- `app/src/main/java/com/wear/data/HealthPayload.kt` - telemetry payload model.
- `app/src/main/java/com/wear/data/WatchCommand.kt` - command and ack model.

## CONTRACTS

Literal path constants must match phone and web docs:
- `/running-health/update`
- `/running-health/command`
- `/running-health/ack`

## ANTI-PATTERNS

- Do not rename message paths without synchronized mobile + web updates.
- Do not document non-Wear platforms as supported for this app.
- Do not add unverified runtime claims that are not backed by `app/src/main/` implementation.
