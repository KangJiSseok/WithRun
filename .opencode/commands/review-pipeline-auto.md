---
description: Run the full OMO-native review pipeline for the current branch.
---

Use the `review-pipeline-auto` skill to run the complete review pipeline for the current branch.

Arguments: $ARGUMENTS

Requirements:
- Confirm the base branch explicitly if it is not already clear from `$ARGUMENTS`.
- Use `.review-artifacts/{branch-name}/` for all generated artifacts.
- Keep code changes restricted to the `reflect-review` phase.
