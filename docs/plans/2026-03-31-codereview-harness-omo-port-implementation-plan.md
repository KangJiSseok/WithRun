# OMO Code Review Harness Port Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build an OMO-native version of the core mafia codereview harness workflow inside `S14P21D202`.

**Architecture:** The port will use `.opencode/skills/` for the real workflow logic and `.opencode/commands/` as thin entrypoints. It will preserve the original `.review-artifacts/{branch}/` contract while adapting Claude-specific plugin behavior into OMO-native delegation and validation.

**Tech Stack:** OMO/OpenCode markdown commands, OMO skills, repo-local docs in YAML/Markdown, git diff based review workflow.

---

### Task 1: Add repository review source documents

**Files:**
- Create: `docs/code-convention.yaml`
- Create: `docs/adr.yaml`
- Reference: `C:\Users\SSAFY\Desktop\새 폴더\mafia-codereview-harness\plugin\docs\code-convention.yaml`
- Reference: `C:\Users\SSAFY\Desktop\새 폴더\mafia-codereview-harness\plugin\docs\adr.yaml`

**Step 1: Read the source templates**

Read the two source YAML files from the cloned harness.

**Step 2: Create local copies**

Copy them into `S14P21D202/docs/` with only minimal wording changes needed for this repo.

**Step 3: Verify the files exist**

Read both new files back from `docs/` and confirm the expected schema is preserved.

**Step 4: Do not commit**

Do not create a git commit unless the user explicitly asks.

---

### Task 2: Create the write-intent skill and command

**Files:**
- Create: `.opencode/skills/review-pipeline-write-intent/SKILL.md`
- Create: `.opencode/commands/review-pipeline-write-intent.md`
- Reference: `C:\Users\SSAFY\Desktop\새 폴더\mafia-codereview-harness\plugin\commands\write-intent.md`

**Step 1: Write the skill**

Translate the original `write-intent` command into OMO-native instructions. Preserve:
- artifact path
- document structure
- ambiguity surfacing

Translate Claude-only concepts into OMO wording.

**Step 2: Write the command wrapper**

Create a thin command file that invokes or describes the `review-pipeline-write-intent` workflow.

**Step 3: Verify structure**

Read both files back and confirm naming, artifact path, and OMO-native language.

---

### Task 3: Create the gen-criteria skill and command

**Files:**
- Create: `.opencode/skills/review-pipeline-gen-criteria/SKILL.md`
- Create: `.opencode/commands/review-pipeline-gen-criteria.md`
- Reference: `C:\Users\SSAFY\Desktop\새 폴더\mafia-codereview-harness\plugin\commands\gen-criteria.md`
- Reference: `AGENTS.md`
- Reference: `web/AGENTS.md`
- Reference: `mobile/AGENTS.md`
- Reference: `was/AGENTS.md`
- Reference: `watch/AGENTS.md`

**Step 1: Write the skill**

Preserve the ADR extraction and code-convention merge behavior.

Also add repo-specific guidance:
- read root/module `AGENTS.md`
- infer relevant surfaces from changed files
- use subagent delegation for ADR extraction

**Step 2: Write the command wrapper**

Create the command entry for the criteria generation workflow.

**Step 3: Verify content**

Read the created files and ensure they explicitly reference `docs/code-convention.yaml`, `docs/adr.yaml`, and `.review-artifacts/{branch}/code-quality-guide.md`.

---

### Task 4: Create the PR-body skill and command

**Files:**
- Create: `.opencode/skills/review-pipeline-create-pr-body/SKILL.md`
- Create: `.opencode/commands/review-pipeline-create-pr-body.md`
- Reference: `C:\Users\SSAFY\Desktop\새 폴더\mafia-codereview-harness\plugin\commands\create-pr-body.md`

**Step 1: Write the skill**

Keep the original PR-body structure, but adapt wording to this repo’s monorepo layout.

**Step 2: Write the command wrapper**

Create the OMO command entry for PR-body generation.

**Step 3: Verify content**

