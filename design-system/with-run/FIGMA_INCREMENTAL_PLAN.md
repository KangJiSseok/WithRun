# With Run Figma Incremental Plan

This file breaks the design system into tiny Figma MCP write batches so the file can be built one call at a time under strict tool limits.

## Target File

- Figma file: `With Run Design System`
- File key: `rpPInLpYpnjnGehYuJhcHY`

## Writing Strategy

- Use one `use_figma` call per batch.
- Create only one page section or one small component group per call.
- Prefer updating an existing page instead of creating many nodes at once.
- Verify visually in Figma after each successful write before continuing.

## Batch 01

- Page: `03 Rules Sync`
- Block name: `Rule Block 01`
- Goal: establish the project direction headline and core visual summary
- Contents:
  - badge: `With Run Design Rule`
  - title: `Bright Illustrated Mobile Companion`
  - body:
    - primary action uses bright orange
    - support accent uses clear sky blue
    - green stays semantic, not brand-led
    - surfaces stay cream or white
    - one hero illustration per screen
    - large tap targets and bottom-weighted interaction

## Batch 02

- Page: `03 Rules Sync`
- Block name: `Rule Block 02`
- Goal: document core palette
- Contents:
  - `Primary Action #FF8A3D`
  - `Primary Soft #FFE3CF`
  - `Support Accent #5BB8FF`
  - `Support Soft #DDF2FF`
  - `Warm Highlight #FFD76A`
  - `Background #FFF9F2`
  - `Surface #FFFFFF`
  - `Text Primary #24324A`
  - `Text Secondary #677991`
  - `Border #D9E7F5`

## Batch 03

- Page: `03 Rules Sync`
- Block name: `Rule Block 03`
- Goal: document typography and spacing
- Contents:
  - font family: `Pretendard Variable`
  - display, heading, title, body, caption hierarchy
  - spacing scale: `4, 8, 12, 16, 20, 24, 32, 40, 48, 64`
  - minimum tap target: `44x44`
  - minimum gap between touch targets: `8`

## Batch 04

- Page: `03 Rules Sync`
- Block name: `Rule Block 04`
- Goal: document form and onboarding shell rules
- Contents:
  - bottom sheet as default onboarding shell
  - keep one active step in focus
  - reduce illustration complexity during text entry
  - always preserve visible progress and clear CTA

## Batch 05

- Page: `04 Components`
- Group name: `Button Set 01`
- Goal: create small starter components
- Contents:
  - primary button
  - secondary button
  - social login button

## Batch 06

- Page: `04 Components`
- Group name: `Form Set 01`
- Goal: create form primitives
- Contents:
  - text input
  - chip default
  - chip selected

## Batch 07

- Page: `04 Components`
- Group name: `Card Set 01`
- Goal: create mobile summary components
- Contents:
  - metric card
  - onboarding bottom sheet card

## Source of Truth

- Product rules: [AGENTS.md](C:/Users/SSAFY/Desktop/S14P21D202/AGENTS.md)
- Detailed design system: [MASTER.md](C:/Users/SSAFY/Desktop/S14P21D202/design-system/with-run/MASTER.md)
