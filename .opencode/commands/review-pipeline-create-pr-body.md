---
description: Create the PR-body artifact for the current branch from the confirmed base diff.
---

Use the `review-pipeline-create-pr-body` skill.

Arguments: $ARGUMENTS

Confirm the base branch if needed, analyze `git diff <base>...HEAD`, and create `.review-artifacts/{branch-name}/pr-body.md`.
