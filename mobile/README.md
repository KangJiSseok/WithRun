# Running-Health Mobile (Expo)

React Native Android host that bridges Wear OS data to the embedded web UI.

## Platform constraint

Android-only watch support:
- The Wear bridge module depends on Android Wearable APIs.
- The watch screen is gated by `Platform.OS === "android"`.

## Wire contracts

Use these exact path constants end-to-end:

```txt
/running-health/update
/running-health/command
/running-health/ack
```

## Key files

- `app/(tabs)/watch.tsx` - WebView host, JS bridge injection, command forwarding.
- `hooks/use-watch-native-events.ts` - typed native event state and `sendCommand()` wrapper.
- `modules/wear-health/index.ts` - native module and event emitter bindings.
- `android/app/src/main/java/com/wear/wearable/WearDataListenerService.kt` - update/ack ingress.
- `android/app/src/main/java/com/wear/wearable/WearHealthModule.kt` - command egress.

## Run commands

```bash
npm install
npm start
npm run android
npm run ios
npm run web
npm run lint
```

## WebView source behavior

- Dev URL (emulator): `http://10.0.2.2:5173`
- Prod URL: `file:///android_asset/watch-web/index.html`
- The repository does not include a script that copies web build output into `android_asset/watch-web`; that packaging step is manual.
