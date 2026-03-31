---
name: review-pipeline-gen-criteria
description: Use when you need to generate the code-quality-guide artifact for the current branch by combining repo conventions, relevant ADRs, and module-specific instructions.
---

# Review Pipeline Generate Criteria

## Overview

Create `.review-artifacts/{branch-name}/code-quality-guide.md` by combining shared conventions, task-relevant ADRs, and module-specific constraints. The output is the grounding source for review comments.

## When to Use

- After design intent exists for the current branch
- Before review comments are generated
- When the diff spans multiple ownership surfaces and review needs scoped criteria

## Core Pattern

1. Resolve the current branch and ensure the artifact directory exists.
2. Read:
   - `docs/code-convention.yaml`
   - `docs/adr.yaml`
   - root `AGENTS.md`
   - module `AGENTS.md` files relevant to changed files
3. Infer affected surfaces from the diff.
4. Delegate ADR extraction to a subagent:
   - filter by relevant stacks first
   - then filter by context/decision fit
   - explain how each selected ADR applies to this branch
5. Merge the selected convention rules and ADR guidance into `code-quality-guide.md`.
6. Surface scope ambiguities to the user, then finalize the artifact.

## Output Structure

- `# 평가기준`
- `## 공통 기준 (Code Convention)`
- `## 이 작업에 적용되는 ADR`
- For each selected ADR:
  - identifier and title
  - direct application guidance for this branch

## Guardrails

- Do not dump the entire ADR file into the artifact.
- Do not include criteria unrelated to changed files.
- Prefer repo-grounded rules over generic best-practice advice.

## Common Mistakes

- Including irrelevant ADRs because they share a language but not a problem
- Ignoring module `AGENTS.md` constraints
- Writing criteria too vaguely to support grounded review comments