Read both files back and confirm they still use `git diff <base>...HEAD` and `.review-artifacts/{branch}/pr-body.md`.

---

### Task 5: Create the review skill and command

**Files:**
- Create: `.opencode/skills/review-pipeline-review/SKILL.md`
- Create: `.opencode/commands/review-pipeline-review.md`
- Reference: `C:\Users\SSAFY\Desktop\새 폴더\mafia-codereview-harness\plugin\commands\review.md`

**Step 1: Write the skill**

Preserve:
- automatic execution intent
- priority classes `[p1]` to `[p4]`
- grounding in criteria
- side-effect explanation requirement

Translate the review into OMO-native behavior and artifact references.

**Step 2: Write the command wrapper**

Create the review entry command.

**Step 3: Verify content**

Read back both files and confirm that taste-based review is explicitly forbidden.

---

### Task 6: Create the reflect-review skill and command

**Files:**
- Create: `.opencode/skills/review-pipeline-reflect-review/SKILL.md`
- Create: `.opencode/commands/review-pipeline-reflect-review.md`
- Reference: `C:\Users\SSAFY\Desktop\새 폴더\mafia-codereview-harness\plugin\commands\reflect-review.md`

**Step 1: Write the skill**

Preserve the judgment model:
- accept/reject per comment
- user-facing reasoning
- code modification only here
- QA loop after changes

Add repo-specific validation guidance:
- `web/`, `mobile/` → `pnpm` commands
- `was/`, `watch/` → Gradle commands

**Step 2: Write the command wrapper**

Create the OMO command entry for reflect-review.

**Step 3: Verify content**

Read both files and confirm the judgment format updates `review-comments.md` in place.

---

### Task 7: Create the auto orchestration skill and command

**Files:**
- Create: `.opencode/skills/review-pipeline-auto/SKILL.md`
- Create: `.opencode/commands/review-pipeline-auto.md`
- Reference: `C:\Users\SSAFY\Desktop\새 폴더\mafia-codereview-harness\plugin\commands\auto.md`

**Step 1: Write the skill**

Preserve the 6-step orchestration order.

Adapt the execution model:
- confirm base branch explicitly
- create `.review-artifacts/{branch}/`
- invoke the other skills in sequence
- keep read-heavy steps non-destructive

**Step 2: Write the command wrapper**

Create the main user entrypoint for the full review pipeline.

**Step 3: Verify the orchestration references**

Read both files and confirm all 5 downstream steps are referenced correctly.

---

### Task 8: Add a usage reference document for this repo

**Files:**
- Create: `docs/omo-review-pipeline.md`
- Reference: `docs/plans/2026-03-31-codereview-harness-omo-port-design.md`

**Step 1: Write the usage doc**

Document:
- available command names
- required docs files
- artifact locations
- base-branch confirmation rule
- the fact that `reflect-review` is the only code-modifying phase

**Step 2: Verify the doc**

Read it back and ensure it matches the approved design.

---

### Task 9: End-to-end verification pass

**Files:**
- Verify: `.opencode/skills/**`
- Verify: `.opencode/commands/**`
- Verify: `docs/code-convention.yaml`
- Verify: `docs/adr.yaml`
- Verify: `docs/omo-review-pipeline.md`

**Step 1: Run file-level review**

Read every created file and check that names, artifact paths, and command references are consistent.

**Step 2: Run diagnostics where available**

If any file types have language support, run diagnostics. For pure Markdown/YAML, perform structural readback verification.

**Step 3: Check git status**

Run `git status --short` and confirm only intended files changed.

**Step 4: Summarize completion**

Report what was ported, what remains intentionally excluded (`update-docs`), and what the recommended next manual dry-run is.

---

### Task 10: Optional follow-up command for docs maintenance

**Files:**
- Future create: `.opencode/skills/review-pipeline-update-docs/SKILL.md`
- Future create: `.opencode/commands/review-pipeline-update-docs.md`

**Step 1: Do nothing now**

Leave this out of the implementation unless the user later requests parity for the standalone maintenance command.
