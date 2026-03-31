# OMO Review Pipeline

## Overview

This repository includes an OMO-native port of the core mafia code-review harness workflow. The pipeline is built from repo-local skills in `.opencode/skills/` and thin command entrypoints in `.opencode/commands/`.

## Available Commands

- `/review-pipeline-auto`
- `/review-pipeline-write-intent`
- `/review-pipeline-gen-criteria`
- `/review-pipeline-create-pr-body`
- `/review-pipeline-review`
- `/review-pipeline-reflect-review`

## Standalone Maintenance Command

- `/review-pipeline-update-docs [convention|adr]`

This command is separate from the core review pipeline. Use it when project conventions or ADR records need maintenance.

## Required Source Documents

The pipeline expects these repo documents to exist:

- `docs/code-convention.yaml`
- `docs/adr.yaml`
- root `AGENTS.md`
- module `AGENTS.md` files when relevant to changed files

## Artifact Location

All generated artifacts are stored under the current branch directory:

```text
.review-artifacts/{branch-name}/
├── design-intent.md
├── code-quality-guide.md
├── pr-body.md
└── review-comments.md
```

`{branch-name}` is resolved from `git branch --show-current`.

## Base Branch Rule

The pipeline should always confirm the base branch explicitly before using `git diff <base>...HEAD`. Do not assume silently, especially in this repository where branch history may vary by workflow.

## Execution Model

- `write-intent`, `gen-criteria`, `create-pr-body`, and `review` are read-heavy stages.
- `reflect-review` is the only core phase that may modify source code.
- Validation should target the touched ownership surfaces:
  - `web`, `mobile` → relevant `pnpm` checks
  - `was`, `watch` → relevant Gradle checks

## Recommended Flow

1. Run `/review-pipeline-auto`
2. Confirm the base branch
3. Review the generated intent, criteria, and PR artifacts as needed
4. Let `review` generate grounded comments
5. Use `reflect-review` to confirm accept or reject judgments and apply approved fixes

## Documentation Maintenance Flow

`/review-pipeline-update-docs` is intentionally kept outside the core `auto` pipeline.

- `convention` → focuses on `docs/code-convention.yaml`
- `adr` → focuses on `docs/adr.yaml`
- no argument → inspects both

The command always reads both documents first, proposes MUST and RECOMMENDED changes, waits for user confirmation, and only then edits the files.
