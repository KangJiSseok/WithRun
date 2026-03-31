---
name: review-pipeline-create-pr-body
description: Use when you need to draft the PR-body artifact for the current branch from the confirmed base branch and git diff.
---

# Review Pipeline Create PR Body

## Overview

Create `.review-artifacts/{branch-name}/pr-body.md` from `git diff <base>...HEAD`. The document should explain the branch in reviewer language: what changed, why it changed, risk, tests, and related context.

## When to Use

- After design intent and evaluation criteria are prepared
- Before the review stage
- When the branch needs a grounded PR summary instead of a raw diff

## Core Pattern

1. Confirm the base branch explicitly if it is not already known.
2. Resolve the current branch and ensure the artifact directory exists.
3. Collect `git diff <base>...HEAD` and the changed-file list.
4. Draft `pr-body.md` with this structure:
   - `# PR: {title}`
   - `## Summary`
   - `## Changes`
   - `## Breaking Changes`
   - `## Test Plan`
   - `## Related`
5. Surface missing reviewer-critical details such as migration risk or issue references.
6. After feedback, save the final artifact.

## Guardrails

- Summarize by module or concern, not by raw file dump.
- Keep the summary grounded in user-visible or maintainer-visible impact.
- Mark breaking changes explicitly as `없음` when none exist.

## Common Mistakes

- Writing a commit log instead of a reviewer-oriented PR body
- Skipping test plan details for changed ownership surfaces
- Forgetting to mention cross-project effects such as bridge contract changes
