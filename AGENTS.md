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

---

## Figma Design System Rules

### Scope

- Treat `web/` as the primary product UI codebase. The mobile app ships these screens inside a React Native WebView shell.
- Default to mobile-first layouts sized for `375px` to `430px` wide viewports.
- Only implement in `mobile/` when the request is about native shell behavior, device permissions, or bridge UX. Otherwise build screen UI in `web/src/**`.

### Visual Direction

- Use a bright illustrated running-companion style: cheerful, outdoorsy, energetic, and approachable.
- Treat the app as a mobile companion product, not a desktop dashboard or a marketing site.
- Use a `soft claymorphism-lite` direction: soft depth, rounded shells, warm highlights, and gentle layering without toy-like heavy borders.
- Prefer light surfaces and vivid accents over dark dashboards or enterprise-looking UI.
- Keep each screen to one primary brand color plus at most two support accents.
- Favor warm white surfaces, bright orange primary actions, clear sky-blue support highlights, sun-tinted accents, and deep navy-gray text.
- Treat green as a semantic success color, not as a competing core brand accent.
- Avoid black-heavy UI, neon cyberpunk palettes, hard neubrutalist outlines, glassmorphism-first styling, or dense desktop-style information layouts for entry flows.
- Treat illustrations as part of the product language rather than as disposable decoration.

### Tokens and Styling

- Use the design tokens in `web/src/shared/styles/tokens/` for colors, typography, spacing, radius, shadow, and z-index.
- Use HSL channel tokens only. Do not hardcode hex or RGB values in component class names or inline styles.
- Route shadcn variables through `web/src/shared/styles/shadcn.css`. If Figma introduces a new semantic color, add it to the token files first and then map it through the shadcn bridge.
- Use Tailwind v4 utilities backed by tokens via `web/src/shared/styles/index.css`.
- Preserve the `@` path alias for `web/src/**`.
- Keep `Pretendard Variable` as the base UI typeface unless the user explicitly asks for a typography migration.
- If Figma exploration suggests playful display fonts, use that as mood reference for headlines only. Do not replace the core app type system without a deliberate token update.
  - Recommended palette direction for new Figma work:
    - Primary action: bright orange
    - Support accent: clear sky blue
    - Warm highlight: soft yellow
    - Base background: cream or warm off-white
    - Main text: deep navy-gray
  - When updating tokens later, move toward this palette through semantic variables instead of one-off screen colors.

### Component Organization

- Reuse low-level primitives from `web/src/shared/components/shadcn/` before creating new controls.
- Place reusable app-wide UI in `web/src/shared/components/`.
- Place screen-specific compositions in `web/src/features/<Feature>/`.
- Keep route wrappers thin in `web/src/pages/**` and put actual UI structure in feature components.
- Reuse `MainLayout`, navigation shells, and shared wrappers before introducing new page chrome.

### Mobile App Layout Rules

- Design for thumb reach and app-like flows. Prefer bottom sheets, stacked cards, large buttons, and single-column layouts.
- Keep primary actions full-width and close to the lower interaction zone when the flow allows it.
- Keep touch targets at least `44x44px`.
- Keep at least `8px` spacing between adjacent touch targets.
- Do not introduce desktop-only patterns such as top mega-nav, hover-only reveals, sidebars, or dense multi-column forms in core mobile flows.
- Assume screens run inside a WebView. Keep transitions, scroll behavior, and input handling smooth and lightweight.
- Do not rely on hover-only meaning. All primary interactions must read clearly on touch devices.

### Illustration Rules

- Build scenes from simple shapes, soft gradients, rounded geometry, and slightly tinted outlines rather than harsh black strokes.
- Keep mascot, landscape, and motion language consistent across onboarding, empty states, and celebratory surfaces.
- Use one hero illustration or character stage per screen. Do not let illustration compete with the primary CTA or form.
- Use illustration colors from the token palette instead of one-off art-only colors.
- If an illustration needs a new accent, promote that accent into tokens first.
- Let orange and sky blue lead the illustration language, with yellow used only as a small warmth accent.
- Avoid full toy-like claymorphism with `3px` to `4px` borders everywhere. Borrow the softness, not the exaggerated thickness.

### Motion Rules

- Use quick, friendly motion: `180ms` to `240ms` for standard transitions and spring motion for mascot or celebratory moments.
- Use motion to guide progress, confirm state changes, or support scene transitions.
- Avoid long ambient loops, heavy parallax, or motion that distracts during data entry.
- Respect reduced-motion when adding large animated scenes.
- Use soft press feedback and small depth changes rather than large scaling that shifts layout.

### Icons and Assets

- Use `lucide-react` as the default icon system for functional UI.
- Do not add new icon packages for Figma work unless the user explicitly requests it.
- Store build-time imported assets in `web/src/assets/`.
- Use `web/public/` only for assets that must be served by URL.
- If the Figma MCP payload provides SVG or image assets, use those assets directly and keep filenames descriptive.

### Figma MCP Workflow

1. Run `get_design_context` for the exact node first.
2. If the payload is too large, run `get_metadata`, narrow the target, then run `get_design_context` again.
3. Run `get_screenshot` for visual parity.
4. Translate the result into this codebase's patterns: React 19, Tailwind v4, shadcn primitives, token CSS, and mobile-first layouts.
5. Reuse existing components and layout shells before creating new structures.
6. Validate the final implementation against the screenshot and against `375px` to `430px` viewport behavior before finishing.
7. When a Figma concept conflicts with this project, prefer:
   mobile companion app rules over generic landing-page guidance
   bright illustration over dark app-store aesthetics
   Pretendard and current token architecture over imported font systems

### Screen Guidance for Onboarding and Entry Flows

- Prefer bottom-sheet composition like the current onboarding and login flows rather than centered desktop cards.
- Use short copy, one primary action, and visible progress.
- Keep form steps sequential and calm. Reduce background motion once the user is typing or selecting options.
- Use illustration to reinforce emotion and context, not to explain the form.
- For onboarding, borrow the friendliness of Claymorphism and soft rounded systems, but keep controls production-ready and accessible.

### Quality Gates

- Run `npm run lint` in `web/` after Figma-driven UI edits.
- Preserve the existing React Router structure in `web/src/routes/**`.
- Maintain accessible contrast even when using bright palettes.
- Never bypass tokens for quick visual matching.
