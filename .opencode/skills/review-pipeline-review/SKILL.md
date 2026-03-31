---
name: review-pipeline-review
description: Use when you need to produce grounded review comments for the current branch from the design intent, criteria guide, PR body, and git diff.
---

# Review Pipeline Review

## Overview

Generate `.review-artifacts/{branch-name}/review-comments.md` from the confirmed review inputs. Every comment must be grounded in the criteria guide or explicit design intent, never taste alone.

## When to Use

- Design intent, criteria guide, and PR body already exist
- The branch is ready for automated review
- You need prioritized review findings before deciding what to change

## Core Pattern

1. Resolve the current branch.
2. Read:
   - `.review-artifacts/{branch-name}/code-quality-guide.md`
   - `.review-artifacts/{branch-name}/design-intent.md`
   - `.review-artifacts/{branch-name}/pr-body.md`
3. Collect `git diff <base>...HEAD`.
4. Produce `review-comments.md` in this structure:
   - `# Code Review`
   - `## Summary`
   - `## Comments`
   - comments grouped by `[p1]` to `[p4]`
5. For each actionable comment include:
   - file path and line
   - grounding source
   - problem description
   - proposed change
   - side effect
   - why the proposal is still worth it

## Priority Rules

- `[p1]`: correctness, safety, criteria violation, intent mismatch
- `[p2]`: strong maintainability or readability issue
- `[p3]`: good improvement but not blocking
- `[p4]`: minor cleanup

## Guardrails

- Respect intentional decisions documented in `design-intent.md`.
- If implementation matches a documented intentional tradeoff, do not raise it as a complaint.
- Review should continue automatically without user confirmation.

## Common Mistakes

- Taste-based review without a grounding reference
- Questioning an intentional exclusion as if it were accidental
- Suggesting changes without calling out their side effects
