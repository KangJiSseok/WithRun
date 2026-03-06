
# Running-Health

Running telemetry from a Wear OS watch to a web UI, with a React Native Android host as the bridge.

Android-only watch support:
- The wearable app is Wear OS.
- The phone bridge uses Android Wearable APIs.
- The mobile app renders a WebView-based screen that is gated behind `Platform.OS === "android"`.

## Repo layout

- `mobile/`: Expo (React Native) Android host, contains the WebView screen and native Wear bridge.
- `web/`: Vite + React web app, runs inside the `mobile/` WebView.
- `wear/`: Wear OS app (Kotlin + Compose), collects Health Services metrics and talks to the phone.

## Architecture

At runtime, the web UI is not a standalone mobile browser tab. It runs inside a React Native `WebView` in `mobile/`, and receives watch telemetry through a native bridge.

### Wire contracts (Data Layer and Message API)

Literal path constants are shared across watch and phone:

```txt
/running-health/update   (telemetry, Data Layer data item)
/running-health/command  (commands, Message API message)
/running-health/ack      (ack and pong, Message API message)
```

Implementation sources:
- Phone constants: `mobile/android/app/src/main/java/com/wear/wearable/WearDataPaths.kt`
- Watch constants: `wear/app/src/main/java/com/wear/data/DataLayerPaths.kt`

Payload key used for Data Layer updates:
- `payload` (see `WearDataPaths.KEY_PAYLOAD` and `DataLayerPaths.KEY_PAYLOAD`)

### Bridge layers

- Wear OS produces metrics with Health Services and pushes them to the phone via DataClient.
- Android phone receives telemetry and ACKs in services, then emits JS events through a React Native native module.
- React Native screen injects those events into the WebView as `MessageEvent` data.
- Web UI parses those messages and renders telemetry, plus posts command JSON back to React Native.

## Component responsibilities

### wear (watch)

- `wear/app/src/main/java/com/wear/exercise/ExerciseForegroundService.kt`
  - Collects `ExerciseUpdate` data (heart rate, distance, calories, speed, pace).
  - Serializes `HealthPayload` JSON and writes it to `DataLayerPaths.HEALTH_UPDATE_PATH` (`/running-health/update`).
  - Throttles send cadence via `ExerciseRepository.sendIntervalMs`.

- `wear/app/src/main/java/com/wear/wearable/PhoneCommandListenerService.kt`
  - Receives command messages on `DataLayerPaths.COMMAND_PATH` (`/running-health/command`).
  - Executes commands and responds via MessageClient on `DataLayerPaths.ACK_PATH` (`/running-health/ack`).
  - Emits `ACK` (success or error) and `PONG` responses.

- `wear/app/src/main/java/com/wear/data/DataLayerPaths.kt`
  - Canonical constants for `/running-health/update`, `/running-health/command`, `/running-health/ack`.

- `wear/app/src/main/java/com/wear/data/WatchCommand.kt`
  - Parses incoming JSON commands.
  - Defines `WatchAck` and its JSON serialization for ACK and PONG.

- `wear/app/src/main/java/com/wear/data/HealthPayload.kt`
  - Defines the telemetry JSON schema and mapping from Health Services data points.

### mobile (phone, React Native)

- `mobile/app/(tabs)/watch.tsx`
  - Hosts the `react-native-webview` runtime.
  - Injects bridge envelopes into WebView using `injectJavaScript`.
  - Forwards web commands from `WebView.onMessage` to native via `sendCommand`.
  - WebView URL behavior:
    - Dev: `http://10.0.2.2:5173` (Android emulator alias for host localhost, physical devices must use a LAN IP URL)
    - Prod: `file:///android_asset/watch-web/index.html`

- `mobile/hooks/use-watch-native-events.ts`
  - Subscribes to native events (`WearHealthUpdate`, `WearAckReceived`, `WearConnectionStatus`).
  - Provides typed state to the screen and exposes `sendCommand()`.

- `mobile/modules/wear-health/index.ts`
  - Wraps the native module and event emitter (`WearHealth`, `WEAR_EVENTS`).

### mobile/android (phone, native bridge)

- `mobile/android/app/src/main/java/com/wear/wearable/WearDataListenerService.kt`
  - Telemetry ingress: listens for Data Layer changes on `/running-health/update`.
  - ACK ingress: listens for Message API messages on `/running-health/ack`.
  - Connection status: emits connect and disconnect events.

- `mobile/android/app/src/main/java/com/wear/wearable/WearHealthModule.kt`
  - React Native module that exposes `sendCommand()` to JS.
  - Sends Message API commands to connected wearable nodes on `/running-health/command`.

- `mobile/android/app/src/main/java/com/wear/wearable/WearBridgeStore.kt`
  - Holds a weak reference to the module and emits events into JS.

- `mobile/android/app/src/main/java/com/wear/wearable/WearDataPaths.kt`
  - Phone-side path and event constants.

