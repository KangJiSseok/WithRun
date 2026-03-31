---
name: review-pipeline-auto
description: Use when you need to run the full OMO-native review pipeline for the current branch from status check through review reflection and QA.
---

# Review Pipeline Auto

## Overview

Run the six core stages in order for the current branch. This skill creates the artifact directory, confirms the review baseline, orchestrates the other review-pipeline skills, and reports the final artifact set and completion status.

## When to Use

- A feature branch is implemented and ready for structured review
- The repo already has `docs/code-convention.yaml` and `docs/adr.yaml`
- You want the full artifact chain, not just one stage

## Core Pattern

1. Resolve the current branch with `git branch --show-current`.
2. Confirm the base branch explicitly with the user.
3. Collect `git diff <base>...HEAD --stat` and summarize changed surfaces.
4. Create `.review-artifacts/{branch-name}/`.
5. Run the stages in order:
   - `review-pipeline-write-intent`
   - `review-pipeline-gen-criteria`
   - `review-pipeline-create-pr-body`
   - `review-pipeline-review`
   - `review-pipeline-reflect-review`
6. At the end, report the artifact list and current branch status.

## Execution Model

- `write-intent`, `gen-criteria`, `create-pr-body`, and `review` are read-heavy stages.
- `reflect-review` is the only stage allowed to modify source code.
- Worktrees are optional, not mandatory, unless isolation materially improves safety.

## Common Mistakes

- Assuming the base branch instead of confirming it
- Running review before intent or criteria exist
- Allowing early stages to mutate source code
