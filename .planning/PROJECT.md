# Museum Puzzle & Daily Reveal

## What This Is

The AnkiDroid Museum homescreen features a 10x10 jigsaw puzzle overlay on masterpiece paintings. Locked pieces are semi-transparent gray gradient PNGs (80% opacity) that tease the painting underneath. As users review flashcards, puzzle pieces unlock to reveal the painting at full opacity. Both locked and unlocked pieces share the exact same jigsaw shapes via PNG alpha mask compositing.

## Core Value

Studying flashcards progressively reveals a masterpiece — the puzzle is a daily visual reward that connects effort to beauty.

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

_(None — next milestone requirements TBD)_

### Out of Scope

- Changing the puzzle grid dimensions (stays 10x10) — not requested
- Changing the Museum layout, gallery, or animation systems — not requested
- Streak counter — creates loss aversion anxiety; conflicts with Anki's flexible study philosophy
- Adjustable opacity slider — settings creep; 80% is user-tested
- Multiple simultaneous puzzles — adds complexity; validate single puzzle first

### Future (v1.2 candidates)

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

## Constraints

- **Assets**: User-provided PNGs must be used as-is (no procedural generation for locked pieces)
- **Compatibility**: Must not break unlocked piece rendering or animations
- **Module**: Changes confined to AnkiDroid module only

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

---
*Last updated: 2026-02-13 after v1.1 milestone*
