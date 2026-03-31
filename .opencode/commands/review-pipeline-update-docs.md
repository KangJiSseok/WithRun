---
description: Maintain docs/code-convention.yaml and docs/adr.yaml as a separate documentation workflow.
---

Use the `review-pipeline-update-docs` skill.

Arguments: $ARGUMENTS

Behavior:
- `convention` → update `docs/code-convention.yaml`
- `adr` → update `docs/adr.yaml`
- no argument → inspect and potentially update both

Requirements:
- Always read both documents first.
- Present MUST and RECOMMENDED proposals before editing.
- Treat this as a standalone maintenance workflow, not part of `review-pipeline-auto`.