### web (WebView content)

- `web/src/hooks/use-watch-bridge.ts`
  - Receives bridge messages via `window.addEventListener('message', ...)` and `document.addEventListener('message', ...)`.
  - Accepts multiple input shapes (wrapped events, direct payloads) and normalizes into typed state.
  - Sends commands via `window.ReactNativeWebView.postMessage(JSON.stringify(command))`.

- `web/src/types/watch-bridge.ts`
  - Runtime validators and shared types for payload, connection status, and ack.

## Data flow

The system has three loops: telemetry, command, and ack.

### 1) Telemetry loop (`/running-health/update`)

1. Watch collects metrics in `ExerciseForegroundService`.
2. Watch writes a Data Layer item at `/running-health/update` with `payload=<HealthPayload JSON>`.
3. Phone receives the update in `WearDataListenerService.onDataChanged()`.
4. Phone emits `WearHealthUpdate` via `WearBridgeStore` and `WearHealthModule` into React Native.
5. `mobile/hooks/use-watch-native-events.ts` stores the last payload.
6. `mobile/app/(tabs)/watch.tsx` injects `{ event: "WearHealthUpdate", data: "..." }` into the WebView.
7. `web/src/hooks/use-watch-bridge.ts` parses it and updates the web UI.

### 2) Command loop (`/running-health/command`)

1. Web UI calls `sendCommand(command)` from `useWatchBridge`.
2. Web posts JSON to the native bridge via `window.ReactNativeWebView.postMessage(...)`.
3. `mobile/app/(tabs)/watch.tsx` receives it in `WebView.onMessage` and parses it as a `WatchCommand`.
4. React Native calls `wearHealthModule.sendCommand(JSON.stringify(command))`.
5. `WearHealthModule.sendCommand()` sends a Message API message to each connected wearable node at `/running-health/command`.
6. Watch receives it in `PhoneCommandListenerService.onMessageReceived()` and runs the action.

### 3) Ack loop (`/running-health/ack`)

1. After command handling, watch replies via MessageClient on `/running-health/ack`.
2. Phone receives it in `WearDataListenerService.onMessageReceived()`.
3. Phone emits `WearAckReceived` into React Native.
4. `mobile/app/(tabs)/watch.tsx` injects `{ event: "WearAckReceived", data: "..." }` into the WebView.
5. Web parses and surfaces the ack state.

## Run methods

Commands below come from root `AGENTS.md`.

### mobile/ (Expo)

```bash
cd mobile
npm install
npm start
npm run android
npm run ios
npm run web
npm run lint
```

### web/ (Vite)

```bash
cd web
npm install
npm run dev
npm run build
npm run preview
npm run lint
```

### wear/ (Android, Kotlin)

```bash
cd wear
./gradlew build
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew lint
./gradlew test
./gradlew connectedAndroidTest
./gradlew :app:test
```

### E2E dev flow (watch -> phone -> web)

Prereqs: Android SDK + Platform Tools (`adb`), an Android emulator or physical Android device, and a Wear OS device paired to the phone.

1. Start the web dev server in one terminal: `cd web && npm install && npm run dev`.
2. In a second terminal, start the mobile Android app: `cd mobile && npm install && npm run android`.
3. Open the mobile "Watch" tab, confirm the WebView loads the dev URL (`http://10.0.2.2:5173` in emulator, or your machine's LAN IP URL on a physical device).
4. Build and install the Wear app (Android Studio, or `cd wear && ./gradlew assembleDebug` then `adb -s <watch-serial> install -r wear/app/build/outputs/apk/debug/app-debug.apk`), then run it on a Wear OS device.
5. Start an exercise on the watch, confirm telemetry updates appear in the WebView.
6. Trigger a command from the web UI, confirm an ACK or PONG returns.

## Constraints and gotchas

- Android-only watch integration: `mobile/app/(tabs)/watch.tsx` blocks non-Android platforms.
- WebView dev URL: `10.0.2.2` is emulator-only. For a physical Android device, the dev URL must point at your machine's LAN IP (same network, firewall allows inbound).
- Data Layer pairing requirement: phone and watch builds must use the same `applicationId` (and signing key), otherwise `putDataItem()` can succeed locally on watch while phone `WearableListenerService` never receives events.
- If both phone and watch emulators are connected, `wear` module `installDebug` can fail by attempting install on phone; prefer `assembleDebug` + `adb -s <watch-serial> install -r ...`.
- WebView prod URL: the screen expects a bundled asset at `file:///android_asset/watch-web/index.html`.
- WebView prod assets: there is no automation for bundling the web build into `mobile/`. After `cd web && npm run build`, copy `web/dist/*` into `mobile/android/app/src/main/assets/watch-web/`.
- Command payloads are JSON strings. Malformed messages from the web are ignored by the mobile screen.
