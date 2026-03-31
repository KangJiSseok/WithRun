---
description: Triage review comments, apply approved fixes, and run module-aware QA.
---

Use the `review-pipeline-reflect-review` skill.

Arguments: $ARGUMENTS

Read `.review-artifacts/{branch-name}/review-comments.md`, propose accept or reject judgments, wait for user confirmation, then apply approved changes and update the artifact with final judgments.
