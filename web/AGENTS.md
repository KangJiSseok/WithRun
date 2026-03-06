# web/AGENTS.md

## OVERVIEW

`web/` renders the watch dashboard UI and runs inside the Android mobile WebView host.

Android-only watch integration depends on the Android host app WebView bridge for command and telemetry transport.

## WHERE TO LOOK

- `src/hooks/use-watch-bridge.ts` - inbound bridge event handling and outbound command postMessage.
- `src/types/watch-bridge.ts` - payload and ack shape definitions.
- `src/components/` - telemetry and command UI surfaces.

## CONTRACTS

Literal path constants must stay aligned across docs and implementation:
- `/running-health/update`
- `/running-health/command`
- `/running-health/ack`

## ANTI-PATTERNS

- Do not treat this as a standalone mobile browser flow; primary runtime is the mobile WebView.
- Do not alter bridge message shapes in UI code without updating mobile and wear contracts.
- Do not invent automation around mobile asset packaging in this project docs.
