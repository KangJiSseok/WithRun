# MOBILE MODULE GUIDE

Scope: mobile-specific rules only. Follow `../AGENTS.md` for shared repo workflow and cross-project boundaries.

## OVERVIEW
`mobile/` is an Expo native shell around the web app. The important responsibilities here are WebView boot, deep links, device permissions, GPS/cadence capture, and phone/watch bridge behavior.

## STRUCTURE
```text
mobile/
├── index.ts                 # Expo JS entry; start here only for app boot failures
├── src/App.tsx              # Thin handoff root; almost everything delegates immediately
├── src/screens/webview/     # Real mobile app shell: WebView boot, deep links, device-to-web forwarding
├── src/bridge/              # Mobile-owned bridge contract and native/webview wiring
├── src/utils/               # Native-shell support logic such as sensor/GPS helpers
├── android/                 # Android-only integrations: watch bridge, native listeners, platform config
├── ios/                     # iOS project boundary; touch only for platform-native concerns
├── app.json                 # Expo config + deep-link scheme ownership
└── eas.json                 # Real build profile and release workflow config
```

This is not a multi-screen RN product app. The important ownership seam is the WebView shell plus the bridge that feeds native capabilities into the web experience.

## WHERE TO LOOK
- Full mobile boot path: `index.ts -> src/App.tsx -> src/screens/webview/WebViewScreen.tsx`
- OAuth/deep-link callback behavior: `src/screens/webview/WebViewScreen.tsx` plus `app.json`
- Web content loads or syncs incorrectly on mobile: start in `src/screens/webview/`, not `web/` routes or native folders
- Shared bridge payload and method changes: `src/bridge/types.ts`, `src/bridge/bridge.ts`, then review `../web/src/bridge/`
- GPS, cadence, watch, or device-to-web forwarding: `src/bridge/hooks/` and `src/screens/webview/`
- Native Android phone/watch transport or background delivery: `android/app/src/main/java/com/mobile/`
- Platform deep-link scheme/config: `app.json`, then platform manifests/projects if needed

## CONVENTIONS
- Real entry chain is `index.ts -> src/App.tsx -> src/screens/webview/WebViewScreen.tsx`.
- There is no active Expo Router structure here. Do not assume `app/` route files.
- `@/*` maps to `src/*`.
- Product screen UI usually belongs in `web/`, not `mobile/`.
- `src/bridge/types.ts` is mobile-owned shared contract surface consumed by `web/`.
- `bridge.ts` and `types.ts` must stay in sync.

## BUILD / TOOLING
- Use `pnpm` here in practice (`pnpm-lock.yaml` + `.npmrc` are checked in).
- `pnpm start` uses `expo start --dev-client`.
- EAS profiles in `eas.json` are the real mobile build workflow; do not invent local lint/test/build scripts that are not declared.

## ANTI-PATTERNS
- Do not treat this as a native-screen-first product app.
- Do not search `mobile/` for product screen ownership first; most user-facing UI still lives in `web/`.
- Do not assume `src/screens/` is a broad RN screen tree; the WebView host is the important screen.
- Do not move web-owned UI flows into React Native just because they appear on mobile.
- Do not break the `withrun://auth/callback` deep link contract.
- Do not change bridge payload shapes casually; `web/` depends on them.
- Do not look in `android/` for app-flow logic unless the issue is native bridge, watch transport, or platform integration.
- Do not assume `npm run lint` or `npm test` exist here.

## COMMANDS
```bash
cd mobile && pnpm install
cd mobile && pnpm start
cd mobile && pnpm android
cd mobile && pnpm ios
cd mobile && pnpm web
```

## NOTES
- Android native code here includes the phone-side watch receiver and bridge package.
- If a change spans `mobile/src/bridge` and `web/src/bridge`, review both sides together.
