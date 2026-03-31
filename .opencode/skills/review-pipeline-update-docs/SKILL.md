---
name: review-pipeline-update-docs
description: Use when you need to maintain docs/code-convention.yaml and/or docs/adr.yaml after new team rules or architecture decisions emerge outside the core review pipeline.
---

# Review Pipeline Update Docs

## Overview

Maintain `docs/code-convention.yaml` and `docs/adr.yaml` as a separate documentation workflow, not as part of the core review pipeline. Always analyze both documents first, identify conflicts or outdated rules, present MUST and RECOMMENDED proposals, and only edit after user confirmation.

## When to Use

- A new coding rule should be added or an old one should be revised
- A meaningful architecture decision has been made and should be recorded as an ADR
- Existing ADRs and conventions no longer match the current project reality
- A recent implementation created follow-up documentation debt

## Core Pattern

1. Determine scope from the request:
   - `convention` → focus on `docs/code-convention.yaml`
   - `adr` → focus on `docs/adr.yaml`
   - no argument → inspect and potentially update both
2. Read **both** `docs/code-convention.yaml` and `docs/adr.yaml` before proposing any change.
3. Compare current work context against both documents and identify:
   - conflicts
   - chain updates where one document implies changes in the other
   - stale or deprecated entries
4. Present a proposal list grouped as:
   - `MUST`
   - `RECOMMENDED`
5. Wait for user confirmation.
6. Apply approved additions, edits, or removals.
7. Report the final documentation changes clearly.

## Convention Update Rules

- Keep rules explicit and actionable.
- Use entries shaped like:

```yaml
- id: GEN-001
  rule: ...
  stacks: [all]
```

- Prefer stack or ownership-specific rules only when the rule is not genuinely global.

## ADR Update Rules

- Use entries shaped like:

```yaml
- id: ADR-001
  title: ...
  status: adopted | deprecated | superseded
  date: YYYY-MM-DD
  stacks: [...]
  context: |
    ...
  decision: ...
  consequence: ...
```

- `context` must explain why the decision became necessary in concrete terms.
- If context is too weak to justify a durable ADR, ask the user for more detail before editing.

## Guardrails

- Do not run this as part of `review-pipeline-auto`.
- Do not edit either document before showing the proposal list.
- When updating one document, still cross-check the other for cascading impact.

## Common Mistakes

- Updating only one document without checking for contradictions in the other
- Writing vague rules that cannot guide later review
- Recording a conclusion as an ADR without enough historical context
