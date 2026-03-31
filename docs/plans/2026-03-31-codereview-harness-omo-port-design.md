# OMO Code Review Harness Port Design

## Goal

Port the core `mafia-codereview-harness` workflow into `S14P21D202` as an OMO-native review pipeline that preserves the original review intent and artifact contract without depending on Claude Code plugins.

## Scope

### In scope

- Recreate the core 6-step workflow in OMO-native form:
  - `auto`
  - `write-intent`
  - `gen-criteria`
  - `create-pr-body`
  - `review`
  - `reflect-review`
- Use OMO-style command and skill names instead of `mafia-codereview:*`.
- Preserve artifact output under `.review-artifacts/{branch-name}/`.
- Treat the whole monorepo as the review target and derive relevant modules from changed files.
- Keep code changes restricted to the final `reflect-review` phase.
- Add `docs/code-convention.yaml` and `docs/adr.yaml` templates to this repo.

### Out of scope

- Literal Claude Code plugin packaging parity.
- Porting `.claude-plugin` metadata.
- Making `update-docs` part of the core review pipeline.
- Full automation of branch creation or PR creation.

## Command and Skill Surface

The port will expose thin OMO commands and reusable OMO skills.

### Planned commands

- `review-pipeline-auto`
- `review-pipeline-write-intent`
- `review-pipeline-gen-criteria`
- `review-pipeline-create-pr-body`
- `review-pipeline-review`
- `review-pipeline-reflect-review`

### Planned skills

- `.opencode/skills/review-pipeline-auto/SKILL.md`
- `.opencode/skills/review-pipeline-write-intent/SKILL.md`
- `.opencode/skills/review-pipeline-gen-criteria/SKILL.md`
- `.opencode/skills/review-pipeline-create-pr-body/SKILL.md`
- `.opencode/skills/review-pipeline-review/SKILL.md`
- `.opencode/skills/review-pipeline-reflect-review/SKILL.md`

The commands are only entrypoints. The actual behavior and reusable logic live in the skills.

## Artifact Contract

Artifacts remain compatible with the original harness shape.

```text
.review-artifacts/{branch-name}/
├── design-intent.md
├── code-quality-guide.md
├── pr-body.md
└── review-comments.md
```

`{branch-name}` is resolved from `git branch --show-current`.

## Workflow Design

### 1. `review-pipeline-auto`

- Confirms current branch and base branch.
- Collects `git diff <base>...HEAD --stat`.
- Creates `.review-artifacts/{branch-name}/`.
- Orchestrates the 5 downstream stages in order.
- Reports final artifact list and terminal status.

### 2. `review-pipeline-write-intent`

- Reads current conversation context and changed-file context.
- Produces `design-intent.md` using the original document structure.
- Surfaces ambiguities for user confirmation before finalizing.

### 3. `review-pipeline-gen-criteria`

- Reads `docs/code-convention.yaml` and `docs/adr.yaml`.
- Reads root and module `AGENTS.md` files as additional local constraints.
- Delegates ADR extraction to a subagent.
- Produces `code-quality-guide.md` containing:
  - common standards
  - change-relevant ADRs
  - task-specific application notes

### 4. `review-pipeline-create-pr-body`

- Uses `git diff <base>...HEAD` and changed-file context.
- Produces `pr-body.md` with summary, changes, breaking changes, test plan, and related context.

### 5. `review-pipeline-review`

- Runs automatically without user confirmation once upstream artifacts exist.
- Inputs:
  - `design-intent.md`
  - `code-quality-guide.md`
  - `pr-body.md`
  - `git diff <base>...HEAD`
- Produces `review-comments.md`.
- Review comments must be grounded in documented criteria, not personal taste.

### 6. `review-pipeline-reflect-review`

- Reads `review-comments.md` and classifies each comment.
- Presents accept/reject reasoning to the user.
- After confirmation, applies code changes.
- Runs validation appropriate to the touched modules.
- Updates `review-comments.md` with explicit judgments.

## Execution Model

The original harness uses frequent Claude fork/worktree stages. The OMO port will preserve the safety intent while simplifying default execution.

- `write-intent`, `gen-criteria`, `create-pr-body`, and `review` are read-heavy stages.
- `reflect-review` is the only phase allowed to modify application code.
- Worktrees may still be used when a stage benefits from isolation, but they are not mandatory for every phase.
- Validation is module-aware:
  - `web/` and `mobile/` → `pnpm` validation
  - `was/` → Gradle validation
  - `watch/` → Gradle validation

## Repository-Specific Adaptations

- The target repo is a monorepo with active surfaces in `web/`, `mobile/`, `was/`, and `watch/`.
- Review scope is determined from changed files, not from a user-picked single module.
- Root `AGENTS.md` selects domain ownership; child `AGENTS.md` files provide module-specific constraints.
- The original default base branch is `main`, but this repo has branch-history inconsistencies; the command should always confirm the base branch instead of assuming silently.

## Non-Core Command Handling

`update-docs` exists in the original repository but is not part of the 6-step auto pipeline. This port intentionally excludes it from the core workflow design. It can be added later as a separate maintenance command if needed.

## Risks and Mitigations

- **Risk: Claude-specific wording copied too literally**
  - Translate all plugin, fork, and sub-agent language into OMO-native commands, skills, and delegation.
- **Risk: review quality becomes opinionated**
  - Force all review output to cite `code-quality-guide.md` or explicit design intent.
- **Risk: monorepo validation becomes too expensive**
  - Validate only the changed ownership surfaces, not every project in the repo.
- **Risk: base branch mismatch produces misleading artifacts**
  - Ask for explicit base-branch confirmation during `auto`.

## Acceptance Criteria

- OMO-native commands and skills exist for the 6 core stages.
- The pipeline writes artifacts to `.review-artifacts/{branch-name}/`.
- The pipeline consumes `docs/code-convention.yaml` and `docs/adr.yaml`.
- The review stage produces grounded comments with priorities and side-effect notes.
- The reflect stage can update code and append judgments to `review-comments.md`.
- The design remains compatible with the repo’s existing `.opencode` and `AGENTS.md` structure.
