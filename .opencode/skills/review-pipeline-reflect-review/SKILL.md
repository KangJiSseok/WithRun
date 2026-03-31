---
name: review-pipeline-reflect-review
description: Use when you need to triage review-comments, get user confirmation on accept or reject decisions, apply approved code changes, and run module-aware QA.
---

# Review Pipeline Reflect Review

## Overview

This is the only core pipeline stage allowed to modify source code. It reads `review-comments.md`, proposes accept or reject judgments, applies approved changes, runs QA, and updates the artifact with explicit judgments.

## When to Use

- Review comments already exist for the current branch
- The user wants to act on review findings
- The branch is ready for code changes and verification

## Core Pattern

1. Resolve the current branch and read `.review-artifacts/{branch-name}/review-comments.md`.
2. For each comment, prepare a judgment proposal:
   - `[p1]` default accept unless there is a strong reason not to
   - `[p2]` propose accept or reject with reasoning
   - `[p3]` propose but leave room for user preference
   - `[p4]` batch as accept or ignore
3. Present the judgment list to the user.
4. After confirmation, apply accepted code changes.
5. Run QA for touched surfaces:
   - `web`, `mobile` → relevant `pnpm` checks
   - `was`, `watch` → relevant Gradle checks
6. Update `review-comments.md` in place with `**판정: ACCEPT**` or `**판정: REJECT**` and reasoning.
7. Report the final changes and QA results.

## Guardrails

- Do not change application code before the user confirms the judgment list.
- Keep edits scoped to accepted findings and bugs introduced while fixing them.
- Validation must follow the touched ownership surfaces instead of running unrelated modules by default.

## Common Mistakes

- Editing code before judgment confirmation
- Forgetting to write the final judgment back into the artifact
- Treating `[p3]` and `[p4]` as mandatory fixes
