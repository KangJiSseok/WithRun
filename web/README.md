# Running-Health Web (Vite + React)

Web dashboard rendered inside the mobile app WebView for the watch bridge flow.

## Platform constraint

Android-only watch support:
Android-only watch support depends on the Android host app WebView bridge.
- Watch integration depends on the Android mobile bridge.
- The primary runtime target is the mobile WebView on Android.

## Wire contracts

Use these exact path constants end-to-end:

```txt
/running-health/update
/running-health/command
/running-health/ack
```

## Key files

- `src/hooks/use-watch-bridge.ts` - receives bridge messages, normalizes payloads, sends commands.
- `src/types/watch-bridge.ts` - bridge payload and ack typing/validation.
- `src/components/` - UI rendering of telemetry and command interactions.

## Run commands

```bash
npm install
npm run dev
npm run build
npm run preview
npm run lint
```

## Dev runtime notes

- The mobile emulator loads this dev server via `http://10.0.2.2:5173`.
- In production, the web bundle is loaded by mobile from `file:///android_asset/watch-web/index.html`.
