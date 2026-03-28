# PROJECT KNOWLEDGE BASE

**Generated:** 2026-03-28 16:25:54 KST
**Commit:** 18ab4db
**Branch:** chore/agent.md

## OVERVIEW
Running companion monorepo. Active product surfaces are `web/` (primary UI), `mobile/` (Expo native shell around the web app), `was/` (Spring Boot backend), and `watch/` (Wear OS sensor app).

## STRUCTURE
```text
S14P21D202/
├── web/                  # Real product screen ownership; default home for UI work
├── mobile/               # Native shell around web; device/deep-link/bridge ownership
├── was/                  # Backend business logic and API ownership by domain
├── watch/                # Wear runtime; health capture and phone messaging ownership
├── deploy/               # Deployed environment topology; compose/nginx only
├── infra/local/valhalla/ # Local routing bootstrap; not deploy config ownership
├── DB/                   # Schema contract and DB intent docs, not app logic
├── docs/                 # Mixed analysis/how-to/history docs; mostly reference material
└── wear/                 # Stale placeholder; ignore unless revived as a real module
```

Read this tree as ownership, not just storage. `web/`, `mobile/`, `was/`, and `watch/` are the active execution surfaces; the rest are support, ops, or reference areas.

## WHERE TO LOOK
| Task | Location | Notes |
|---|---|---|
| Product screen UI | `web/src/features/` | `pages/` are mostly thin route wrappers |
| Web routing/auth gates | `web/src/routes/` | `routes.tsx`, `publicRoutes.tsx`, `privateRoutes.tsx`, `AuthGuards.tsx` |
| Shared styling/tokens | `web/src/shared/styles/` | Tailwind v4 + token bridge |
| Phone ↔ web bridge types | `mobile/src/bridge/` | `web` builds against these types via `@bridge` |
| Native mobile shell | `mobile/src/screens/webview/` | WebView boot, deep links, bridge sync |
| Backend business logic | `was/src/main/java/kr/withrun/was/domain/` | Main domains: auth, course, navigation, running, user |
| Backend cross-cutting config | `was/src/main/java/kr/withrun/was/global/` | security, filters, response envelope, config |
| Wear runtime | `watch/app/src/main/java/com/mobile/watch/` | `MainActivity`, `ExerciseService` |
| Deployment | `deploy/` | Compose + nginx; operational boundary |
| Local routing infra | `infra/local/valhalla/` | Local bootstrap only; do not treat data artifacts as code |
| DB contract | `DB/schema.sql`, `DB/SCHEMA_GUIDE.md` | Schema source-of-truth + rationale |

Use the child `AGENTS.md` when you already know the module. Use this root file when you need to choose the right module first.

## CODE MAP
| Symbol | Type | Location | Role |
|---|---|---|---|
| `queryClient` | constant | `web/src/main.tsx` | Web app bootstraps React Query here |
| `router` | router | `web/src/routes/routes.tsx` | Merges public/private route trees |
| `WasApplication` | class | `was/src/main/java/kr/withrun/was/WasApplication.java` | Spring Boot entrypoint |
| `MainActivity` | class | `watch/app/src/main/java/com/mobile/watch/MainActivity.kt` | Wear launcher activity |
| `ExerciseService` | class | `watch/app/src/main/java/com/mobile/watch/ExerciseService.kt` | Foreground health collection + phone messaging |

## CONVENTIONS
- Root guidance is for shared repo workflow only. Module-specific rules live in `web/AGENTS.md`, `mobile/AGENTS.md`, `was/AGENTS.md`, and `watch/AGENTS.md`.
- Use `pnpm` in `web/` and `mobile/`. There is no root package manager workspace.
- `web/` is the default home for product UI. Only implement in `mobile/` when the task is native shell behavior, device permissions, sensors, deep links, or phone/watch bridging.
- `watch/` is the real wearable module. Ignore `wear/` unless it becomes a real project again.
- `web` depends on `mobile/src/bridge/types.ts` via the `@bridge` alias. Bridge contract changes are cross-project changes.
- `was/` is an active top-level service. Do not rely on older summaries that describe the repo as only mobile/web/wear.

## ANTI-PATTERNS (THIS PROJECT)
- Do not suppress type errors or commit without explicit instruction.
- Do not treat `mobile/` as an Expo Router app; the real entry is `mobile/index.ts -> mobile/src/App.tsx -> WebViewScreen`.
- Do not treat `wear/` as the wearable codebase; current code lives in `watch/`.
- Do not put product screen work in `mobile/` when the real UI belongs in `web/`.
- Do not bypass token-based styling in `web/` with one-off hex/RGB values.
- Do not modify `deploy/compose/*`, `deploy/nginx/*`, or `.gitlab-ci.yml` while doing local Valhalla bootstrap work in `infra/local/valhalla/`.
- Do not treat `infra/local/valhalla/custom_files/**` or tile directories as code modules.

## UNIQUE STYLES
- `web/` uses a bright mobile-first running-companion visual system with HSL semantic tokens, Tailwind v4, and shadcn bridging.
- `mobile/` is intentionally thin: native shell + bridge + device integrations around the web app.
- `was/` is package-by-domain with repeated `repository/query` seams and substantial mirrored tests under `src/test`.
- `watch/` is a small, focused Wear app with direct Health Services + Data Layer responsibilities.

## COMMANDS
```bash
# web
cd web && pnpm install && pnpm lint && pnpm build

# mobile
cd mobile && pnpm install && pnpm start
cd mobile && pnpm android
cd mobile && pnpm ios

# backend
cd was && ./gradlew build
cd was && ./gradlew test

# watch (use local Gradle / Android Studio; no wrapper script is checked in)
cd watch && gradle :app:compileDebugKotlin
cd watch && gradle :app:assembleDebug
```

## NOTES
- GitLab CI lives in `.gitlab-ci.yml`; no GitHub Actions workflow was found.
- Root README says `main`, but GitLab CI deploy rules target `master`; treat that branch mismatch as a repo gotcha.
- `deploy/`, `infra/local/valhalla/`, `DB/`, and `docs/` inherit this root file plus their local README/docs for now. They do not get their own `AGENTS.md` unless edit frequency or local rules materially grow.
- Child `AGENTS.md` files should contain subtree-specific deltas only; do not repeat this root file.
