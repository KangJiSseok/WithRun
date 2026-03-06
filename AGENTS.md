# AGENTS.md — Running-Health

Monorepo with three sub-projects:
- `mobile/` — Expo (React Native) app, TypeScript, file-based routing (expo-router)
- `web/` — Vite + React 19 + TypeScript, React Compiler enabled
- `wear/` — Android Wear OS app, Kotlin + Jetpack Compose

---

## Commands

### mobile/ (Expo / React Native)

```bash
cd mobile
npm install          # install deps
npm start            # start Expo dev server (interactive)
npm run android      # launch on Android emulator
npm run ios          # launch on iOS simulator
npm run web          # launch on web browser
npm run lint         # run expo lint (ESLint via eslint-config-expo)
```

No test runner is configured yet. When added, it will likely be Jest:
```bash
npm test                          # all tests
npm test -- --testPathPattern=foo # single file
```

### web/ (Vite + React)

```bash
cd web
npm install          # install deps
npm run dev          # Vite dev server (http://localhost:5173)
npm run build        # tsc -b && vite build
npm run preview      # preview production build
npm run lint         # ESLint (typescript-eslint + react-hooks + react-refresh)
```

No test runner is configured yet. When added (e.g., Vitest):
```bash
npm test                          # all tests
npm test -- --reporter=verbose foo.test.ts  # single file
```

### wear/ (Android / Kotlin)

```bash
cd wear
./gradlew build                   # full build
./gradlew assembleDebug           # build debug APK
./gradlew assembleRelease         # build release APK
./gradlew lint                    # Android lint
./gradlew test                    # JVM unit tests
./gradlew connectedAndroidTest    # instrumented tests (device/emulator required)
./gradlew :app:test               # unit tests for :app module only
```

---

## TypeScript / React Style (mobile + web)

### General TypeScript
- **Strict mode** is enabled (`"strict": true`) in mobile. Never suppress errors with `as any`, `@ts-ignore`, or `@ts-expect-error`.
- Use `type` for props and local types; `interface` is acceptable only when extending is needed.
- Prefer explicit return types on exported functions and hooks.
- `ecmaVersion: 2020` in the web ESLint config.

### Imports
- Sort imports: third-party packages → internal absolute paths (`@/`) → relative paths.
- mobile uses the path alias `@/*` → project root (e.g. `@/hooks/use-color-scheme`).
- web does not configure path aliases — use relative imports.
- Group React-Native or Expo imports before generic JS/TS imports.

```ts
// mobile — correct import order
import { StyleSheet, Text, type TextProps } from 'react-native';

import { useThemeColor } from '@/hooks/use-theme-color';
```

### File & Directory Naming
- **Kebab-case** for all TypeScript/TSX files: `themed-text.tsx`, `use-color-scheme.ts`, `haptic-tab.tsx`.
- Expo Router uses parenthesised directory groups: `(tabs)/`.
- React Native screen/component files live in `components/` or `app/`.

### Components
- Export as **named function declarations** (not arrow functions) for components:
  ```tsx
  export function ThemedText({ style, ...rest }: ThemedTextProps) { ... }
  ```
- Default exports only for Expo Router page/layout files:
  ```tsx
  export default function RootLayout() { ... }
  ```
- Props types are defined directly above the component using `type`:
  ```ts
  export type ThemedTextProps = TextProps & {
    lightColor?: string;
    darkColor?: string;
    type?: 'default' | 'title';
  };
  ```

### Hooks
- File names: `use-foo.ts` (kebab-case).
- Hook names: `useFoo` (camelCase).
- Platform-specific hooks use `.web.ts` suffix: `use-color-scheme.web.ts`.

### Styling (mobile)
- Use `StyleSheet.create({})` for all React Native styles — no inline style objects.
- Colour constants live in `constants/theme.ts` (`Colors`, `Fonts`).
- Support light/dark mode through `useThemeColor`; never hard-code colour literals in components.

### Styling (web)
- Plain CSS files (`App.css`, `index.css`) co-located with components or in `src/`.
- Vite + React Compiler are enabled; avoid patterns that break memoisation (e.g. creating callbacks/objects inline in JSX props without `useCallback`/`useMemo`).

### Error Handling
- No empty `catch` blocks.
- Surface errors to the user or log with context; never silently swallow exceptions.

---

## Kotlin / Wear OS Style (wear/)

### Package Structure
```
com.wear.presentation        ← Activities & top-level Composables
com.wear.presentation.theme  ← Theme, colours, typography
```

### Naming Conventions
- **PascalCase** for `@Composable` functions and classes: `WearApp`, `WearTheme`.
- **camelCase** for variables, functions, parameters.
- **UPPER_SNAKE_CASE** for constants.
- Preview composables: annotate with `@WearPreviewDevices` + `@WearPreviewFontScales`.

### Compose Patterns
- Use Wear Compose Material3 (`androidx.wear.compose.material3`) — not the phone Material3 library.
- Scaffold pattern: `AppScaffold` → `ScreenScaffold` → `TransformingLazyColumn`.
- Theme wrapper: always wrap root content in `WearTheme { ... }`.
- Pass `Modifier` as a parameter (do not hard-code modifiers inside composables).

### Imports
- Use explicit imports (no wildcard `import foo.*` except in generated code).
- Group: Android SDK → Compose → Wear Compose → project-local.

### Build
- Kotlin compile target: JVM 11 (`JavaVersion.VERSION_11`).
- `minSdk = 36`, `targetSdk = 36` (Wear OS).
- Version catalog lives in `gradle/libs.versions.toml` — add new dependencies there, not inline.

---

## Linting

| Project | Config | Run |
|---------|--------|-----|
| mobile  | `mobile/eslint.config.js` (eslint-config-expo/flat) | `npm run lint` in `mobile/` |
| web     | `web/eslint.config.js` (typescript-eslint + react-hooks) | `npm run lint` in `web/` |
| wear    | `wear/app/lint.xml` | `./gradlew lint` in `wear/` |

ESLint enforces: recommended TypeScript rules, React Hooks rules, React Refresh rules. Fix all lint errors before committing — warnings are acceptable but should not increase.

---

## Key Constraints for AI Agents

- **Never commit** unless explicitly asked.
- **Never suppress type errors** (`as any`, `@ts-ignore`, `@ts-expect-error`).
- **Never delete failing tests** to make a suite pass.
- When editing mobile or web files, run `npm run lint` in the relevant subdirectory and resolve all new errors.
- When editing Kotlin files, run `./gradlew :app:compileDebugKotlin` to check for compile errors.
- The project is early-stage (greenfield); no test infrastructure exists yet — do not assume tests are running.
- Each sub-project (`mobile/`, `web/`, `wear/`) manages its own dependencies independently; do not mix them.
