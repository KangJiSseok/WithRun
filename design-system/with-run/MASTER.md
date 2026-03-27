# With Run Design System Master

> First check `design-system/with-run/pages/[page-name].md`.
> If a page file exists, it overrides this master.
> If not, follow this file as the source of truth.

---

## Product Direction

- Product type: mobile running companion app
- Delivery context: React web UI inside a React Native WebView shell
- Core mood: bright, sunny, friendly, energetic, outdoorsy, approachable
- Visual language: illustration-first mobile UI with soft claymorphism-lite depth

## Design Intent

- Make running feel easy to start, not intimidating
- Use illustration to create warmth and emotional context
- Keep one clear hero action per screen
- Prefer app-like mobile flows over landing-page or dashboard conventions

---

## Style System

### Primary Style

- Style name: Bright Illustration + Soft Claymorphism-Lite
- Use soft depth, rounded surfaces, gentle layering, and warm highlights
- Borrow softness from claymorphism, but do not use heavy toy-like `3px` to `4px` borders everywhere
- Keep controls production-ready, lightweight, and accessible
- Build the brand around a bright orange and sky-blue pairing instead of a red-first or green-first palette

### Avoid

- Dark dashboard aesthetics
- Cyberpunk or neon sports UI
- Heavy glassmorphism
- Hard neubrutalist borders
- Desktop multi-column onboarding layouts
- Overdecorated scenes where illustration competes with CTA

---

## Color Direction

Use these semantic roles for all new Figma work and future token updates.

| Role | Target | Notes |
|------|--------|-------|
| Primary action | `#FF8A3D` | Bright orange for main CTA and key moments |
| Primary soft | `#FFE3CF` | Soft orange for selected chips and light fills |
| Support accent | `#5BB8FF` | Clear sky blue for highlight, focus, and info |
| Support soft | `#DDF2FF` | Light sky surface |
| Warm highlight | `#FFD76A` | Sun accent for illustration warmth |
| App background | `#FFF9F2` | Cream base surface |
| Surface | `#FFFFFF` | Primary card and sheet surface |
| Text primary | `#24324A` | Deep navy-gray |
| Text secondary | `#677991` | Secondary copy |
| Border | `#D9E7F5` | Soft outline |
| Error | `#F06A6A` | Error and destructive state |
| Success | `#38B97A` | Positive feedback, not a core brand accent |

### Color Rules

- Use orange as the main action color and sky blue as the main support color
- Keep green limited to semantic success and health feedback, not as a competing brand accent
- Use one primary brand color plus at most two support accents per screen
- Keep backgrounds light and breathable
- Use semantic color mapping, never one-off screen hex values
- Illustration accents must come from the same palette system

---

## Typography

- Base UI font: `Pretendard Variable`
- Keep Pretendard as the default app type system
- If display typography is explored in concept work, use it only as mood reference until tokens are intentionally migrated

### Type Hierarchy

| Token | Size | Line Height | Weight | Use |
|------|------|-------------|--------|-----|
| Display | `32-40px` | `40-48px` | `700` | hero onboarding moments |
| Heading | `24px` | `32-36px` | `700` | section titles |
| Title | `18-20px` | `26-30px` | `700` | card and sheet titles |
| Body L | `16px` | `24px` | `400-500` | primary explanatory copy |
| Body | `14px` | `21-22px` | `400-500` | standard copy |
| Caption | `12px` | `18px` | `500` | chips, helper text, metadata |

### Typography Rules

- Headings should feel compact, warm, and easy to scan
- Body copy should stay calm and readable
- Use strong numeric hierarchy for distance, pace, time, and calories

---

## Spacing, Radius, and Elevation

### Spacing

- Base spacing unit: `4px`
- Common steps: `4, 8, 12, 16, 20, 24, 32, 40, 48, 64`
- Maintain at least `8px` between adjacent touch targets

### Radius

- Small controls: `8-12px`
- Inputs and compact cards: `16-20px`
- Bottom sheets and key cards: `24-28px`
- Pills and chips: `999px`

### Elevation

- Use soft shadows only
- Favor one outer shadow layer over stacked glossy effects
- Depth should feel airy, not shiny

---

## Component Rules

### Primary CTA

- Full width on key mobile flows
- Bright orange background
- White text
- Rounded `20-24px`
- Minimum height `44px`

### Secondary CTA

- White or cream surface
- Soft border
- Lower emphasis than primary

### Social Login Buttons

- Use provider color only when required for brand recognition
- Keep button shape consistent with the rest of the app

### Chips

- Default: white or cream surface with soft border
- Selected: soft orange or soft sky fill with stronger text emphasis
- Never make chips too small for touch

### Inputs

- Rounded, breathable, low-noise fields
- Use soft borders and strong focus rings
- Avoid dense stacked field groups in onboarding

### Metric Cards

- Use bright surfaces and bold numeric hierarchy
- Keep decoration secondary to the metric itself

### Bottom Sheets

- Default shell for onboarding, entry, and lightweight decision flows
- Large top radius
- Clear top handle
- One clear CTA area

---

## Illustration Rules

- Use simple shapes, soft gradients, rounded geometry, and lightly tinted outlines
- Keep mascot, landscape, and empty-state illustration language consistent
- One hero illustration or stage per screen is enough
- Reduce background motion during form entry
- Illustration should support the screen's emotion, not explain the interface
- Use orange and sky blue as the dominant illustration anchors, with yellow only as a small warmth accent

---

## Motion Rules

- Standard transitions: `180-240ms`
- Mascot or celebratory moments: soft spring motion
- Prefer depth, opacity, and position changes over large scale jumps
- Do not rely on hover for primary meaning
- Respect reduced motion

---

## Mobile Layout Rules

- Design for `375px` to `430px` widths first
- Prefer single-column layout and bottom-weighted interaction
- Keep primary actions within easy thumb reach when possible
- Use app-like page composition, not desktop cards centered in empty space
- Avoid hover-only states, wide tables, and dense multi-column forms

---

## Onboarding Rules

- Use bottom-sheet composition as the default onboarding form shell
- Keep copy short and reassuring
- Keep one step in focus at a time
- Show visible progress
- Use illustration for emotional warmth and forward momentum
- Lower scene complexity once the user is making selections or typing

---

## Accessibility and Quality Gates

- Minimum touch target: `44x44px`
- Minimum touch gap: `8px`
- Contrast: `4.5:1` minimum for regular text
- Focus states must remain visible
- Primary interactions must work without hover
- Respect `prefers-reduced-motion`
- Validate at `375px`, `430px`, `768px`, and desktop breakpoints
