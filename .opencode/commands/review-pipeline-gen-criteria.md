---
description: Generate the code-quality-guide artifact from conventions, ADRs, and AGENTS guidance.
---

Use the `review-pipeline-gen-criteria` skill.

Arguments: $ARGUMENTS

Read `docs/code-convention.yaml`, `docs/adr.yaml`, and relevant `AGENTS.md` files, then create `.review-artifacts/{branch-name}/code-quality-guide.md`.
