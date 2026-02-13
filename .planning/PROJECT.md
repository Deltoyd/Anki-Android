# Museum Puzzle & Daily Reveal

## What This Is

The AnkiDroid Museum homescreen features a masterpiece painting gallery with a 10x10 jigsaw puzzle overlay on the active painting. A streak system tracks daily study activity — tapping the fire button in the toolbar opens a bottom sheet showing weekly, monthly, and yearly study heatmaps. The masterpiece gallery occupies the full lower homescreen, with active paintings showing the puzzle, completed paintings fully revealed, and locked paintings blurred.

## Core Value

Studying flashcards progressively reveals a masterpiece — the puzzle is a daily visual reward that connects effort to beauty.

## Current Milestone: v1.2 Streak & Gallery Redesign

**Goal:** Replace the homescreen heatmap with a streak bottom sheet and redesign the gallery to give the masterpiece full screen presence.

**Target features:**
- Streak button (🔥) in toolbar opens a bottom sheet
- Bottom sheet with Week/Month/Year tabs showing study heatmaps
- Streak count + study time display
- Remove heatmap from homescreen
- Gallery states: active (puzzle), completed (full painting), locked (blurred)
- Masterpiece takes full lower homescreen area

## Requirements

### Validated

- ✓ Museum homescreen with 10x10 puzzle grid — existing
- ✓ Locked pieces rendered as gray PNG overlays — existing
- ✓ Unlocked pieces reveal painting through clipped jigsaw paths — existing
- ✓ Piece unlock animation with fade-in — existing
- ✓ Peek mode to temporarily show full painting — existing
- ✓ Gallery pager with multiple art pieces — existing
- ✓ Puzzle break cinematic animation — existing
- ✓ Replace 9 old PNG assets with 14 new gray gradient puzzle pieces — v1.0
- ✓ Fix piece placement logic so tabs always interlock with adjacent holes — v1.0
- ✓ Corners placed at exactly the 4 grid corners — v1.0
- ✓ Border variants alternate along edges so adjacent pieces interlock — v1.0
- ✓ Interior variants alternate in checkerboard pattern so all neighbors interlock — v1.0
- ✓ Visual result: one unified gray puzzle, not a disordered grid — v1.0
- ✓ Unlocked pieces use the same jigsaw shape as their locked PNG counterpart — v1.1
- ✓ Locked pieces rendered at 80% opacity so painting is visible underneath — v1.1

### Active

- [ ] Streak button (🔥) in toolbar opens streak bottom sheet
- [ ] Weekly view: 7 day circles with study intensity coloring
- [ ] Monthly view: calendar grid with day circles colored by intensity
- [ ] Yearly view: compact dot heatmap
- [ ] Streak count + study time displayed in bottom sheet
- [ ] Remove heatmap from homescreen
- [ ] Gallery states: active (puzzle overlay), completed (full painting), locked (blurred/dimmed)
- [ ] Masterpiece area takes full lower homescreen

### Out of Scope

- Changing the puzzle grid dimensions (stays 10x10) — not requested
- Adjustable opacity slider — settings creep; 80% is user-tested
- Multiple simultaneous puzzles — adds complexity; validate single puzzle first
- Study goals / minimum thresholds — any card reviewed counts as studied
- Push notifications for streak maintenance — not requested

### Future (v1.3+ candidates)

- Card review-driven unlocking (one piece per card review)
- Daily midnight reset of incomplete puzzles
- Completion celebration & permanent gallery collection
- Unlock sound effect
- Progress counter (pieces unlocked / total)

## Context

**Current state (post v1.1):** `PaintingPuzzleView.kt` renders a 10x10 jigsaw puzzle using 14 gray gradient PNG assets. Locked pieces show the painting underneath at 80% opacity via saveLayer + PorterDuff DST_IN compositing. Unlocked pieces use the same PNG alpha masks to clip the painting to identical jigsaw shapes. `PuzzlePiecePathGenerator` has been deleted — all shape clipping derives from the PNG assets directly.

**Shipped v1.0** on 2026-02-12: 1 phase, 2 plans, 23 files changed, +1,383/-242 lines.
**Shipped v1.1** on 2026-02-13: 2 phases, 2 plans, 2 files changed, +72/-266 lines.

**Key files:**
- `AnkiDroid/src/main/java/com/ichi2/anki/ui/museum/PaintingPuzzleView.kt` — piece rendering and placement
- `AnkiDroid/src/main/res/drawable-nodpi/puzzle_*.png` — 14 gray gradient PNG assets

**Design references (v1.2):**
- Weekly bottom sheet: Ikasi-style streak sheet with fire icon + 7 day circles
- Monthly view: Calendar grid with colored day circles (reference: Ikasi monthly heatmap)
- Yearly view: Compact dot heatmap (reference: Ikasi yearly view)
- Colors: Gray (unstudied) → #d36b52 shades (studied, darker = more)

## Constraints

- **Assets**: User-provided PNGs must be used as-is (no procedural generation for locked pieces)
- **Compatibility**: Must not break unlocked piece rendering or animations
- **Module**: Changes confined to AnkiDroid module only
- **Color**: Study intensity uses #d36b52 color family, gray for unstudied

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Use 14 PNGs with 2 variants per category | Allows proper tab/hole interlocking between neighbors | ✓ Good |
| Checkerboard alternation via (row+col) % 2 | Standard jigsaw pattern ensures adjacent pieces always complement | ✓ Good |
| Inverted variant formula for left/right borders | Left/right PNG variants have swapped tab orientations vs top/bottom | ✓ Good |
| Per-piece body offset rendering | Each PNG has different body-to-tab ratios; uniform scaling caused gaps | ✓ Good |
| PorterDuff DST_IN with saveLayer for alpha mask compositing | Pixel-perfect shape clipping using PNG alpha channel | ✓ Good |
| Delete PuzzlePiecePathGenerator entirely | No other usages; eliminates dual-shape system and 239 lines of dead code | ✓ Good |
| 80% opacity for locked piece transparency | User tested 50%, preferred more opaque. 80% keeps gray dominant with painting hint | ✓ Good |
| Streak bottom sheet replaces homescreen heatmap | Frees homescreen space for masterpiece; streak is on-demand, not always visible | — Pending |
| Any card reviewed = studied day | Low friction; encourages daily habit without pressure of meeting a quota | — Pending |
| #d36b52 color family for study intensity | User-specified; warm tone contrasts with gray unstudied days | — Pending |

---
*Last updated: 2026-02-13 after v1.2 milestone started*
