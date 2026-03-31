---
name: review-pipeline-write-intent
description: Use when you need to create or refresh the design-intent artifact for the current branch before criteria generation, PR drafting, or review.
---

# Review Pipeline Write Intent

## Overview

Create `.review-artifacts/{branch-name}/design-intent.md` for the current branch. The document should capture the intended behavior, major design decisions, explicit exclusions, and review cautions so later stages can review against intent instead of guessing.

## When to Use

- Running the review pipeline for a feature branch
- The branch changed enough that previous design intent is stale
- Criteria generation or review needs a grounded intent document

## Core Pattern

1. Resolve the current branch with `git branch --show-current`.
2. Ensure `.review-artifacts/{branch-name}/` exists.
3. Review the current task context, changed files, and any relevant `AGENTS.md` instructions.
4. Draft `design-intent.md` with this structure:
   - `# 설계의도`
   - `## 작업 개요`
   - `## 핵심 설계 결정`
   - `## 의도적으로 제외한 것`
   - `## 주의사항`
5. Surface concrete ambiguities instead of vague questions.
6. After user feedback, save the final document to `.review-artifacts/{branch-name}/design-intent.md`.

## Output Requirements

- The document must explain what problem the branch solves.
- Each major decision must include alternatives, the selected direction, and why.
- Explicitly list what is intentionally out of scope.
- Highlight module-specific cautions when the diff crosses `web`, `mobile`, `was`, or `watch`.

## Common Mistakes

- Repeating file changes without explaining intent
- Leaving out excluded scope, which causes later taste-based review
- Asking open-ended questions instead of listing the exact ambiguity to resolve
